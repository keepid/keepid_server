package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import Security.EmailExceptions;
import Security.EmailUtil;
import User.User;
import User.UserMessage;
import Validation.ValidationUtils;

import java.util.Optional;

public class SendEmailLoginInstructionsService implements Service {
  private final UserDao userDao;
  private final String username;

  public SendEmailLoginInstructionsService(UserDao userDao, String username) {
    this.userDao = userDao;
    this.username = username;
  }

  @Override
  public Message executeAndGetResponse() {
    Optional<User> userResult = userDao.get(username);
    if (userResult.isEmpty()) {
      return UserMessage.USER_NOT_FOUND;
    }

    User user = userResult.get();
    String email = user.getEmail();
    if (!ValidationUtils.hasValue(email)) {
      return UserMessage.EMAIL_DOES_NOT_EXIST;
    }

    String normalizedEmail = email.trim().toLowerCase();
    try {
      String message = EmailUtil.getLoginInstructionsEmail();
      EmailUtil.sendEmail("Keep Id", normalizedEmail, "Keep.id login instructions", message);
      return UserMessage.SUCCESS;
    } catch (EmailExceptions e) {
      return e;
    }
  }
}
