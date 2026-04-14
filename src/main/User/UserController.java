package User;

import Config.Message;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.Notification.NotificationDao;
import Database.Organization.OrgDao;
import Database.Token.TokenDao;
import Database.User.UserDao;
import Organization.Organization;
import io.jsonwebtoken.Claims;
import File.File;
import File.FileMessage;
import File.FileType;
import File.IdCategoryType;
import File.Services.DownloadFileService;
import File.Services.UploadFileService;
import Security.EmailSender;
import Security.EmailSenderFactory;
import Security.EmailUtil;
import Security.SecurityUtils;
import User.Onboarding.OnboardingStatus;
import User.Services.*;
import Validation.ValidationUtils;
import static User.UserMessage.*;
import static User.UserType.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.MongoDatabase;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONObject;

@Slf4j
public class UserController {
  MongoDatabase db;
  UserDao userDao;
  TokenDao tokenDao;
  ActivityDao activityDao;
  FormDao formDao;
  FileDao fileDao;
  OrgDao orgDao;
  NotificationDao notificationDao;
  EmailSender emailSender;

  public UserController(
      UserDao userDao,
      TokenDao tokenDao,
      FileDao fileDao,
      ActivityDao activityDao,
      FormDao formDao,
      OrgDao orgDao,
      MongoDatabase db) {
    this(userDao, tokenDao, fileDao, activityDao, formDao, orgDao, null, db, EmailSenderFactory.smtp());
  }

  public UserController(
      UserDao userDao,
      TokenDao tokenDao,
      FileDao fileDao,
      ActivityDao activityDao,
      FormDao formDao,
      OrgDao orgDao,
      MongoDatabase db,
      EmailSender emailSender) {
    this(userDao, tokenDao, fileDao, activityDao, formDao, orgDao, null, db, emailSender);
  }

  public UserController(
      UserDao userDao,
      TokenDao tokenDao,
      FileDao fileDao,
      ActivityDao activityDao,
      FormDao formDao,
      OrgDao orgDao,
      NotificationDao notificationDao,
      MongoDatabase db,
      EmailSender emailSender) {
    this.userDao = userDao;
    this.tokenDao = tokenDao;
    this.fileDao = fileDao;
    this.activityDao = activityDao;
    this.formDao = formDao;
    this.orgDao = orgDao;
    this.notificationDao = notificationDao;
    this.db = db;
    this.emailSender = emailSender;
  }

  public static final String newUserActualURL = Objects.requireNonNull(System.getenv("NEW_USER_ACTUALURL"));
  public static final String newUserTestURL = Objects.requireNonNull(System.getenv("NEW_USER_TESTURL"));

  public Handler ingestCsv = ctx -> {
    String csvName = "Face to Face Birth Certificate Clinic Signups.csv";
    String orgName = "Face to Face";
    String creatorUsername = "FACE-TO-FACE-ADMIN";
    String defaultBirthdate = "01-01-2025";
    String faceToFaceAddress = "123 E Price St";
    String faceToFaceCity = "Philadelphia";
    String faceToFaceState = "PA";
    String faceToFaceZip = "19144";
    List<String[]> records = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader(csvName))) {
      String line;
      String header = br.readLine();
      while ((line = br.readLine()) != null) {
        String[] values = line.split(",");
        if (values.length >= 4) {
          String[] entry = new String[4];
          entry[0] = values[0].trim();
          entry[1] = values[1].trim();
          entry[2] = values[2].trim();
          entry[3] = values[3].trim();
          records.add(entry);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    for (String[] record : records) {
      String clientFirstName = record[0];
      String clientLastName = record[1];
      String clientEmail = record[2];
      String clientPhoneNumber = record[3];
      String clientUsername = clientFirstName.toLowerCase() + "-" + clientLastName.toLowerCase();
      String clientPassword = clientFirstName.toLowerCase()
          + clientLastName.toLowerCase()
          + clientPhoneNumber.substring(clientPhoneNumber.length() - 4);

      Name clientName = new Name(clientFirstName, clientLastName);
      Address clientAddress = new Address(faceToFaceAddress, faceToFaceCity, faceToFaceState, faceToFaceZip);

      CreateUserService createUserService = new CreateUserService(
          userDao, activityDao, orgDao, Admin, orgName, creatorUsername,
          clientName, defaultBirthdate, clientEmail, clientPhoneNumber,
          clientAddress, false, clientUsername, clientPassword, Client);
      Message response = createUserService.executeAndGetResponse();
      System.out.println(response.toResponseString());
    }
    ctx.result("SUCCESS");
  };

  public Handler loginUser = ctx -> {
    ctx.req.getSession().invalidate();
    JSONObject req = new JSONObject(ctx.body());
    String loginIdentifier = req.optString("username", req.optString("email", ""));
    String password = req.getString("password");
    String ip = ctx.ip();
    String userAgent = ctx.userAgent();
    log.info("Attempting to login " + loginIdentifier);

    LoginService loginService =
        new LoginService(userDao, activityDao, loginIdentifier, password, ip, userAgent);
    Message response = loginService.executeAndGetResponse();
    log.info(response.toString() + response.getErrorDescription());
    JSONObject responseJSON = response.toJSON();
    if (response == UserMessage.AUTH_SUCCESS) {
      responseJSON.put("username", loginService.getUsername());
      responseJSON.put("userRole", loginService.getUserRole());
      responseJSON.put("organization", loginService.getOrganization());
      responseJSON.put("firstName", loginService.getFirstName());
      responseJSON.put("lastName", loginService.getLastName());

      ctx.sessionAttribute("privilegeLevel", loginService.getUserRole());
      ctx.sessionAttribute("orgName", loginService.getOrganization());
      ctx.sessionAttribute("username", loginService.getUsername());
      ctx.sessionAttribute("fullName", loginService.getFullName());
      userDao
          .get(loginService.getUsername())
          .ifPresent(
              u -> {
                OrganizationIdResolver.resolveAndPersistIfMissing(u, orgDao, userDao)
                    .ifPresent(oid -> ctx.sessionAttribute("organizationId", oid.toHexString()));
              });
    } else {
      responseJSON.put("username", "");
      responseJSON.put("userRole", "");
      responseJSON.put("organization", "");
      responseJSON.put("firstName", "");
      responseJSON.put("lastName", "");
    }
    ctx.result(responseJSON.toString());
  };

  public Handler googleLoginRequestHandler = ctx -> {
    ctx.req.getSession().invalidate();
    JSONObject req = new JSONObject(ctx.body());
    String redirectUri = req.optString("redirectUri", null);
    String originUri = req.optString("originUri", null);
    log.info("Processing Google login request with redirect URI: {}, origin URI: {}", redirectUri, originUri);

    ProcessGoogleLoginRequestService processGoogleLoginRequestService = new ProcessGoogleLoginRequestService(
        redirectUri, originUri);
    Message response = processGoogleLoginRequestService.executeAndGetResponse();
    JSONObject responseJSON = response.toJSON();
    log.info("Google login request processed with status: {}", response.getErrorName());

    if (response == GoogleLoginRequestMessage.REQUEST_SUCCESS) {
      log.info("Setting session attributes");
      ctx.sessionAttribute("origin_uri", originUri);
      ctx.sessionAttribute("redirect_uri", redirectUri);
      ctx.sessionAttribute("PKCECodeVerifier", processGoogleLoginRequestService.getCodeVerifier());
      ctx.sessionAttribute("state", processGoogleLoginRequestService.getCsrfToken());

      responseJSON.put("codeChallenge", processGoogleLoginRequestService.getCodeChallenge());
      responseJSON.put("state", processGoogleLoginRequestService.getCsrfToken());
      ctx.result(responseJSON.toString());
    }
    ctx.result(responseJSON.toString());
  };

  public Handler googleLoginResponseHandler = ctx -> {
    String authCode = ctx.queryParam("code");
    String state = ctx.queryParam("state");
    String ip = ctx.ip();
    String userAgent = ctx.userAgent();
    String codeVerifier = ctx.sessionAttribute("PKCECodeVerifier");
    String originUri = ctx.sessionAttribute("origin_uri");
    String redirectUri = ctx.sessionAttribute("redirect_uri");
    String storedCsrfToken = ctx.sessionAttribute("state");

    ProcessGoogleLoginResponseService processGoogleLoginResponseService = new ProcessGoogleLoginResponseService(
        userDao, activityDao, state, storedCsrfToken, authCode, codeVerifier, originUri, redirectUri, ip, userAgent);
    Message response = processGoogleLoginResponseService.executeAndGetResponse();

    if (response == GoogleLoginResponseMessage.AUTH_SUCCESS) {
      ctx.sessionAttribute("privilegeLevel", processGoogleLoginResponseService.getUserRole());
      ctx.sessionAttribute("orgName", processGoogleLoginResponseService.getOrganization());
      ctx.sessionAttribute("username", processGoogleLoginResponseService.getUsername());
      ctx.sessionAttribute("fullName", processGoogleLoginResponseService.getFullName());
      ctx.sessionAttribute("googleLoginError", null);
      userDao
          .get(processGoogleLoginResponseService.getUsername())
          .ifPresent(
              u -> {
                OrganizationIdResolver.resolveAndPersistIfMissing(u, orgDao, userDao)
                    .ifPresent(oid -> ctx.sessionAttribute("organizationId", oid.toHexString()));
              });
    } else {
      ctx.sessionAttribute("googleLoginError", response.getErrorName());
      if (response == GoogleLoginResponseMessage.USER_NOT_FOUND) {
        ctx.sessionAttribute("googleEmail", processGoogleLoginResponseService.getGoogleEmail());
        ctx.sessionAttribute("googleFirstName", processGoogleLoginResponseService.getGoogleFirstName());
        ctx.sessionAttribute("googleLastName", processGoogleLoginResponseService.getGoogleLastName());
      }
    }
    ctx.sessionAttribute("PKCECodeVerifier", null);
    ctx.sessionAttribute("origin_uri", null);
    ctx.sessionAttribute("redirect_uri", null);
    ctx.sessionAttribute("state", null);
    ctx.redirect(processGoogleLoginResponseService.getOrigin() + "/login");
  };

  public Handler microsoftLoginRequestHandler = ctx -> {
    ctx.req.getSession().invalidate();
    JSONObject req = new JSONObject(ctx.body());
    String redirectUri = req.optString("redirectUri", null);
    String originUri = req.optString("originUri", null);
    log.info("Processing Microsoft login request with redirect URI: {}, origin URI: {}", redirectUri, originUri);

    ProcessMicrosoftLoginRequestService processMicrosoftLoginRequestService =
        new ProcessMicrosoftLoginRequestService(redirectUri, originUri);
    Message response = processMicrosoftLoginRequestService.executeAndGetResponse();
    JSONObject responseJSON = response.toJSON();
    log.info("Microsoft login request processed with status: {}", response.getErrorName());

    if (response == MicrosoftLoginRequestMessage.REQUEST_SUCCESS) {
      ctx.sessionAttribute("origin_uri", originUri);
      ctx.sessionAttribute("redirect_uri", redirectUri);
      ctx.sessionAttribute("PKCECodeVerifier", processMicrosoftLoginRequestService.getCodeVerifier());
      ctx.sessionAttribute("state", processMicrosoftLoginRequestService.getCsrfToken());
      responseJSON.put("codeChallenge", processMicrosoftLoginRequestService.getCodeChallenge());
      responseJSON.put("state", processMicrosoftLoginRequestService.getCsrfToken());
    }
    ctx.result(responseJSON.toString());
  };

  public Handler microsoftLoginResponseHandler = ctx -> {
    String authCode = ctx.queryParam("code");
    String state = ctx.queryParam("state");
    String ip = ctx.ip();
    String userAgent = ctx.userAgent();
    String codeVerifier = ctx.sessionAttribute("PKCECodeVerifier");
    String originUri = ctx.sessionAttribute("origin_uri");
    String redirectUri = ctx.sessionAttribute("redirect_uri");
    String storedCsrfToken = ctx.sessionAttribute("state");

    ProcessMicrosoftLoginResponseService processMicrosoftLoginResponseService =
        new ProcessMicrosoftLoginResponseService(
            userDao,
            activityDao,
            state,
            storedCsrfToken,
            authCode,
            codeVerifier,
            originUri,
            redirectUri,
            ip,
            userAgent);
    Message response = processMicrosoftLoginResponseService.executeAndGetResponse();

    if (response == MicrosoftLoginResponseMessage.AUTH_SUCCESS) {
      ctx.sessionAttribute("privilegeLevel", processMicrosoftLoginResponseService.getUserRole());
      ctx.sessionAttribute("orgName", processMicrosoftLoginResponseService.getOrganization());
      ctx.sessionAttribute("username", processMicrosoftLoginResponseService.getUsername());
      ctx.sessionAttribute("fullName", processMicrosoftLoginResponseService.getFullName());
      ctx.sessionAttribute("microsoftLoginError", null);
      userDao
          .get(processMicrosoftLoginResponseService.getUsername())
          .ifPresent(
              u -> {
                OrganizationIdResolver.resolveAndPersistIfMissing(u, orgDao, userDao)
                    .ifPresent(oid -> ctx.sessionAttribute("organizationId", oid.toHexString()));
              });
    } else {
      ctx.sessionAttribute("microsoftLoginError", response.getErrorName());
      if (response == MicrosoftLoginResponseMessage.USER_NOT_FOUND) {
        ctx.sessionAttribute("microsoftEmail", processMicrosoftLoginResponseService.getMicrosoftEmail());
        ctx.sessionAttribute("microsoftFirstName", processMicrosoftLoginResponseService.getMicrosoftFirstName());
        ctx.sessionAttribute("microsoftLastName", processMicrosoftLoginResponseService.getMicrosoftLastName());
      }
    }

    ctx.sessionAttribute("PKCECodeVerifier", null);
    ctx.sessionAttribute("origin_uri", null);
    ctx.sessionAttribute("redirect_uri", null);
    ctx.sessionAttribute("state", null);
    ctx.redirect(processMicrosoftLoginResponseService.getOrigin() + "/login");
  };

  public Handler getSessionUser = ctx -> {
    JSONObject responseJSON = new JSONObject();
    String org = ctx.sessionAttribute("orgName");
    String username = ctx.sessionAttribute("username");
    String fullName = ctx.sessionAttribute("fullName");
    UserType role = ctx.sessionAttribute("privilegeLevel");
    String googleLoginError = ctx.sessionAttribute("googleLoginError");
    String microsoftLoginError = ctx.sessionAttribute("microsoftLoginError");

    responseJSON.put("organization", org == null ? "" : org);
    String organizationIdHex = ctx.sessionAttribute("organizationId");
    responseJSON.put("organizationId", organizationIdHex == null ? "" : organizationIdHex);
    responseJSON.put("username", username == null ? "" : username);
    responseJSON.put("fullName", fullName == null ? "" : fullName);
    responseJSON.put("userRole", role == null ? "" : role);
    if (googleLoginError != null) {
      responseJSON.put("googleLoginError", googleLoginError);
      ctx.sessionAttribute("googleLoginError", null);
      String googleEmail = ctx.sessionAttribute("googleEmail");
      String googleFirstName = ctx.sessionAttribute("googleFirstName");
      String googleLastName = ctx.sessionAttribute("googleLastName");
      if (googleEmail != null) {
        responseJSON.put("googleEmail", googleEmail);
        responseJSON.put("googleFirstName", googleFirstName != null ? googleFirstName : "");
        responseJSON.put("googleLastName", googleLastName != null ? googleLastName : "");
        ctx.sessionAttribute("googleEmail", null);
        ctx.sessionAttribute("googleFirstName", null);
        ctx.sessionAttribute("googleLastName", null);
      }
    }
    if (microsoftLoginError != null) {
      responseJSON.put("microsoftLoginError", microsoftLoginError);
      ctx.sessionAttribute("microsoftLoginError", null);
      String microsoftEmail = ctx.sessionAttribute("microsoftEmail");
      String microsoftFirstName = ctx.sessionAttribute("microsoftFirstName");
      String microsoftLastName = ctx.sessionAttribute("microsoftLastName");
      if (microsoftEmail != null) {
        responseJSON.put("microsoftEmail", microsoftEmail);
        responseJSON.put("microsoftFirstName", microsoftFirstName != null ? microsoftFirstName : "");
        responseJSON.put("microsoftLastName", microsoftLastName != null ? microsoftLastName : "");
        ctx.sessionAttribute("microsoftEmail", null);
        ctx.sessionAttribute("microsoftFirstName", null);
        ctx.sessionAttribute("microsoftLastName", null);
      }
    }
    ctx.result(responseJSON.toString());
  };

  public Handler authenticateUser = ctx -> {
    String sessionUsername = ctx.sessionAttribute("username");
    AuthenticateUserService authenticateUserService = new AuthenticateUserService(userDao, sessionUsername);
    Message response = authenticateUserService.executeAndGetResponse();
    JSONObject responseJSON = response.toJSON();
    if (response == UserMessage.AUTH_SUCCESS) {
      responseJSON.put("username", authenticateUserService.getUsername());
      responseJSON.put("userRole", authenticateUserService.getUserRole());
      responseJSON.put("organization", authenticateUserService.getOrganization());
      responseJSON.put("firstName", authenticateUserService.getFirstName());
      responseJSON.put("lastName", authenticateUserService.getLastName());
    } else {
      responseJSON.put("username", "");
      responseJSON.put("userRole", "");
      responseJSON.put("organization", "");
      responseJSON.put("firstName", "");
      responseJSON.put("lastName", "");
    }
    ctx.result(responseJSON.toString());
  };

  public Handler usernameExists = ctx -> {
    JSONObject req = new JSONObject(ctx.body());
    String username = req.getString("username");
    CheckUsernameExistsService checkUsernameExistsService = new CheckUsernameExistsService(userDao, username);
    ctx.result(checkUsernameExistsService.executeAndGetResponse().toResponseString());
  };

  public Handler createNewUser = ctx -> {
    log.info("Starting createNewUser handler");
    JSONObject req = new JSONObject(ctx.body());
    UserType sessionUserLevel = ctx.sessionAttribute("privilegeLevel");
    String organizationName = ctx.sessionAttribute("orgName");
    String sessionUsername = ctx.sessionAttribute("username");
    String firstName = req.getString("firstname").strip();
    String lastName = req.getString("lastname").strip();
    String birthDate = req.getString("birthDate").strip();
    String email = req.getString("email").toLowerCase().strip();
    String phone = req.getString("phonenumber").strip();
    String addressLine1 = req.getString("address").strip();
    String city = req.getString("city").strip();
    String state = req.getString("state").strip();
    String zipcode = req.getString("zipcode").strip();
    Boolean twoFactorOn = req.getBoolean("twoFactorOn");
    String username = req.getString("username").strip();
    String password = req.getString("password").strip();
    String userTypeString = req.getString("personRole").strip();
    UserType userType = UserType.userTypeFromString(userTypeString);

    Name currentName = new Name(firstName, lastName);
    Address personalAddress = new Address(addressLine1, city, state, zipcode);

    CreateUserService createUserService = new CreateUserService(
        userDao, activityDao, orgDao, sessionUserLevel, organizationName, sessionUsername,
        currentName, birthDate, email, phone, personalAddress,
        twoFactorOn, username, password, userType);
    Message response = createUserService.executeAndGetResponse();
    ctx.result(response.toJSON().toString());
  };

  public Handler enrollClient = ctx -> {
    log.info("Starting enrollClient handler");
    JSONObject req = new JSONObject(ctx.body());

    UserType sessionUserLevel = ctx.sessionAttribute("privilegeLevel");
    String organizationName = ctx.sessionAttribute("orgName");
    String sessionUsername = ctx.sessionAttribute("username");

    if (sessionUserLevel == null || organizationName == null || sessionUsername == null) {
      ctx.result(SESSION_TOKEN_FAILURE.toJSON().toString());
      return;
    }

    String firstName = req.getString("firstname").strip();
    String middleName = req.optString("middlename", "").strip();
    String lastName = req.getString("lastname").strip();
    String suffix = req.optString("suffix", "").strip();
    String birthDate = req.getString("birthDate").strip();
    String email = "";
    if (req.has("email") && !req.isNull("email")) {
      email = req.optString("email", "").strip().toLowerCase(Locale.ROOT);
    }
    String phone = req.optString("phonenumber", "").strip();

    String dobCompact = birthDate.replace("-", "");
    String randomSuffix = UUID.randomUUID().toString().substring(0, 4);
    String username =
        ValidationUtils.slugForEnrollmentUsernameSegment(firstName)
            + "-"
            + ValidationUtils.slugForEnrollmentUsernameSegment(lastName)
            + "-"
            + dobCompact
            + "-"
            + randomSuffix;
    String password = UUID.randomUUID().toString();

    Name currentName = new Name(firstName, middleName, lastName, suffix, null);

    CreateUserService createUserService = new CreateUserService(
        userDao, activityDao, orgDao, sessionUserLevel, organizationName, sessionUsername,
        currentName, birthDate, email, phone, null,
        false, username, password, UserType.Client);
    Message createResponse = createUserService.executeAndGetResponse();

    if (createResponse != ENROLL_SUCCESS) {
      ctx.result(createResponse.toJSON().toString());
      return;
    }

    ctx.result(ENROLL_SUCCESS.toJSON().toString());
  };

  public Handler enrollWorker = ctx -> {
    log.info("Starting enrollWorker handler");
    JSONObject req = new JSONObject(ctx.body());

    UserType sessionUserLevel = ctx.sessionAttribute("privilegeLevel");
    String organizationName = ctx.sessionAttribute("orgName");
    String sessionUsername = ctx.sessionAttribute("username");

    if (sessionUserLevel == null || organizationName == null || sessionUsername == null) {
      ctx.result(SESSION_TOKEN_FAILURE.toJSON().toString());
      return;
    }

    String firstName = req.getString("firstname").strip();
    String middleName = req.optString("middlename", "").strip();
    String lastName = req.getString("lastname").strip();
    String suffix = req.optString("suffix", "").strip();
    String maiden = req.optString("maiden", "").strip();
    String birthDate = req.getString("birthDate").strip();
    String email = req.getString("email").toLowerCase().strip();
    String phone = req.optString("phonenumber", "").strip();
    String personRole = req.getString("personRole").strip();

    UserType userType = UserType.userTypeFromString(personRole);
    if (userType != UserType.Worker && userType != UserType.Admin) {
      ctx.result(INVALID_PARAMETER.toJSON().toString());
      return;
    }

    String dobCompact = birthDate.replace("-", "");
    String randomSuffix = UUID.randomUUID().toString().substring(0, 4);
    String username =
        ValidationUtils.slugForEnrollmentUsernameSegment(firstName)
            + "-"
            + ValidationUtils.slugForEnrollmentUsernameSegment(lastName)
            + "-"
            + dobCompact
            + "-"
            + randomSuffix;
    String password = UUID.randomUUID().toString();

    Name currentName = new Name(firstName, middleName, lastName, suffix, maiden);

    CreateUserService createUserService = new CreateUserService(
        userDao, activityDao, orgDao, sessionUserLevel, organizationName, sessionUsername,
        currentName, birthDate, email, phone, null,
        false, username, password, userType);
    Message createResponse = createUserService.executeAndGetResponse();

    if (createResponse != ENROLL_SUCCESS) {
      ctx.result(createResponse.toJSON().toString());
      return;
    }

    try {
      String emailBody = EmailUtil.getEnrollmentWelcomeEmail(firstName, organizationName);
      emailSender.sendEmail("Keep Id", email,
          "You've been added to " + organizationName + " on Keep.id", emailBody);
    } catch (Exception e) {
      log.warn("Failed to send welcome email to {}: {}", email, e.getMessage());
    }

    ctx.result(ENROLL_SUCCESS.toJSON().toString());
  };

  public Handler deleteUser = ctx -> {
    log.info("Starting deleteUser handler");
    JSONObject req = new JSONObject(ctx.body());
    String sessionUsername = ctx.sessionAttribute("username");
    String password = req.getString("password").strip();

    DeleteUserService deleteUserService = new DeleteUserService(db, userDao, sessionUsername, password);
    Message response = deleteUserService.executeAndGetResponse();
    ctx.result(response.toJSON().toString());
  };

  public Handler createNewInvitedUser = ctx -> {
    log.info("Starting createNewInvitedUser handler");
    JSONObject req = new JSONObject(ctx.body());

    String inviteJwt = req.optString("inviteJwt", "").strip();
    if (inviteJwt.isEmpty()) {
      ctx.result(INVALID_PARAMETER.toJSON().toString());
      return;
    }

    final Claims claims;
    try {
      claims = SecurityUtils.decodeJWT(inviteJwt);
    } catch (Exception e) {
      log.warn("Invalid invite JWT", e);
      ctx.result(INVALID_PARAMETER.toJSON().toString());
      return;
    }

    String roleStr = claims.get("role", String.class);
    UserType userType = userTypeFromString(roleStr == null ? "" : roleStr.strip());
    if (userType == null) {
      ctx.result(INVALID_PRIVILEGE_TYPE.toJSON().toString());
      return;
    }

    String orgIdHex = claims.get("organizationId", String.class);
    Optional<Organization> organizationOpt;
    if (orgIdHex != null && ObjectId.isValid(orgIdHex.strip())) {
      organizationOpt = orgDao.get(new ObjectId(orgIdHex.strip()));
    } else {
      String orgNameClaim = claims.get("organization", String.class);
      if (orgNameClaim == null || orgNameClaim.isBlank()) {
        ctx.result(INVALID_PARAMETER.toJSON().toString());
        return;
      }
      organizationOpt = orgDao.get(orgNameClaim.strip());
    }
    if (organizationOpt.isEmpty()) {
      ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
      return;
    }
    String organizationName = organizationOpt.get().getOrgName();

    String firstName = req.getString("firstname").strip();
    String lastName = req.getString("lastname").strip();
    String birthDate = req.getString("birthDate").strip();
    String email = req.getString("email").strip();
    String phone = req.getString("phonenumber").strip();
    String addressLine1 = req.getString("address").strip();
    String city = req.getString("city").strip();
    String state = req.getString("state").strip();
    String zipcode = req.getString("zipcode").strip();
    Boolean twoFactorOn = req.getBoolean("twoFactorOn");
    String username = req.getString("username").strip();
    String password = req.getString("password").strip();

    Name currentName = new Name(firstName, lastName);
    Address personalAddress = new Address(addressLine1, city, state, zipcode);

    CreateUserService createUserService = new CreateUserService(
        userDao, activityDao, orgDao, Director, organizationName, null,
        currentName, birthDate, email, phone, personalAddress,
        twoFactorOn, username, password, userType);
    Message response = createUserService.executeAndGetResponse();
    ctx.result(response.toJSON().toString());
  };

  public Handler logout = ctx -> {
    ctx.req.getSession().invalidate();
    log.info("Signed out");
    ctx.result(UserMessage.SUCCESS.toJSON().toString());
  };

  public Handler getUserInfo = ctx -> {
    log.info("Started getUserInfo handler");

    String targetUsername = null;
    String body = ctx.body();
    if (body != null && !body.trim().isEmpty()) {
      try {
        JSONObject req = new JSONObject(body);
        targetUsername = req.optString("username", null);
        if (targetUsername != null && targetUsername.isEmpty()) {
          targetUsername = null;
        }
      } catch (Exception e) {
        log.info("Could not parse request body, using ctx username");
      }
    }

    Message authCheck = checkProfileAuthorization(ctx, targetUsername);
    if (authCheck != null) {
      ctx.result(authCheck.toJSON().toString());
      return;
    }

    String username = targetUsername != null ? targetUsername : ctx.sessionAttribute("username");

    GetUserInfoService infoService = new GetUserInfoService(userDao, username);
    Message response = infoService.executeAndGetResponse();
    if (response != UserMessage.SUCCESS) {
      ctx.result(response.toJSON().toString());
    } else {
      JSONObject userInfo = infoService.getUserFields();
      JSONObject mergedInfo = mergeJSON(response.toJSON(), userInfo);
      ctx.result(mergedInfo.toString());
    }
  };

  public Handler getOrganizationInfo = ctx -> {
    log.info("Started getOrganizationInfo handler");
    JSONObject req = new JSONObject(ctx.body());
    String sessionUsername = ctx.sessionAttribute("username");
    String sessionOrgName = ctx.sessionAttribute("orgName");

    if (sessionUsername == null || sessionUsername.isEmpty()) {
      ctx.result(AUTH_FAILURE.toJSON().toString());
      return;
    }

    String requestedOrgName = req.optString("orgName", null);
    if (requestedOrgName == null || requestedOrgName.isEmpty()) {
      ctx.result(USER_NOT_FOUND.toJSON().toString());
      return;
    }

    if (!requestedOrgName.equals(sessionOrgName)) {
      ctx.result(CROSS_ORG_ACTION_DENIED.toJSON().toString());
      return;
    }

    Optional<Organization> orgOpt = orgDao.get(requestedOrgName);
    if (orgOpt.isEmpty()) {
      ctx.result(USER_NOT_FOUND.toJSON().toString());
      return;
    }

    Organization org = orgOpt.get();
    Address orgAddr = org.getOrgAddress();
    String addressStr = orgAddr != null ? orgAddr.toString() : "";

    JSONObject res = new JSONObject();
    res.put("status", "SUCCESS");
    res.put("name", org.getOrgName());
    res.put("address", addressStr);
    res.put("orgAddress", orgAddr != null ? orgAddr.serialize() : JSONObject.NULL);
    res.put("phone", org.getOrgPhoneNumber() != null ? org.getOrgPhoneNumber() : "");
    res.put("email", org.getOrgEmail() != null ? org.getOrgEmail() : "");
    ctx.result(res.toString());
  };

  public Handler updateOrganizationInfo = ctx -> {
    log.info("Started updateOrganizationInfo handler");
    JSONObject req = new JSONObject(ctx.body());
    String sessionUsername = ctx.sessionAttribute("username");
    String sessionOrgName = ctx.sessionAttribute("orgName");
    UserType privilegeLevel = ctx.sessionAttribute("privilegeLevel");

    if (sessionUsername == null || sessionUsername.isEmpty()) {
      ctx.result(AUTH_FAILURE.toJSON().toString());
      return;
    }

    String requestedOrgName = req.optString("orgName", null);
    if (requestedOrgName == null || requestedOrgName.isEmpty()) {
      ctx.result(USER_NOT_FOUND.toJSON().toString());
      return;
    }

    if (!requestedOrgName.equals(sessionOrgName)) {
      ctx.result(CROSS_ORG_ACTION_DENIED.toJSON().toString());
      return;
    }

    if (privilegeLevel != UserType.Admin && privilegeLevel != UserType.Director) {
      ctx.result(INSUFFICIENT_PRIVILEGE.toJSON().toString());
      return;
    }

    Optional<Organization> orgOpt = orgDao.get(requestedOrgName);
    if (orgOpt.isEmpty()) {
      ctx.result(USER_NOT_FOUND.toJSON().toString());
      return;
    }

    Organization org = orgOpt.get();

    if (req.has("address")) {
      JSONObject addressJson = req.optJSONObject("address");
      if (addressJson != null) {
        Address currentAddress = org.getOrgAddress();
        if (currentAddress == null) {
          currentAddress = new Address("", "", "", "", "", "");
        }
        currentAddress.setLine1(addressJson.optString("line1", ""));
        currentAddress.setLine2(addressJson.optString("line2", ""));
        currentAddress.setCity(addressJson.optString("city", ""));
        currentAddress.setState(addressJson.optString("state", ""));
        currentAddress.setZip(addressJson.optString("zip", ""));
        currentAddress.setCounty(addressJson.optString("county", ""));
        org.setOrgAddress(currentAddress);
      }
    }

    if (req.has("phone")) {
      org.setOrgPhoneNumber(req.optString("phone", ""));
    }

    if (req.has("email")) {
      org.setOrgEmail(req.optString("email", ""));
    }

    if (req.has("newName") && !req.getString("newName").isBlank()) {
       String newName = req.getString("newName");
       if (!newName.equals(org.getOrgName())) {
           org.setOrgName(newName);
           // Also update session attribute if they themselves changed it
           ctx.sessionAttribute("orgName", newName);
       }
    }

    orgDao.update(org);

    JSONObject res = new JSONObject();
    res.put("status", "SUCCESS");
    ctx.result(res.toString());
  };

  public Handler getMembers = ctx -> {
    log.info("Started getMembers handler");
    JSONObject req = new JSONObject(ctx.body());
    JSONObject res = new JSONObject();

    String searchValue = req.getString("name").trim();
    String orgName = ctx.sessionAttribute("orgName");
    UserType privilegeLevel = UserType.userTypeFromString(req.getString("role"));
    String listType = req.getString("listType").toUpperCase();

    GetMembersService getMembersService = new GetMembersService(userDao, searchValue, orgName, privilegeLevel, listType);
    Message message = getMembersService.executeAndGetResponse();
    if (message == UserMessage.SUCCESS) {
      res.put("people", getMembersService.getPeoplePage());
      res.put("numPeople", getMembersService.getNumReturnedElements());
      ctx.result(mergeJSON(res, message.toJSON()).toString());
    } else {
      ctx.result(message.toResponseString());
    }
  };

  public Handler getAllMembersByRole = ctx -> {
    log.info("Started getAllMembersByRoles handler");
    JSONObject req = new JSONObject(ctx.body());
    JSONObject res = new JSONObject();
    String orgName = ctx.sessionAttribute("orgName");
    UserType privilegeLevel = UserType.userTypeFromString(req.getString("role"));
    GetAllMembersByRoleService getAllMembersByRoleService = new GetAllMembersByRoleService(userDao, orgName, privilegeLevel);
    Message message = getAllMembersByRoleService.executeAndGetResponse();
    if (message == UserMessage.SUCCESS) {
      res.put("people", getAllMembersByRoleService.getUsersWithSpecificRole());
      res.put("numPeople", getAllMembersByRoleService.getUsersWithSpecificRole().size());
      ctx.result(mergeJSON(res, message.toJSON()).toString());
    } else {
      ctx.result(message.toResponseString());
    }
  };

  public Handler getLogInHistory = ctx -> {
    log.info("Started getLogInHistory handler");
    String username = ctx.sessionAttribute("username");
    LoginHistoryService loginHistoryService = new LoginHistoryService(userDao, username);
    Message responseMessage = loginHistoryService.executeAndGetResponse();
    JSONObject res = responseMessage.toJSON();
    if (responseMessage == UserMessage.SUCCESS) {
      res.put("username", loginHistoryService.getUsername());
      res.put("history", loginHistoryService.getLoginHistoryArray());
    }
    ctx.result(res.toString());
  };

  public static JSONObject mergeJSON(JSONObject object1, JSONObject object2) {
    JSONObject merged = new JSONObject(object1, JSONObject.getNames(object1));
    for (String key : JSONObject.getNames(object2)) {
      merged.put(key, object2.get(key));
    }
    return merged;
  }

  public Handler uploadPfp = ctx -> {
    String username = ctx.formParam("username");
    String fileName = ctx.formParam("fileName");
    UploadedFile file = ctx.uploadedFile("file");
    Optional<User> optionalUser = userDao.get(username);
    if (optionalUser.isEmpty()) {
      ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
      return;
    }
    User user = optionalUser.get();
    Date uploadDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    File fileToUpload = new File(
        username, uploadDate, file.getContent(), FileType.PROFILE_PICTURE,
        IdCategoryType.NONE, file.getFilename(), user.getOrganization(), false, file.getContentType());
    if (user.getOrganizationId() != null) {
      fileToUpload.setOrganizationId(user.getOrganizationId());
    }
    UploadFileService service = new UploadFileService(
        fileDao, activityDao, username, fileToUpload,
        Optional.empty(), Optional.empty(), false, Optional.empty(), Optional.empty());
    JSONObject res = service.executeAndGetResponse().toJSON();
    ctx.result(res.toString());
  };

  public Handler loadPfp = ctx -> {
    JSONObject req = new JSONObject(ctx.body());
    String username = req.getString("username");
    DownloadFileService serv = new DownloadFileService(
        fileDao, activityDao, username, username,
        Optional.empty(), Optional.empty(), FileType.PROFILE_PICTURE,
        Optional.empty(), Optional.empty(), Optional.empty(), formDao);
    Message mes = serv.executeAndGetResponse();
    JSONObject responseJSON = mes.toJSON();
    if (mes == FileMessage.SUCCESS) {
      ctx.header("Content-Type", "image/" + serv.getContentType());
      ctx.result(serv.getInputStream());
    } else {
      ctx.result(responseJSON.toString());
    }
  };

  public Handler setDefaultIds = ctx -> {
    JSONObject req = new JSONObject(ctx.body());
    String username = ctx.sessionAttribute("username");
    String id = req.getString("id");
    String docTypeString = req.getString("documentType");
    DocumentType documentType = DocumentType.documentTypeFromString(docTypeString);

    SetUserDefaultIdService setUserDefaultIdService = new SetUserDefaultIdService(userDao, username, documentType, id);
    Message response = setUserDefaultIdService.executeAndGetResponse();

    if (response == UserMessage.SUCCESS) {
      JSONObject responseJSON = new JSONObject();
      responseJSON.put("Message", "DefaultId for " + DocumentType.stringFromDocumentType(documentType) + " has successfully been set");
      responseJSON.put("fileId", setUserDefaultIdService.getDocumentTypeId(documentType));
      JSONObject mergedInfo = mergeJSON(response.toJSON(), responseJSON);
      ctx.result(mergedInfo.toString());
    }
  };

  public Handler getDefaultIds = ctx -> {
    JSONObject req = new JSONObject(ctx.body());
    String username = ctx.sessionAttribute("username");
    String docTypeString = req.getString("documentType");
    DocumentType documentType = DocumentType.documentTypeFromString(docTypeString);

    GetUserDefaultIdService getUserDefaultIdService = new GetUserDefaultIdService(userDao, username, documentType);
    Message response = getUserDefaultIdService.executeAndGetResponse();

    if (response == UserMessage.SUCCESS) {
      String fileId = getUserDefaultIdService.getId(documentType);
      JSONObject responseJSON = new JSONObject();
      responseJSON.put("Message", "DefaultId for " + DocumentType.stringFromDocumentType(documentType) + " has successfully been retrieved");
      responseJSON.put("fileId", fileId);
      responseJSON.put("documentType", DocumentType.stringFromDocumentType(documentType));
      JSONObject mergedInfo = mergeJSON(response.toJSON(), responseJSON);
      ctx.result(mergedInfo.toString());
    } else {
      ctx.result(response.toResponseString());
    }
  };

  public Handler assignWorkerToUser = ctx -> {
    log.info("Started assignWorkerToUser handler");
    JSONObject req = new JSONObject(ctx.body());
    String currentlyLoggedInUsername = ctx.sessionAttribute("username");
    String targetUser = req.getString("user");

    Gson gson = new Gson();
    List<String> workerUsernamesToAdd = gson.fromJson(
        req.get("workerUsernamesToAdd").toString(),
        new TypeToken<ArrayList<String>>() {}.getType());
    AssignWorkerToUserService getMembersService = new AssignWorkerToUserService(
        userDao, currentlyLoggedInUsername, targetUser, workerUsernamesToAdd);
    Message message = getMembersService.executeAndGetResponse();
    ctx.result(message.toResponseString());
  };

  public Handler getOnboardingChecklist = ctx -> {
    log.info("Started getOnboardingChecklist handler");
    String username = ctx.sessionAttribute("username");
    String originUri = ctx.header("Origin");
    GetOnboardingChecklistService getOnboardingChecklistService = new GetOnboardingChecklistService(
        userDao, formDao, fileDao, username, originUri);
    Message message = getOnboardingChecklistService.executeAndGetResponse();
    if (message == UserMessage.AUTH_SUCCESS) {
      ctx.status(200).json(getOnboardingChecklistService.getOnboardingChecklistResponse());
      return;
    }
    ctx.status(401).json(Map.of("error", "Unauthorized"));
  };

  public Handler postOnboardingStatus = ctx -> {
    log.info("Started postOnboardingStatus handler");
    OnboardingStatus newOnboardingStatus = ctx.bodyAsClass(OnboardingStatus.class);
    String username = ctx.sessionAttribute("username");
    PostOnboardingChecklistService postOnboardingChecklistService = new PostOnboardingChecklistService(userDao, username, newOnboardingStatus);
    Message message = postOnboardingChecklistService.executeAndGetResponse();
    ctx.result(message.toResponseString());
  };

  private Message checkProfileAuthorization(io.javalin.http.Context ctx, String targetUsername) {
    String sessionUsername = ctx.sessionAttribute("username");
    String sessionOrgName = ctx.sessionAttribute("orgName");
    UserType sessionUserType = ctx.sessionAttribute("privilegeLevel");

    if (sessionUsername == null || sessionUsername.isEmpty()) {
      return AUTH_FAILURE;
    }
    Optional<User> sessionUserOpt = userDao.get(sessionUsername);
    if (sessionUserOpt.isEmpty()) {
      return AUTH_FAILURE;
    }
    if (targetUsername == null || targetUsername.isEmpty() || targetUsername.equals(sessionUsername)) {
      return null;
    }
    if (sessionUserType != Worker && sessionUserType != Admin && sessionUserType != Director) {
      return INSUFFICIENT_PRIVILEGE;
    }

    Optional<User> targetUserOpt = userDao.get(targetUsername);
    if (targetUserOpt.isEmpty()) {
      return USER_NOT_FOUND;
    }
    if (!targetUserOpt.get().getOrganization().equals(sessionOrgName)) {
      return CROSS_ORG_ACTION_DENIED;
    }
    return null;
  }

  /** Clients may not change birth date via profile update (workers handle corrections). */
  private Message checkClientCannotChangeBirthDate(UserType sessionUserType, JSONObject updateRequest) {
    if (!updateRequest.has("birthDate")) {
      return null;
    }
    if (sessionUserType == Client) {
      return INSUFFICIENT_PRIVILEGE;
    }
    return null;
  }

  /**
   * When a staff member updates another user's profile, changing legal name or birth date for a
   * {@link UserType#Client} is limited to {@link UserType#Worker}, {@link UserType#Admin}, and
   * {@link UserType#Director}.
   */
  private Message checkStaffClientIdentityEdits(
      io.javalin.http.Context ctx, String targetUsername, JSONObject updateRequest) {
    String sessionUsername = ctx.sessionAttribute("username");
    UserType sessionUserType = ctx.sessionAttribute("privilegeLevel");

    if (targetUsername == null
        || targetUsername.isEmpty()
        || targetUsername.equals(sessionUsername)) {
      return null;
    }
    if (!updateRequest.has("currentName") && !updateRequest.has("birthDate")) {
      return null;
    }
    Optional<User> targetOpt = userDao.get(targetUsername);
    if (targetOpt.isEmpty()) {
      return USER_NOT_FOUND;
    }
    if (targetOpt.get().getUserType() != Client) {
      return null;
    }
    if (sessionUserType != Worker
        && sessionUserType != Admin
        && sessionUserType != Director) {
      return INSUFFICIENT_PRIVILEGE;
    }
    return null;
  }

  public Handler updateUserProfile = ctx -> {
    log.info("Started updateUserProfile handler");
    JSONObject req = new JSONObject(ctx.body());

    String targetUsername = req.optString("username", null);
    if (targetUsername != null && targetUsername.isEmpty()) targetUsername = null;

    Message authCheck = checkProfileAuthorization(ctx, targetUsername);
    if (authCheck != null) {
      ctx.result(authCheck.toJSON().toString());
      return;
    }

    JSONObject updateRequest = new JSONObject(req.toString());
    updateRequest.remove("username");

    UserType sessionUserType = ctx.sessionAttribute("privilegeLevel");
    Message clientBirthAuth = checkClientCannotChangeBirthDate(sessionUserType, updateRequest);
    if (clientBirthAuth != null) {
      ctx.result(clientBirthAuth.toJSON().toString());
      return;
    }

    Message identityAuth = checkStaffClientIdentityEdits(ctx, targetUsername, updateRequest);
    if (identityAuth != null) {
      ctx.result(identityAuth.toJSON().toString());
      return;
    }

    String username = targetUsername != null ? targetUsername : ctx.sessionAttribute("username");

    UpdateUserProfileService updateService =
        new UpdateUserProfileService(userDao, username, updateRequest, emailSender);
    Message response = updateService.executeAndGetResponse();
    ctx.result(response.toJSON().toString());
  };

  public Handler updateProfileFromDirectives = ctx -> {
    log.info("Started updateProfileFromDirectives handler");
    JSONObject req = new JSONObject(ctx.body());

    String targetUsername = req.optString("username", null);
    if (targetUsername != null && targetUsername.isEmpty()) targetUsername = null;

    Message authCheck = checkProfileAuthorization(ctx, targetUsername);
    if (authCheck != null) {
      ctx.result(authCheck.toJSON().toString());
      return;
    }

    JSONObject directivesMap = req.getJSONObject("directives");

    String username = targetUsername != null ? targetUsername : ctx.sessionAttribute("username");

    // Re-map format to dummy keys containing directives so UpdateProfileFromFormService accepts it seamlessly
    JSONObject formAnswers = new JSONObject();
    for (String directive : directivesMap.keySet()) {
      formAnswers.put("dummy:" + directive, directivesMap.get(directive));
    }

    UpdateProfileFromFormService updateService =
        new UpdateProfileFromFormService(userDao, username, formAnswers);
    Message response = updateService.executeAndGetResponse();
    ctx.result(response.toJSON().toString());
  };

  public Handler saveWorkerNotes = ctx -> {
    JSONObject req = new JSONObject(ctx.body());
    String targetUsername = req.optString("username", null);

    UserType sessionUserType = ctx.sessionAttribute("privilegeLevel");
    if (sessionUserType != Worker && sessionUserType != Admin && sessionUserType != Director) {
      ctx.result(INSUFFICIENT_PRIVILEGE.toResponseString());
      return;
    }

    Message authCheck = checkProfileAuthorization(ctx, targetUsername);
    if (authCheck != null) {
      ctx.result(authCheck.toResponseString());
      return;
    }

    String notes = req.optString("workerNotes", "");
    userDao.updateField(targetUsername, "workerNotes", notes);
    ctx.result(SUCCESS.toResponseString());
  };

  public Handler sendEmailLoginInstructions = ctx -> {
    JSONObject req = new JSONObject(ctx.body());
    String targetUsername = req.optString("username", null);
    if (targetUsername != null && targetUsername.isEmpty()) targetUsername = null;

    Message authCheck = checkProfileAuthorization(ctx, targetUsername);
    if (authCheck != null) {
      ctx.result(authCheck.toJSON().toString());
      return;
    }

    String username = targetUsername != null ? targetUsername : ctx.sessionAttribute("username");
    SendEmailLoginInstructionsService sendService =
        new SendEmailLoginInstructionsService(userDao, username, emailSender);
    Message response = sendService.executeAndGetResponse();
    ctx.result(response.toJSON().toString());
  };

  public Handler deleteProfileField = ctx -> {
    log.info("Started deleteProfileField handler");
    JSONObject req = new JSONObject(ctx.body());

    String targetUsername = req.optString("username", null);
    if (targetUsername != null && targetUsername.isEmpty()) targetUsername = null;

    String fieldPath = req.getString("fieldPath");

    Message authCheck = checkProfileAuthorization(ctx, targetUsername);
    if (authCheck != null) {
      ctx.result(authCheck.toJSON().toString());
      return;
    }

    String username = targetUsername != null ? targetUsername : ctx.sessionAttribute("username");
    DeleteUserProfileFieldService deleteService = new DeleteUserProfileFieldService(userDao, username, fieldPath);
    Message response = deleteService.executeAndGetResponse();
    ctx.result(response.toJSON().toString());
  };

  private static org.json.JSONArray phoneBookToJsonArray(java.util.List<PhoneBookEntry> entries) {
    org.json.JSONArray arr = new org.json.JSONArray();
    for (PhoneBookEntry entry : entries) {
      JSONObject e = new JSONObject();
      e.put("label", entry.getLabel());
      e.put("phoneNumber", entry.getPhoneNumber());
      arr.put(e);
    }
    return arr;
  }

  public Handler getPhoneBook = ctx -> {
    log.info("Started getPhoneBook handler");
    JSONObject req = new JSONObject(ctx.body().isEmpty() ? "{}" : ctx.body());
    String targetUsername = req.optString("username", null);
    if (targetUsername != null && targetUsername.isEmpty()) targetUsername = null;

    Message authCheck = checkProfileAuthorization(ctx, targetUsername);
    if (authCheck != null) {
      ctx.result(authCheck.toJSON().toString());
      return;
    }

    String username = targetUsername != null ? targetUsername : ctx.sessionAttribute("username");
    PhoneBookService service = PhoneBookService.get(userDao, username);
    Message response = service.executeAndGetResponse();
    if (response == UserMessage.SUCCESS) {
      JSONObject res = response.toJSON();
      res.put("phoneBook", phoneBookToJsonArray(service.getResultPhoneBook()));
      ctx.result(res.toString());
    } else {
      ctx.result(response.toJSON().toString());
    }
  };

  public Handler addPhoneBookEntry = ctx -> {
    log.info("Started addPhoneBookEntry handler");
    JSONObject req = new JSONObject(ctx.body());
    String targetUsername = req.optString("username", null);
    if (targetUsername != null && targetUsername.isEmpty()) targetUsername = null;

    Message authCheck = checkProfileAuthorization(ctx, targetUsername);
    if (authCheck != null) {
      ctx.result(authCheck.toJSON().toString());
      return;
    }

    String username = targetUsername != null ? targetUsername : ctx.sessionAttribute("username");
    String label = req.getString("label");
    String phoneNumber = req.getString("phoneNumber");

    PhoneBookService service = PhoneBookService.add(userDao, username, label, phoneNumber);
    Message response = service.executeAndGetResponse();
    if (response == UserMessage.SUCCESS) {
      JSONObject res = response.toJSON();
      res.put("phoneBook", phoneBookToJsonArray(service.getResultPhoneBook()));
      ctx.result(res.toString());
    } else {
      ctx.result(response.toJSON().toString());
    }
  };

  public Handler updatePhoneBookEntry = ctx -> {
    log.info("Started updatePhoneBookEntry handler");
    JSONObject req = new JSONObject(ctx.body());
    String targetUsername = req.optString("username", null);
    if (targetUsername != null && targetUsername.isEmpty()) targetUsername = null;

    Message authCheck = checkProfileAuthorization(ctx, targetUsername);
    if (authCheck != null) {
      ctx.result(authCheck.toJSON().toString());
      return;
    }

    String username = targetUsername != null ? targetUsername : ctx.sessionAttribute("username");
    String phoneNumber = req.getString("phoneNumber");
    String newLabel = req.optString("newLabel", null);
    String newPhoneNumber = req.optString("newPhoneNumber", null);

    PhoneBookService service = PhoneBookService.update(userDao, username, phoneNumber, newLabel, newPhoneNumber);
    Message response = service.executeAndGetResponse();
    if (response == UserMessage.SUCCESS) {
      JSONObject res = response.toJSON();
      res.put("phoneBook", phoneBookToJsonArray(service.getResultPhoneBook()));
      ctx.result(res.toString());
    } else {
      ctx.result(response.toJSON().toString());
    }
  };

  public Handler deletePhoneBookEntry = ctx -> {
    log.info("Started deletePhoneBookEntry handler");
    JSONObject req = new JSONObject(ctx.body());
    String targetUsername = req.optString("username", null);
    if (targetUsername != null && targetUsername.isEmpty()) targetUsername = null;

    Message authCheck = checkProfileAuthorization(ctx, targetUsername);
    if (authCheck != null) {
      ctx.result(authCheck.toJSON().toString());
      return;
    }

    String username = targetUsername != null ? targetUsername : ctx.sessionAttribute("username");
    String phoneNumber = req.getString("phoneNumber");

    PhoneBookService service = PhoneBookService.delete(userDao, username, phoneNumber);
    Message response = service.executeAndGetResponse();
    if (response == UserMessage.SUCCESS) {
      JSONObject res = response.toJSON();
      res.put("phoneBook", phoneBookToJsonArray(service.getResultPhoneBook()));
      ctx.result(res.toString());
    } else {
      ctx.result(response.toJSON().toString());
    }
  };

  public Handler removeOrganizationMember = ctx -> {
    log.info("Starting removeOrganizationMember handler");
    JSONObject req = new JSONObject(ctx.body());
    String sessionUsername = ctx.sessionAttribute("username");
    String sessionOrgName = ctx.sessionAttribute("orgName");
    UserType sessionUserType = ctx.sessionAttribute("privilegeLevel");
    String targetUsername = req.getString("username").strip();

    RemoveOrganizationMemberService removeService = new RemoveOrganizationMemberService(
        db, userDao, fileDao, formDao, activityDao, notificationDao,
        sessionUsername, targetUsername, sessionOrgName, sessionUserType);
    Message response = removeService.executeAndGetResponse();
    ctx.result(response.toJSON().toString());
  };
}
