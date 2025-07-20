package Security.Services;

import Activity.UserActivity.AuthenticationActivity.ChangePasswordActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.User.UserDao;
import Security.SecurityUtils;
import User.User;
import User.UserMessage;
import Validation.ValidationUtils;
import java.util.Objects;
import java.util.Optional;

public class ChangePasswordService implements Service {
  UserDao userDao;
  String username;
  ActivityDao activityDao;
  String oldPassword;
  String newPassword;

  public ChangePasswordService(
      UserDao userDao,
      String username,
      ActivityDao activityDao,
      String oldPassword,
      String newPassword) {
    this.userDao = userDao;
    this.username = username;
    this.activityDao = activityDao;
    this.oldPassword = oldPassword;
    this.newPassword = newPassword;
  }

  @Override
  public Message executeAndGetResponse() {
    Objects.requireNonNull(userDao);
    Objects.requireNonNull(username);
    Objects.requireNonNull(newPassword);
    Objects.requireNonNull(oldPassword);
    if (!ValidationUtils.isValidUsername(username)
        || !ValidationUtils.isValidPassword(newPassword)) {
      return UserMessage.INVALID_PARAMETER;
    }
    return changePassword(userDao, username, activityDao, oldPassword, newPassword);
  }

  public static Message changePassword(
      UserDao userDao,
      String username,
      ActivityDao activityDao,
      String oldPassword,
      String newPassword) {
    Optional<User> userResult = userDao.get(username);
    if (userResult.isEmpty()) {
      return UserMessage.USER_NOT_FOUND;
    } else if (oldPassword.equals(newPassword)) {
      return UserMessage.PASSWORD_UNCHANGED;
    }
    User user = userResult.get();
    String hash = user.getPassword();
    SecurityUtils.PassHashEnum hashStatus = SecurityUtils.verifyPassword(oldPassword, hash);
    if (hashStatus == SecurityUtils.PassHashEnum.ERROR) {
      return UserMessage.SERVER_ERROR;
    } else if (hashStatus == SecurityUtils.PassHashEnum.FAILURE) {
      return UserMessage.AUTH_FAILURE;
    } else {
      String newPasswordHash = SecurityUtils.hashPassword(newPassword);
      userDao.resetPassword(user, newPasswordHash);
    }
    recordChangePasswordActivity(username, activityDao);
    return UserMessage.AUTH_SUCCESS;
  }

  private static void recordChangePasswordActivity(String username, ActivityDao activityDao) {
    ChangePasswordActivity a = new ChangePasswordActivity(username);
    activityDao.save(a);
  }
}
