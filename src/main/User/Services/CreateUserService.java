package User.Services;

import Activity.CreateUserActivity.CreateAdminActivity;
import Activity.CreateUserActivity.CreateClientActivity;
import Activity.CreateUserActivity.CreateDirectorActivity;
import Activity.CreateUserActivity.CreateWorkerActivity;

import static User.UserController.newUserActualURL;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.User.UserDao;
import Security.SecurityUtils;
import User.Address;
import User.IpObject;
import User.Name;
import User.User;
import User.UserMessage;
import User.UserType;
import Validation.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

@Slf4j
public class CreateUserService implements Service {
  UserDao userDao;
  ActivityDao activityDao;
  UserType sessionUserLevel;
  String organizationName;
  String sessionUsername;
  Name currentName;
  String birthDate;
  String email;
  String phone;
  Address personalAddress;
  Boolean twoFactorOn;
  String username;
  String password;
  UserType userType;

  public CreateUserService(
      UserDao userDao,
      ActivityDao activityDao,
      UserType sessionUserLevel,
      String organizationName,
      String sessionUsername,
      Name currentName,
      String birthDate,
      String email,
      String phone,
      Address personalAddress,
      Boolean twoFactorOn,
      String username,
      String password,
      UserType userType) {
    this.userDao = userDao;
    this.activityDao = activityDao;
    this.sessionUserLevel = sessionUserLevel;
    this.organizationName = organizationName;
    this.sessionUsername = sessionUsername;
    this.currentName = currentName;
    this.birthDate = birthDate;
    this.email = email;
    this.phone = phone;
    this.personalAddress = personalAddress;
    this.twoFactorOn = twoFactorOn;
    this.username = username;
    this.password = password;
    this.userType = userType;
  }

  @Override
  public Message executeAndGetResponse() {
    String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    this.email = normalizedEmail;

    boolean requiresOrganization = userType != UserType.Developer;
    if (requiresOrganization && organizationName == null) {
      log.info("Token failure");
      return UserMessage.SESSION_TOKEN_FAILURE;
    }
    if (userType == null) {
      log.info("Invalid privilege type");
      return UserMessage.INVALID_PRIVILEGE_TYPE;
    }

    User user;
    try {
      user =
          new User(
              currentName,
              birthDate,
              email,
              phone,
              organizationName,
              personalAddress,
              twoFactorOn,
              username,
              password,
              userType);
    } catch (ValidationException ve) {
      log.error("Validation exception");
      return ve;
    }

    if ((user.getUserType() == UserType.Director
            || user.getUserType() == UserType.Admin
            || user.getUserType() == UserType.Worker)
        && sessionUserLevel != UserType.Admin
        && sessionUserLevel != UserType.Director) {
      log.error("Cannot enroll ADMIN/DIRECTOR as NON-ADMIN/NON-DIRECTOR");
      return UserMessage.NONADMIN_ENROLL_ADMIN;
    }

    if (user.getUserType() == UserType.Client && sessionUserLevel == UserType.Client) {
      log.error("Cannot enroll CLIENT as CLIENT");
      return UserMessage.CLIENT_ENROLL_CLIENT;
    }

    Optional<User> optionalUser = userDao.get(username);
    if (optionalUser.isPresent()) {
      log.info("Username already exists");
      return UserMessage.USERNAME_ALREADY_EXISTS;
    }
    if (!user.getEmail().isEmpty()) {
      Optional<User> optionalByEmail = userDao.getByEmail(user.getEmail());
      if (optionalByEmail.isPresent()) {
        log.info("Email already exists");
        return UserMessage.EMAIL_ALREADY_EXISTS;
      }
    }

    String hash = SecurityUtils.hashPassword(password);
    if (hash == null) {
      log.error("Could not hash password");
      return UserMessage.HASH_FAILURE;
    }
    user.setPassword(hash);

    List<IpObject> logInInfo = new ArrayList<IpObject>(1000);
    user.setLogInHistory(logInInfo);

    userDao.save(user);
    if (sessionUsername == null) {
      sessionUsername = username;
    }

    switch (user.getUserType()) {
      case Worker:
        CreateWorkerActivity act = new CreateWorkerActivity(sessionUsername, user.getUsername());
        activityDao.save(act);
        break;
      case Director:
        CreateDirectorActivity dir =
            new CreateDirectorActivity(sessionUsername, user.getUsername());
        activityDao.save(dir);
        break;
      case Admin:
        CreateAdminActivity adm = new CreateAdminActivity(sessionUsername, user.getUsername());
        activityDao.save(adm);
        break;
      case Client:
        CreateClientActivity cli = new CreateClientActivity(sessionUsername, user.getUsername());
        activityDao.save(cli);
        break;
      case Developer:
        // Developer account creation does not currently emit a dedicated activity event.
        break;
    }
    log.info("Successfully created user, {}", user.getUsername());
    generateCreateUserSlackMessage();
    return UserMessage.ENROLL_SUCCESS;
  }

  private void generateCreateUserSlackMessage() {
    JSONArray blocks = new JSONArray();
    JSONObject titleJson = new JSONObject();
    JSONObject titleText = new JSONObject();
    titleText.put("text", "*User type: * " + userType);
    titleText.put("type", "mrkdwn");
    titleJson.put("type", "section");
    titleJson.put("text", titleText);
    blocks.put(titleJson);
    JSONObject desJson = new JSONObject();
    JSONObject desText = new JSONObject();
    String description =
        "User "
            + username
            + " has been created for "
            + currentName.getFirst()
            + " "
            + currentName.getLast()
            + (organizationName == null ? "" : (" in " + organizationName));
    desText.put("text", description);
    desText.put("type", "mrkdwn");
    desJson.put("text", desText);
    desJson.put("type", "section");
    blocks.put(desJson);
    JSONObject input = new JSONObject();
    input.put("blocks", blocks);
    log.info("Trying to post the message on Slack");
    Unirest.post(newUserActualURL)
        .header("accept", "application/json")
        .body(input.toString())
        .asEmpty();
  }
}
