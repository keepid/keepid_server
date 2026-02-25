package User;

import Config.Message;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.Organization.OrgDao;
import Database.Token.TokenDao;
import Database.User.UserDao;
import Organization.Organization;
import File.File;
import File.FileMessage;
import File.FileType;
import File.IdCategoryType;
import File.Services.DownloadFileService;
import File.Services.UploadFileService;
import Security.EmailExceptions;
import Security.EmailSender;
import Security.EmailSenderFactory;
import Security.EmailUtil;
import User.Onboarding.OnboardingStatus;
import User.Services.*;
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
  EmailSender emailSender;

  public UserController(
      UserDao userDao,
      TokenDao tokenDao,
      FileDao fileDao,
      ActivityDao activityDao,
      FormDao formDao,
      OrgDao orgDao,
      MongoDatabase db) {
    this(userDao, tokenDao, fileDao, activityDao, formDao, orgDao, db, EmailSenderFactory.smtp());
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
    this.userDao = userDao;
    this.tokenDao = tokenDao;
    this.fileDao = fileDao;
    this.activityDao = activityDao;
    this.formDao = formDao;
    this.orgDao = orgDao;
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
          userDao, activityDao, UserType.Admin, orgName, creatorUsername,
          clientName, defaultBirthdate, clientEmail, clientPhoneNumber,
          clientAddress, false, clientUsername, clientPassword, UserType.Client);
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

  public Handler getSessionUser = ctx -> {
    JSONObject responseJSON = new JSONObject();
    String org = ctx.sessionAttribute("orgName");
    String username = ctx.sessionAttribute("username");
    String fullName = ctx.sessionAttribute("fullName");
    UserType role = ctx.sessionAttribute("privilegeLevel");
    String googleLoginError = ctx.sessionAttribute("googleLoginError");

    responseJSON.put("organization", org == null ? "" : org);
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
        userDao, activityDao, sessionUserLevel, organizationName, sessionUsername,
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
    String lastName = req.getString("lastname").strip();
    String birthDate = req.getString("birthDate").strip();
    String email = req.getString("email").toLowerCase().strip();
    String phone = req.optString("phonenumber", "").strip();

    String dobCompact = birthDate.replace("-", "");
    String randomSuffix = UUID.randomUUID().toString().substring(0, 4);
    String username = (firstName + "-" + lastName + "-" + dobCompact + "-" + randomSuffix).toLowerCase();
    String password = UUID.randomUUID().toString();

    Name currentName = new Name(firstName, lastName);

    CreateUserService createUserService = new CreateUserService(
        userDao, activityDao, sessionUserLevel, organizationName, sessionUsername,
        currentName, birthDate, email, phone, null,
        false, username, password, UserType.Client);
    Message createResponse = createUserService.executeAndGetResponse();

    if (createResponse != ENROLL_SUCCESS) {
      ctx.result(createResponse.toJSON().toString());
      return;
    }

    try {
      String welcomeEmail = EmailUtil.getEnrollmentWelcomeEmail(firstName);
      emailSender.sendEmail("Keep Id", email, "Welcome to Keep.id", welcomeEmail);
    } catch (EmailExceptions e) {
      log.warn("User enrolled but welcome email failed to send to {}: {}", email, e.getMessage());
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
    UserType userType = UserType.userTypeFromString(req.getString("personRole").strip());
    String organizationName = req.getString("orgName").strip();

    Name currentName = new Name(firstName, lastName);
    Address personalAddress = new Address(addressLine1, city, state, zipcode);

    CreateUserService createUserService = new CreateUserService(
        userDao, activityDao, UserType.Director, organizationName, null,
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
        Optional.empty(), Optional.empty(), formDao);
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

    String username = targetUsername != null ? targetUsername : ctx.sessionAttribute("username");

    JSONObject updateRequest = new JSONObject(req.toString());
    updateRequest.remove("username");

    UpdateUserProfileService updateService =
        new UpdateUserProfileService(userDao, username, updateRequest, emailSender);
    Message response = updateService.executeAndGetResponse();
    ctx.result(response.toJSON().toString());
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
        db, userDao, sessionUsername, targetUsername, sessionOrgName, sessionUserType);
    Message response = removeService.executeAndGetResponse();
    ctx.result(response.toJSON().toString());
  };
}
