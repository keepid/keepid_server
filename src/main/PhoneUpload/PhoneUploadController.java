package PhoneUpload;

import Config.Message;
import Database.PhoneUpload.PhoneUploadSessionDao;
import Database.User.UserDao;
import File.IdCategoryType;
import File.SessionOrganizationId;
import Notification.WindmillNotificationClient;
import User.User;
import User.UserMessage;
import io.javalin.http.Handler;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.json.JSONObject;

public class PhoneUploadController {
  private static final long TEN_MINUTES_MS = 10L * 60L * 1000L;

  private UserDao userDao;
  private PhoneUploadSessionDao phoneUploadSessionDao;
  private WindmillNotificationClient notificationClient;

  public PhoneUploadController(
      UserDao userDao,
      PhoneUploadSessionDao phoneUploadSessionDao,
      WindmillNotificationClient notificationClient) {
    this.userDao = userDao;
    this.phoneUploadSessionDao = phoneUploadSessionDao;
    this.notificationClient = notificationClient;
  }

  public Handler createPhoneUploadSession =
      ctx -> {
        String actorUsername = ctx.sessionAttribute("username");
        String orgName = ctx.sessionAttribute("orgName");
        if (actorUsername == null || orgName == null) {
          ctx.result(UserMessage.SESSION_TOKEN_FAILURE.toResponseString());
          return;
        }

        JSONObject req = new JSONObject(ctx.body());
        String targetUser = req.optString("targetUser", "").trim();
        String idCategoryRaw = req.optString("idCategory", "").trim();
        String customIdCategoryRaw = req.optString("customIdCategory", "").trim();
        if (targetUser.isEmpty()) {
          ctx.result(UserMessage.INVALID_PARAMETER.toJSON("Missing target user").toString());
          return;
        }
        IdCategoryType idCategory = IdCategoryType.createFromString(idCategoryRaw);
        if (idCategory == IdCategoryType.NONE) {
          ctx.result(UserMessage.INVALID_PARAMETER.toJSON("Invalid ID category").toString());
          return;
        }
        String customIdCategory = null;
        if (idCategory == IdCategoryType.OTHER) {
          if (customIdCategoryRaw.isEmpty()) {
            ctx.result(
                UserMessage.INVALID_PARAMETER
                    .toJSON("Custom ID category is required for Other")
                    .toString());
            return;
          }
          customIdCategory = customIdCategoryRaw;
        }

        Optional<User> actorUserOpt = userDao.get(actorUsername);
        Optional<User> targetUserOpt = userDao.get(targetUser);
        if (actorUserOpt.isEmpty() || targetUserOpt.isEmpty()) {
          ctx.result(UserMessage.USER_NOT_FOUND.toResponseString());
          return;
        }
        User actorUser = actorUserOpt.get();
        User target = targetUserOpt.get();
        if (!isSameOrganization(ctx, target, orgName)) {
          ctx.result(UserMessage.CROSS_ORG_ACTION_DENIED.toResponseString());
          return;
        }

        String actorPhone = normalizeUsPhoneToE164(actorUser.getPhone());
        if (!WindmillNotificationClient.isValidPhoneNumber(actorPhone)) {
          ctx.result(
              UserMessage.INVALID_PARAMETER
                  .toJSON("Case worker must have a valid US phone number saved in profile")
                  .toString());
          return;
        }

        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + TEN_MINUTES_MS);
        String rawToken = PhoneUploadTokenUtil.generateRawToken();
        String tokenHash = PhoneUploadTokenUtil.hashToken(rawToken);
        String clientDisplayName = buildClientDisplayName(target);
        PhoneUploadSession phoneUploadSession =
            new PhoneUploadSession()
                .setTokenHash(tokenHash)
                .setActorUsername(actorUsername)
                .setTargetClientUsername(target.getUsername())
                .setTargetClientDisplayName(clientDisplayName)
                .setOrganizationName(target.getOrganization())
                .setOrganizationId(target.getOrganizationId())
                .setIdCategory(idCategoryRaw)
                .setCustomIdCategory(customIdCategory)
                .setCreatedAt(now)
                .setExpiresAt(expiresAt);
        phoneUploadSessionDao.save(phoneUploadSession);

        String mobileUrl = buildMobileUrl(ctx, rawToken);
        String categoryForSms = customIdCategory != null ? customIdCategory : idCategory.toString();
        String smsMessage =
            "Upload a "
                + categoryForSms
                + " for "
                + clientDisplayName
                + ". Link expires in 10 minutes: "
                + mobileUrl;
        notificationClient.sendSms(actorPhone, smsMessage);

        JSONObject res = UserMessage.SUCCESS.toJSON();
        res.put("mobileUrl", mobileUrl);
        res.put("qrUrl", mobileUrl);
        res.put("expiresAt", expiresAt.getTime());
        res.put("phoneNumber", actorPhone);
        ctx.result(res.toString());
      };

  public Handler resolvePhoneUploadToken =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String rawToken = req.optString("phoneUploadToken", "").trim();
        if (rawToken.isEmpty()) {
          ctx.result(UserMessage.INVALID_PARAMETER.toJSON("Missing phone upload token").toString());
          return;
        }

        Optional<PhoneUploadSession> sessionOpt = getOpenSession(rawToken, new Date());
        if (sessionOpt.isEmpty()) {
          ctx.result(UserMessage.INVALID_PARAMETER.toJSON("Phone upload token is invalid or expired").toString());
          return;
        }
        PhoneUploadSession session = sessionOpt.get();
        JSONObject res = UserMessage.SUCCESS.toJSON();
        res.put("phoneUpload", session.toClientContextJson());
        ctx.result(res.toString());
      };

  public Handler closePhoneUploadSession =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String rawToken = req.optString("phoneUploadToken", "").trim();
        if (rawToken.isEmpty()) {
          ctx.result(UserMessage.INVALID_PARAMETER.toJSON("Missing phone upload token").toString());
          return;
        }

        Optional<PhoneUploadSession> sessionOpt = getOpenSession(rawToken, new Date());
        if (sessionOpt.isEmpty()) {
          ctx.result(UserMessage.INVALID_PARAMETER.toJSON("Phone upload token is invalid or expired").toString());
          return;
        }
        PhoneUploadSession session = sessionOpt.get();
        session.setClosedAt(new Date());
        phoneUploadSessionDao.update(session);
        ctx.result(UserMessage.SUCCESS.toResponseString());
      };

  public Optional<PhoneUploadSession> getOpenSession(String rawToken, Date now) {
    if (rawToken == null || rawToken.isBlank()) {
      return Optional.empty();
    }
    String tokenHash = PhoneUploadTokenUtil.hashToken(rawToken.trim());
    Optional<PhoneUploadSession> sessionOpt = phoneUploadSessionDao.getByTokenHash(tokenHash);
    if (sessionOpt.isEmpty()) {
      return Optional.empty();
    }
    PhoneUploadSession session = sessionOpt.get();
    if (session.isClosed() || session.isExpired(now)) {
      return Optional.empty();
    }
    return Optional.of(session);
  }

  private static String buildClientDisplayName(User user) {
    String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
    String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
    String fullName = (firstName + " " + lastName).trim();
    if (!fullName.isBlank()) {
      return fullName;
    }
    return user.getUsername();
  }

  private static boolean isSameOrganization(io.javalin.http.Context ctx, User target, String orgName) {
    Optional<ObjectId> sessionOid = SessionOrganizationId.fromContext(ctx);
    if (sessionOid.isPresent() && target.getOrganizationId() != null) {
      return Objects.equals(sessionOid.get(), target.getOrganizationId());
    }
    return Objects.equals(orgName, target.getOrganization());
  }

  private static String buildMobileUrl(io.javalin.http.Context ctx, String rawToken) {
    String origin = ctx.header("Origin");
    String base = (origin != null && !origin.isBlank()) ? origin : "https://keep.id";
    return base + "/phone-upload#t=" + rawToken;
  }

  public static String normalizeUsPhoneToE164(String rawPhone) {
    if (rawPhone == null || rawPhone.isBlank()) {
      return null;
    }
    String digits = rawPhone.replaceAll("[^0-9]", "");
    if (digits.length() == 10) {
      return "+1" + digits;
    }
    if (digits.length() == 11 && digits.startsWith("1")) {
      return "+" + digits;
    }
    return null;
  }
}
