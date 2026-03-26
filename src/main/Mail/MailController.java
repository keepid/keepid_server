package Mail;

import Config.Message;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.Mail.MailDao;
import Form.Form;
import Mail.Services.SubmitToLobMailService;
import io.javalin.http.Handler;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

@Slf4j
public class MailController {
  private MailDao mailDao;
  private FileDao fileDao;
  private FormDao formDao;
  private MailSender mailSender;
  private Security.EncryptionController encryptionController;

  public MailController(
      MailDao mailDao,
      FileDao fileDao,
      FormDao formDao,
      MailSender mailSender,
      Security.EncryptionController encryptionController) {
    this.mailDao = mailDao;
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.mailSender = mailSender;
    this.encryptionController = encryptionController;
  }

  public Handler getFormMailAddresses =
      ctx -> {
        FormMailAddress[] formMailAddresses = FormMailAddress.values();
        JSONObject response = new JSONObject();
        for (FormMailAddress address : formMailAddresses) {
          JSONObject addressJson = new JSONObject();
          addressJson.put("name", address.getName());
          addressJson.put("description", address.getDescription());
          addressJson.put("office_name", address.getOffice_name());
          addressJson.put("name_for_check", address.getNameForCheck());
          addressJson.put("street1", address.getStreet1());
          addressJson.put("street2", address.getStreet2());
          addressJson.put("city", address.getCity());
          addressJson.put("state", address.getState());
          addressJson.put("zipcode", address.getZipcode());
          addressJson.put("check_amount", address.getMaybeCheckAmount().toString());
          addressJson.put("acceptable_states", address.getAcceptable_states());
          addressJson.put("acceptable_counties", address.getAcceptable_counties());
          response.put(address.name(), addressJson);
        }
        ctx.result(response.toString());
      };

  public Handler saveMail =
      ctx -> {
        JSONObject request = new JSONObject(ctx.body());
        String username = request.getString("username");
        String loggedInUser = ctx.sessionAttribute("username");
        String organizationName =
            ctx.sessionAttribute("orgName") != null
                ? ctx.sessionAttribute("orgName")
                : "";
        String formMailAddressString = request.getString("mailKey");
        FormMailAddress formMailAddress =
            Arrays.stream(FormMailAddress.values())
                .filter(e -> e.name().equalsIgnoreCase(formMailAddressString))
                .findFirst()
                .orElseThrow();
        String fileId = request.getString("fileId");

        ReturnAddress returnAddress = null;
        if (request.has("returnAddress")) {
          JSONObject ra = request.getJSONObject("returnAddress");
          returnAddress =
              new ReturnAddress(
                  ra.optString("officeName", ra.optString("name", "")),
                  ra.optString("street1", ""),
                  ra.optString("street2", ""),
                  ra.optString("city", ""),
                  ra.optString("state", ""),
                  ra.optString("zipcode", ""));
        }

        int costCents = computeCostCents(formMailAddress);

        SubmitToLobMailService submitToLobMailService =
            new SubmitToLobMailService(
                fileDao,
                mailDao,
                mailSender,
                formMailAddress,
                fileId,
                username,
                loggedInUser,
                organizationName,
                encryptionController,
                returnAddress,
                costCents);
        try {
          Message response = submitToLobMailService.executeAndGetResponse();
          ctx.result(response.toJSON().toString());
        } catch (Exception e) {
          log.error("saveMail failed: {}", e.getMessage(), e);
          ctx.status(200);
          ctx.result(
              MailMessage.FAILED_WHEN_SENDING_MAIL
                  .toJSON(e.getMessage())
                  .toString());
        }
      };

  public Handler getApplicationMailInfo =
      ctx -> {
        JSONObject request = new JSONObject(ctx.body());
        String fileId = request.getString("fileId");
        Optional<Form> formOpt = formDao.getByFileId(new ObjectId(fileId));
        JSONObject response = new JSONObject();
        if (formOpt.isPresent()) {
          Map<String, String> metadata = formOpt.get().getApplicationMetadata();
          if (metadata != null) {
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
              response.put(entry.getKey(), entry.getValue());
            }
          }
        }
        ctx.result(response.toString());
      };

  public Handler getMailHistory =
      ctx -> {
        JSONObject request = new JSONObject(ctx.body());
        String fileId = request.getString("fileId");
        List<Mail> mails = mailDao.getByFileId(new ObjectId(fileId));
        JSONArray result = new JSONArray();
        for (Mail mail : mails) {
          result.put(mailToJson(mail));
        }
        ctx.result(result.toString());
      };

  public Handler refreshMailStatus =
      ctx -> {
        JSONObject request = new JSONObject(ctx.body());
        String mailId = request.getString("mailId");
        Optional<Mail> mailOpt = mailDao.get(new ObjectId(mailId));
        if (mailOpt.isEmpty()) {
          ctx.status(404).result(new JSONObject().put("error", "Mail not found").toString());
          return;
        }
        Mail mail = mailOpt.get();
        if (mail.getLobId() == null || mail.getLobId().startsWith("noop_")) {
          ctx.result(mailToJson(mail).toString());
          return;
        }
        boolean isCheck = "check".equals(mail.getMailType());
        MailResult result = mailSender.refreshStatus(mail.getLobId(), isCheck);
        mail.applyRefreshResult(result);
        mailDao.update(mail);
        ctx.result(mailToJson(mail).toString());
      };

  public Handler getOrgMailSummary =
      ctx -> {
        JSONObject request = new JSONObject(ctx.body());
        String organizationName = request.getString("organizationName");

        List<Mail> mails;
        if (request.has("fromDate") && request.has("toDate")) {
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
          Date from = sdf.parse(request.getString("fromDate"));
          Date to = sdf.parse(request.getString("toDate"));
          Calendar cal = Calendar.getInstance();
          cal.setTime(to);
          cal.set(Calendar.HOUR_OF_DAY, 23);
          cal.set(Calendar.MINUTE, 59);
          cal.set(Calendar.SECOND, 59);
          cal.set(Calendar.MILLISECOND, 999);
          to = cal.getTime();
          mails = mailDao.getByOrganization(organizationName, from, to);
        } else {
          mails = mailDao.getByOrganization(organizationName);
        }

        JSONArray items = new JSONArray();
        int totalLetters = 0;
        int totalChecks = 0;
        int totalMailingCostCents = 0;
        BigDecimal totalCheckAmount = BigDecimal.ZERO;

        for (Mail mail : mails) {
          items.put(mailToJson(mail));
          if ("check".equals(mail.getMailType())) {
            totalChecks++;
            if (mail.getCheckAmount() != null) {
              try {
                totalCheckAmount = totalCheckAmount.add(new BigDecimal(mail.getCheckAmount()));
              } catch (NumberFormatException ignored) {
              }
            }
          } else {
            totalLetters++;
          }
          totalMailingCostCents += mail.getCostCents();
        }

        JSONObject response = new JSONObject();
        response.put("items", items);
        response.put("totalLetters", totalLetters);
        response.put("totalChecks", totalChecks);
        response.put("totalMailingCostCents", totalMailingCostCents);
        response.put("totalCheckAmount", totalCheckAmount.toPlainString());
        ctx.result(response.toString());
      };

  private JSONObject mailToJson(Mail mail) {
    JSONObject json = new JSONObject();
    json.put("id", mail.getId().toString());
    json.put("fileId", mail.getFileId().toString());
    json.put("mailStatus", mail.getMailStatus().name());
    json.put("lobId", mail.getLobId());
    json.put("lobCreatedAt", mail.getLobCreatedAt() != null ? mail.getLobCreatedAt().getTime() : JSONObject.NULL);
    json.put("expectedDeliveryDate", mail.getExpectedDeliveryDate());
    json.put("lobStatus", mail.getLobStatus());
    json.put("costCents", mail.getCostCents());
    json.put("mailType", mail.getMailType());
    json.put("checkAmount", mail.getCheckAmount());
    json.put("requesterUsername", mail.getRequesterUsername());
    json.put("targetUsername", mail.getTargetUsername());
    json.put("organizationName", mail.getOrganizationName());
    if (mail.getMailingAddress() != null) {
      json.put("mailingAddressName", mail.getMailingAddress().getName());
    }
    JSONArray eventsArray = new JSONArray();
    for (TrackingEvent event : mail.getTrackingEvents()) {
      JSONObject eventJson = new JSONObject();
      eventJson.put("type", event.getType());
      eventJson.put("name", event.getName());
      eventJson.put("time", event.getTime() != null ? event.getTime().getTime() : JSONObject.NULL);
      eventJson.put("location", event.getLocation());
      eventsArray.put(eventJson);
    }
    json.put("trackingEvents", eventsArray);
    return json;
  }

  private int computeCostCents(FormMailAddress formMailAddress) {
    boolean isCheck = formMailAddress.getMaybeCheckAmount().compareTo(BigDecimal.ZERO) > 0;
    if (isCheck) {
      String costEnv = System.getenv("LOB_COST_PER_CHECK_CENTS");
      return costEnv != null ? Integer.parseInt(costEnv) : 116;
    } else {
      String costEnv = System.getenv("LOB_COST_PER_LETTER_CENTS");
      return costEnv != null ? Integer.parseInt(costEnv) : 81;
    }
  }
}
