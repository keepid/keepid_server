package User;

import Config.Message;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.Token.TokenDao;
import Database.User.UserDao;
import File.File;
import File.FileMessage;
import File.FileType;
import File.IdCategoryType;
import File.Services.DownloadFileService;
import File.Services.UploadFileService;
import User.Services.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.MongoDatabase;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import Security.URIUtil;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class UserController {
  MongoDatabase db;
  UserDao userDao;
  TokenDao tokenDao;
  ActivityDao activityDao;
  FileDao fileDao;
  FormDao formDao;

  public UserController(
      UserDao userDao,
      TokenDao tokenDao,
      FileDao fileDao,
      ActivityDao activityDao,
      FormDao formDao,
      MongoDatabase db) {
    this.userDao = userDao;
    this.tokenDao = tokenDao;
    this.fileDao = fileDao;
    this.activityDao = activityDao;
    this.formDao = formDao;
    this.db = db;
  }

  public Handler ingestCsv =
      ctx -> {
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
              entry[0] = values[0].trim(); // First Name
              entry[1] = values[1].trim(); // Last Name
              entry[2] = values[2].trim(); // Email
              entry[3] = values[3].trim(); // Phone Number
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
          String clientUsername =
              clientFirstName.toLowerCase() + "-" + clientLastName.toLowerCase();
          String clientPassword =
              clientFirstName.toLowerCase()
                  + clientLastName.toLowerCase()
                  + clientPhoneNumber.substring(
                      clientPhoneNumber.length() - 4); // firstnamelastnamelast4phone
          System.out.println("First Name: " + clientFirstName);
          System.out.println("Last Name: " + clientLastName);
          System.out.println("Email: " + clientEmail);
          System.out.println("Phone Number: " + clientPhoneNumber);
          System.out.println("---------------------------");

          CreateUserService createUserService =
              new CreateUserService(
                  userDao,
                  activityDao,
                  UserType.Admin,
                  orgName,
                  creatorUsername,
                  clientFirstName,
                  clientLastName,
                  defaultBirthdate,
                  clientEmail,
                  clientPhoneNumber,
                  faceToFaceAddress,
                  faceToFaceCity,
                  faceToFaceState,
                  faceToFaceZip,
                  false,
                  clientUsername,
                  clientPassword,
                  UserType.Client);
          Message response = createUserService.executeAndGetResponse();
          System.out.println(response.toResponseString());
        }
        ctx.result("SUCCESS");
      };

  public Handler loginUser =
      ctx -> {
        Optional.ofNullable(ctx.req().getSession(false)).ifPresent(HttpSession::invalidate);
        JSONObject req = new JSONObject(ctx.body());
        String username = req.getString("username");
        String password = req.getString("password");
        String ip = ctx.ip();
        String userAgent = ctx.userAgent();
        log.info("Attempting to login " + username);

        LoginService loginService =
            new LoginService(userDao, tokenDao, activityDao, username, password, ip, userAgent);
        Message response = loginService.executeAndGetResponse();
        log.info(response.toString() + response.getErrorDescription());
        JSONObject responseJSON = response.toJSON();
        if (response == UserMessage.AUTH_SUCCESS) {
          responseJSON.put("userRole", loginService.getUserRole());
          responseJSON.put("organization", loginService.getOrganization());
          responseJSON.put("firstName", loginService.getFirstName());
          responseJSON.put("lastName", loginService.getLastName());
          responseJSON.put("twoFactorOn", loginService.isTwoFactorOn());

          ctx.sessionAttribute("privilegeLevel", loginService.getUserRole());
          ctx.sessionAttribute("orgName", loginService.getOrganization());
          ctx.sessionAttribute("username", loginService.getUsername());
          ctx.sessionAttribute("fullName", loginService.getFullName());
        } else {
          responseJSON.put("userRole", "");
          responseJSON.put("organization", "");
          responseJSON.put("firstName", "");
          responseJSON.put("lastName", "");
          responseJSON.put("twoFactorOn", "");
        }
        ctx.result(responseJSON.toString());
      };

  /**
   * Initializes the Google OAuth2 Login Workflow.
   *
   * <p>Implements CSRF and PKCE protections.</p>
   *
   * @see <a href="https://developers.google.com/identity/protocols/oauth2/web-server">...</a>
   */
  public Handler googleLoginRequestHandler =
      ctx -> {
        Optional.ofNullable(ctx.req().getSession(false)).ifPresent(HttpSession::invalidate);
        JSONObject req = new JSONObject(ctx.body());
        String redirectUri = req.optString("redirectUri", null);
        String originUri = req.optString("originUri", null);
        log.info("Processing Google login request with redirect URI: {}," +
            "origin URI: {}",
            redirectUri, originUri);

        ProcessGoogleLoginRequestService processGoogleLoginRequestService =
            new ProcessGoogleLoginRequestService(redirectUri, originUri);
        Message response = processGoogleLoginRequestService.executeAndGetResponse();
        JSONObject responseJSON = response.toJSON();
        log.info("Google login request processed with status: {}",  response.getErrorName());

        if (response == GoogleLoginRequestMessage.REQUEST_SUCCESS) {
          log.info("Setting session attributes");
          ctx.sessionAttribute("origin_uri", originUri);
          ctx.sessionAttribute("redirect_uri", redirectUri);
          ctx.sessionAttribute("PKCECodeVerifier",
              processGoogleLoginRequestService.getCodeVerifier());
          ctx.sessionAttribute("state", processGoogleLoginRequestService.getCsrfToken());

          responseJSON.put("codeChallenge", processGoogleLoginRequestService.getCodeChallenge());
          responseJSON.put("state", processGoogleLoginRequestService.getCsrfToken());
          ctx.result(responseJSON.toString());
        }
        ctx.result(responseJSON.toString());
      };

  /**
   * Redirect URI endpoint for Google OAuth2 workflow.
   *
   * @see <a href="https://developers.google.com/identity/protocols/oauth2/web-server">...</a>
   */
  public Handler googleLoginResponseHandler =
      ctx -> {
          String authCode = ctx.queryParam("code");
          String state = ctx.queryParam("state");
          String ip = ctx.ip();
          String userAgent = ctx.userAgent();
          String codeVerifier = ctx.sessionAttribute("PKCECodeVerifier");
          String originUri = ctx.sessionAttribute("origin_uri");
          String redirectUri = ctx.sessionAttribute("redirect_uri");
          String storedCsrfToken = ctx.sessionAttribute("state");

          log.info("Processing Google login response with: authorization code: {}," +
              "state: {}," +
              "retrieved code verifier: {}," +
              "retrieved origin URI: {}," +
              "retrieved redirect URI: {}," +
              "retrieved CSRF token: {}",
              authCode, state, codeVerifier, originUri, redirectUri, storedCsrfToken
          );
          ProcessGoogleLoginResponseService processGoogleLoginResponseService =
          new ProcessGoogleLoginResponseService(
              userDao,
              activityDao,
              state,
              storedCsrfToken,
              authCode,
              codeVerifier,
              originUri,
              redirectUri,
              ip,
              userAgent
          );
        Message response = processGoogleLoginResponseService.executeAndGetResponse();
        log.info("Google login response processed with status: {}", response.getErrorName());

          if (response == GoogleLoginResponseMessage.AUTH_SUCCESS) {
            log.debug("Setting session attributes of privilegeLevel: {}, " +
                    "orgName: {}, " +
                    "username: {}, " +
                    "fullName: {}",
                processGoogleLoginResponseService.getUserRole(),
                processGoogleLoginResponseService.getOrganization(),
                processGoogleLoginResponseService.getUsername(),
                processGoogleLoginResponseService.getFullName());
            ctx.sessionAttribute("privilegeLevel",
                processGoogleLoginResponseService.getUserRole());
            ctx.sessionAttribute("orgName", processGoogleLoginResponseService.getOrganization());
            ctx.sessionAttribute("username", processGoogleLoginResponseService.getUsername());
            ctx.sessionAttribute("fullName", processGoogleLoginResponseService.getFullName());
          }
          ctx.sessionAttribute("PKCECodeVerifier", null);
          ctx.sessionAttribute("origin_uri", null);
          ctx.sessionAttribute("redirect_uri", null);
          ctx.sessionAttribute("state", null);

          // NOTE: query parameters are NOT passed to the frontend
          // for increased privacy and security. Instead, the getSessionUser
          // endpoint will be called to verify that the login was successful
          ctx.redirect(processGoogleLoginResponseService.getOrigin() + "/login");
      };

  /**
   * Endpoint for validating Google logins.
   */
  public Handler getSessionUser =
      ctx -> {
        JSONObject responseJSON = new JSONObject();

        // NOTE: no service needed here because existing session
        // attributes are being retrieved and returned
        String org = ctx.sessionAttribute("orgName");
        String username = ctx.sessionAttribute("username");
        String fullName = ctx.sessionAttribute("fullName");
        UserType role = ctx.sessionAttribute("privilegeLevel");
        log.info("Retrieved session attributes of org: {}, " +
            "username: {}, " +
            "fullName: {}, " +
            "and role: {}",
            org, username, fullName, role
        );

        responseJSON.put("organization", org == null ? "" : org);
        responseJSON.put("username", username == null ? "" : username);
        responseJSON.put("fullName", fullName == null ? "" : fullName);
        responseJSON.put("userRole", role == null ? "" : role);

        log.info("Returning response with session info: {}", responseJSON);
        ctx.result(responseJSON.toString());
      };

  public Handler authenticateUser =
      ctx -> {
        String sessionUsername = ctx.sessionAttribute("username");
        AuthenticateUserService authenticateUserService =
            new AuthenticateUserService(userDao, sessionUsername);
        Message response = authenticateUserService.executeAndGetResponse();
        JSONObject responseJSON = response.toJSON();
        if (response == UserMessage.AUTH_SUCCESS) {
          responseJSON.put("username", authenticateUserService.getUsername());
          responseJSON.put("userRole", authenticateUserService.getUserRole());
          responseJSON.put("organization", authenticateUserService.getOrganization());
          responseJSON.put("firstName", authenticateUserService.getFirstName());
          responseJSON.put("lastName", authenticateUserService.getLastName());
          responseJSON.put("twoFactorOn", authenticateUserService.isTwoFactorOn());
        } else {
          responseJSON.put("username", "");
          responseJSON.put("userRole", "");
          responseJSON.put("organization", "");
          responseJSON.put("firstName", "");
          responseJSON.put("lastName", "");
          responseJSON.put("twoFactorOn", "");
        }
        ctx.result(responseJSON.toString());
      };

  public Handler usernameExists =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String username = req.getString("username");
        CheckUsernameExistsService checkUsernameExistsService =
            new CheckUsernameExistsService(userDao, username);
        ctx.result(checkUsernameExistsService.executeAndGetResponse().toResponseString());
      };

  public Handler createNewUser =
      ctx -> {
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
        String address = req.getString("address").strip();
        String city = req.getString("city").strip();
        String state = req.getString("state").strip();
        String zipcode = req.getString("zipcode").strip();
        Boolean twoFactorOn = req.getBoolean("twoFactorOn");
        String username = req.getString("username").strip();
        String password = req.getString("password").strip();
        String userTypeString = req.getString("personRole").strip();
        UserType userType = UserType.userTypeFromString(userTypeString);

        log.info(sessionUserLevel + " " + organizationName + " " + firstName);

        CreateUserService createUserService =
            new CreateUserService(
                userDao,
                activityDao,
                sessionUserLevel,
                organizationName,
                sessionUsername,
                firstName,
                lastName,
                birthDate,
                email,
                phone,
                address,
                city,
                state,
                zipcode,
                twoFactorOn,
                username,
                password,
                userType);
        Message response = createUserService.executeAndGetResponse();
        ctx.result(response.toJSON().toString());
      };

  public Handler deleteUser =
      ctx -> {
        log.info("Starting deleteUser handler");
        JSONObject req = new JSONObject(ctx.body());
        String sessionUsername = ctx.sessionAttribute("username");
        String password = req.getString("password").strip();
        log.info("Attempting to delete " + sessionUsername);

        DeleteUserService deleteUserService =
            new DeleteUserService(db, userDao, sessionUsername, password);
        Message response = deleteUserService.executeAndGetResponse();
        log.info(response.toString() + response.getErrorDescription());
        ctx.result(response.toJSON().toString());
      };

  public Handler createNewInvitedUser =
      ctx -> {
        log.info("Starting createNewUser handler");
        JSONObject req = new JSONObject(ctx.body());

        String firstName = req.getString("firstname").strip();
        String lastName = req.getString("lastname").strip();
        String birthDate = req.getString("birthDate").strip();
        String email = req.getString("email").strip();
        String phone = req.getString("phonenumber").strip();
        String address = req.getString("address").strip();
        String city = req.getString("city").strip();
        String state = req.getString("state").strip();
        String zipcode = req.getString("zipcode").strip();
        Boolean twoFactorOn = req.getBoolean("twoFactorOn");
        String username = req.getString("username").strip();
        String password = req.getString("password").strip();
        UserType userType = UserType.userTypeFromString(req.getString("personRole").strip());
        String organizationName = req.getString("orgName").strip();

        CreateUserService createUserService =
            new CreateUserService(
                userDao,
                activityDao,
                UserType.Director,
                organizationName,
                null,
                firstName,
                lastName,
                birthDate,
                email,
                phone,
                address,
                city,
                state,
                zipcode,
                twoFactorOn,
                username,
                password,
                userType);
        Message response = createUserService.executeAndGetResponse();
        ctx.result(response.toJSON().toString());
      };

  public Handler logout =
      ctx -> {
        Optional.ofNullable(ctx.req().getSession(false)).ifPresent(HttpSession::invalidate);
        log.info("Signed out");
        ctx.result(UserMessage.SUCCESS.toJSON().toString());
      };

  public Handler getUserInfo =
      ctx -> {
        log.info("Started getUserInfo handler");
        String username;
        try {
          JSONObject req = new JSONObject(ctx.body());
          username = req.getString("username");
        } catch (Exception e) {
          log.info("Username not passed in request, using ctx username");
          username = ctx.sessionAttribute("username");
        }
        GetUserInfoService infoService = new GetUserInfoService(userDao, username);
        Message response = infoService.executeAndGetResponse();
        if (response != UserMessage.SUCCESS) { // if fail return
          ctx.result(response.toJSON().toString());
        } else {
          JSONObject userInfo = infoService.getUserFields(); // get user info here
          JSONObject mergedInfo = mergeJSON(response.toJSON(), userInfo);
          ctx.result(mergedInfo.toString());
        }
      };

  public Handler getMembers =
      ctx -> {
        log.info("Started getMembers handler");
        JSONObject req = new JSONObject(ctx.body());
        JSONObject res = new JSONObject();

        String searchValue = req.getString("name").trim();
        String orgName = ctx.sessionAttribute("orgName");
        UserType privilegeLevel = UserType.userTypeFromString(req.getString("role"));
        String listType = req.getString("listType").toUpperCase();

        GetMembersService getMembersService =
            new GetMembersService(userDao, searchValue, orgName, privilegeLevel, listType);
        Message message = getMembersService.executeAndGetResponse();
        if (message == UserMessage.SUCCESS) {
          res.put("people", getMembersService.getPeoplePage());
          res.put("numPeople", getMembersService.getNumReturnedElements());
          ctx.result(mergeJSON(res, message.toJSON()).toString());
        } else {
          ctx.result(message.toResponseString());
        }
      };

  public Handler getAllMembersByRole =
      ctx -> {
        log.info("Started getAllMembersByRoles handler");
        JSONObject req = new JSONObject(ctx.body());
        JSONObject res = new JSONObject();
        String orgName = ctx.sessionAttribute("orgName");
        UserType privilegeLevel = UserType.userTypeFromString(req.getString("role"));
        GetAllMembersByRoleService getAllMembersByRoleService =
            new GetAllMembersByRoleService(userDao, orgName, privilegeLevel);
        Message message = getAllMembersByRoleService.executeAndGetResponse();
        if (message == UserMessage.SUCCESS) {
          res.put("people", getAllMembersByRoleService.getUsersWithSpecificRole());
          res.put("numPeople", getAllMembersByRoleService.getUsersWithSpecificRole().size());
          ctx.result(mergeJSON(res, message.toJSON()).toString());
        } else {
          ctx.result(message.toResponseString());
        }
      };

  /*
   Returned JSON format:
     {“username”: “username”,
        "history": [
           {
             “date”:”month/day/year, hour:min, Local Time”,
             “device”:”Mobile” or "Computer",
             “IP”:”exampleIP”,
             “location”: “Postal, City”,
           }
      ]
   }
  */
  public Handler getLogInHistory =
      ctx -> {
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

  public static JSONObject mergeJSON(
      JSONObject object1, JSONObject object2) { // helper function to merge 2 json objects
    JSONObject merged = new JSONObject(object1, JSONObject.getNames(object1));
    for (String key : JSONObject.getNames(object2)) {
      merged.put(key, object2.get(key));
    }
    return merged;
  }

  public Handler uploadPfp =
      ctx -> {
        String username = ctx.formParam("username");
        String fileName = ctx.formParam("fileName");
        UploadedFile file = ctx.uploadedFile("file");
        log.info(username + " is attempting to upload a profile picture");
        Optional<User> optionalUser = userDao.get(username);
        if (optionalUser.isEmpty()) {
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
          return;
        }
        User user = optionalUser.get();
        Date uploadDate =
            Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (file == null) {
          ctx.result(UserMessage.INVALID_PARAMETER.toJSON().toString());
          return;
        }
        File fileToUpload =
            new File(
                username,
                uploadDate,
                Objects.requireNonNull(file).content(),
                FileType.PROFILE_PICTURE,
                IdCategoryType.NONE,
                file.filename(),
                user.getOrganization(),
                false,
                file.contentType());
        UploadFileService service =
            new UploadFileService(
                fileDao,
                fileToUpload,
                Optional.empty(),
                Optional.empty(),
                false,
                Optional.empty(),
                Optional.empty());
        JSONObject res = service.executeAndGetResponse().toJSON();
        ctx.result(res.toString());
      };

  public Handler loadPfp =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String username = req.getString("username");
        JSONObject responseJSON;
        DownloadFileService serv =
            new DownloadFileService(
                fileDao,
                username,
                Optional.empty(),
                Optional.empty(),
                FileType.PROFILE_PICTURE,
                Optional.empty(),
                Optional.empty(),
                formDao);
        Message mes = serv.executeAndGetResponse();
        responseJSON = mes.toJSON();
        if (mes == FileMessage.SUCCESS) {
          ctx.header("Content-Type", "image/" + serv.getContentType());
          ctx.result(serv.getInputStream());
        } else ctx.result(responseJSON.toString());
      };

  public Handler setDefaultIds =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String username = ctx.sessionAttribute("username");
        String id = req.getString("id");
        String docTypeString = req.getString("documentType");
        DocumentType documentType = DocumentType.documentTypeFromString(docTypeString);

        // Session attributes contains the following information: {orgName=Stripe testing,
        // privilegeLevel=Admin, fullName=JASON ZHANG, username=stripetest}
        log.info("The username in setDefaultIds is: " + ctx.sessionAttribute("username"));

        SetUserDefaultIdService setUserDefaultIdService =
            new SetUserDefaultIdService(userDao, username, documentType, id);
        Message response = setUserDefaultIdService.executeAndGetResponse();

        if (response == UserMessage.SUCCESS) {
          // Instead of a success message, would be better to return the new ID to be displayed or
          // something similar for get
          JSONObject responseJSON = new JSONObject();
          responseJSON.put(
              "Message",
              "DefaultId for "
                  + DocumentType.stringFromDocumentType(documentType)
                  + " has successfully been set");
          responseJSON.put("fileId", setUserDefaultIdService.getDocumentTypeId(documentType));
          JSONObject mergedInfo = mergeJSON(response.toJSON(), responseJSON);
          ctx.result(mergedInfo.toString());
        }
      };

  public Handler getDefaultIds =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String username = ctx.sessionAttribute("username");
        String docTypeString = req.getString("documentType");
        DocumentType documentType = DocumentType.documentTypeFromString(docTypeString);

        // Session attributes contains the following information: {orgName=Stripe testing,
        // privilegeLevel=Admin, fullName=JASON ZHANG, username=stripetest}
        log.info("The username in setDefaultIds is: " + ctx.sessionAttribute("username"));

        GetUserDefaultIdService getUserDefaultIdService =
            new GetUserDefaultIdService(userDao, username, documentType);
        Message response = getUserDefaultIdService.executeAndGetResponse();

        if (response == UserMessage.SUCCESS) {
          String fileId = getUserDefaultIdService.getId(documentType);
          log.info("fileId retrieved is " + fileId);
          // Instead of a success message, would be better to return the new ID to be displayed or
          // something similar for get
          JSONObject responseJSON = new JSONObject();
          responseJSON.put(
              "Message",
              "DefaultId for "
                  + DocumentType.stringFromDocumentType(documentType)
                  + " has successfully been retrieved");
          responseJSON.put("fileId", fileId);
          responseJSON.put("documentType", DocumentType.stringFromDocumentType(documentType));
          JSONObject mergedInfo = mergeJSON(response.toJSON(), responseJSON);
          ctx.result(mergedInfo.toString());
        } else {
          log.info("Error: {}", response.getErrorName());
          ctx.result(response.toResponseString());
        }
      };

  public Handler assignWorkerToUser =
      ctx -> {
        log.info("Started assignWorkerToUser handler");
        JSONObject req = new JSONObject(ctx.body());
        String currentlyLoggedInUsername = ctx.sessionAttribute("username");
        String targetUser = req.getString("user");

        // convert json list to java list
        Gson gson = new Gson();
        List<String> workerUsernamesToAdd =
            gson.fromJson(
                req.get("workerUsernamesToAdd").toString(),
                new TypeToken<ArrayList<String>>() {}.getType());
        AssignWorkerToUserService getMembersService =
            new AssignWorkerToUserService(
                userDao, currentlyLoggedInUsername, targetUser, workerUsernamesToAdd);
        Message message = getMembersService.executeAndGetResponse();
        ctx.result(message.toResponseString());
      };
}
