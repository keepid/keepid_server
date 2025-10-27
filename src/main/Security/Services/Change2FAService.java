package Security.Services;

import Activity.UserActivity.AuthenticationActivity.Change2FAActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import java.util.Objects;
import java.util.Optional;

public class Change2FAService implements Service {
  private UserDao userDao;
  private ActivityDao activityDao;
  private String username;
  private Boolean isTwoFactorOn;

  public Change2FAService(
      UserDao userDao, ActivityDao activityDao, String username, Boolean isTwoFactorOn) {
    this.userDao = userDao;
    this.activityDao = activityDao;
    this.username = username;
    this.isTwoFactorOn = isTwoFactorOn;
  }

  @Override
  public Message executeAndGetResponse() {
    Objects.requireNonNull(userDao);
    Optional<User> userResult = userDao.get(username);
    if (userResult.isEmpty()) {
      return UserMessage.USER_NOT_FOUND;
    }
    User user = userResult.get();
    if (isTwoFactorOn == null) {
      return UserMessage.INVALID_PARAMETER;
    }
    // No current way to validate this boolean
    user.setTwoFactorOn(isTwoFactorOn);
    String oldBoolean = booleanToString(!isTwoFactorOn);
    String newBoolean = booleanToString(isTwoFactorOn);
    userDao.update(user);
    recordChange2FAActivity();
    return UserMessage.SUCCESS;
  }

  private void recordChange2FAActivity() {
    Change2FAActivity a;
    if (isTwoFactorOn) {
      a = new Change2FAActivity(username, "On");
    } else {
      a = new Change2FAActivity(username, "Off");
    }
    activityDao.save(a);
  }

  private String booleanToString(Boolean bool) {
    if (bool) {
      return "True";
    } else {
      return "False";
    }
  }
}
