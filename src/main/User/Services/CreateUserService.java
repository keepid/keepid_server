package User.Services;

import Activity.CreateAdminActivity;
import Activity.CreateClientActivity;
import Activity.CreateDirectorActivity;
import Activity.CreateWorkerActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.User.UserDao;
import Security.SecurityUtils;
import User.IpObject;
import User.User;
import User.UserMessage;
import User.UserType;
import Validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class CreateUserService implements Service {
  UserDao userDao;
  ActivityDao activityDao;
  UserType sessionUserLevel;
  String organizationName;
  String sessionUsername;
  String firstName;
  String lastName;
  String birthDate;
  String email;
  String phone;
  String address;
  String city;
  String state;
  String zipcode;
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
      String firstName,
      String lastName,
      String birthDate,
      String email,
      String phone,
      String address,
      String city,
      String state,
      String zipcode,
      Boolean twoFactorOn,
      String username,
      String password,
      UserType userType) {
    this.userDao = userDao;
    this.activityDao = activityDao;
    this.sessionUserLevel = sessionUserLevel;
    this.organizationName = organizationName;
    this.sessionUsername = sessionUsername;
    this.firstName = firstName;
    this.lastName = lastName;
    this.birthDate = birthDate;
    this.email = email;
    this.phone = phone;
    this.address = address;
    this.city = city;
    this.state = state;
    this.zipcode = zipcode;
    this.twoFactorOn = twoFactorOn;
    this.username = username;
    this.password = password;
    this.userType = userType;
  }

  @Override
  public Message executeAndGetResponse() {
    // validations
    if (organizationName == null) {
      log.info("Token failure");
      return UserMessage.SESSION_TOKEN_FAILURE;
    }
    if (userType == null) {
      log.info("Invalid privilege type");
      return UserMessage.INVALID_PRIVILEGE_TYPE;
    }
    // create user object
    User user;
    try {
      user =
          new User(
              firstName,
              lastName,
              email,
              organizationName,
              twoFactorOn,
              username,
              password,
              userType);
    } catch (ValidationException ve) {
      log.error("Validation exception");
      return ve;
    }
    // check some conditions
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

    // add to database
    Optional<User> optionalUser = userDao.get(username);
    if (optionalUser.isPresent()) {
      log.info("Username already exists");
      return UserMessage.USERNAME_ALREADY_EXISTS;
    }

    // create password hash
    String hash = SecurityUtils.hashPassword(password);
    if (hash == null) {
      log.error("Could not hash password");
      return UserMessage.HASH_FAILURE;
    }
    user.setPassword(hash);

    // get login info
    List<IpObject> logInInfo = new ArrayList<IpObject>(1000);
    user.setLogInHistory(logInInfo);

    // insert user into database
    userDao.save(user);
    if (sessionUsername == null) {
      sessionUsername = username;
    }

    // create activity
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
    }
    log.info("Successfully created user, " + user.getUsername());
    return UserMessage.ENROLL_SUCCESS;
  }
}
