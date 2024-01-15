package UserV2.Services;

import Config.Message;
import Config.Service;
import Database.UserV2.UserDao;
import UserV2.User;
import UserV2.UserMessage;
import UserV2.UserType;
import Validation.ValidationUtils;

import java.util.Objects;
import java.util.Optional;

public class AuthenticateUserService implements Service {
  UserDao userDao;
  private String username;
  private String sessionUsername;
  private User user;

  public AuthenticateUserService(UserDao userDao, String sessionUsername) {
    this.userDao = userDao;
    this.sessionUsername = sessionUsername;
  }

  @Override
  public Message executeAndGetResponse() {
    if (!ValidationUtils.isValidUsername(sessionUsername)) {
      return UserMessage.AUTH_FAILURE;
    }
    if (sessionUsername == null) {
      return UserMessage.AUTH_FAILURE;
    } else {
      Optional<User> maybeUser = userDao.get(sessionUsername);
      if (maybeUser.isEmpty()) {
        return UserMessage.AUTH_FAILURE;
      }
      this.user = maybeUser.get();
      this.username = this.user.getUsername();
      return UserMessage.AUTH_SUCCESS;
    }
  }

  public UserType getUserRole() {
    Objects.requireNonNull(user);
    return user.getUserType();
  }

  public String getOrganization() {
    Objects.requireNonNull(user);
    return user.getOrganization();
  }

  public String getUsername() {
    Objects.requireNonNull(user);
    return user.getUsername();
  }

  public String getFirstName() {
    Objects.requireNonNull(user);
    return user.getFirstName();
  }

  public String getLastName() {
    Objects.requireNonNull(user);
    return user.getLastName();
  }

  public String getFullName() {
    Objects.requireNonNull(user);
    return user.getFirstName() + " " + user.getLastName();
  }

  public boolean isTwoFactorOn() {
    Objects.requireNonNull(user);
    return user.getTwoFactorOn();
  }
}
