package Security.Services;

import Config.Message;
import Config.Service;
import Database.Token.TokenDao;
import Database.User.UserDao;
import Security.EmailSender;
import Security.EmailSenderFactory;
import Security.EmailExceptions;
import Security.EmailUtil;
import Security.SecurityUtils;
import Security.Tokens;
import User.User;
import User.UserMessage;
import Validation.ValidationUtils;

import java.util.Objects;
import java.util.Optional;

public class ForgotPasswordService implements Service {

  UserDao userDao;
  TokenDao tokenDao;
  EmailSender emailSender;
  private String loginIdentifier;
  public static final int EXPIRATION_TIME_2_HOURS = 7200000;

  public ForgotPasswordService(UserDao userDao, TokenDao tokenDao, String loginIdentifier) {
    this(userDao, tokenDao, loginIdentifier, EmailSenderFactory.smtp());
  }

  public ForgotPasswordService(
      UserDao userDao, TokenDao tokenDao, String loginIdentifier, EmailSender emailSender) {
    this.userDao = userDao;
    this.tokenDao = tokenDao;
    this.loginIdentifier = loginIdentifier;
    this.emailSender = emailSender;
  }

  @Override
  public Message executeAndGetResponse() {
    Objects.requireNonNull(loginIdentifier);
    String normalizedIdentifier = loginIdentifier.trim();
    if (normalizedIdentifier.isBlank()) {
      return UserMessage.INVALID_PARAMETER;
    }

    boolean isEmailIdentifier = looksLikeEmailIdentifier(normalizedIdentifier);
    Optional<User> userResult;
    if (isEmailIdentifier) {
      normalizedIdentifier = normalizedIdentifier.toLowerCase();
      if (!ValidationUtils.isValidEmail(normalizedIdentifier)) {
        return UserMessage.INVALID_PARAMETER;
      }
      userResult = userDao.getByEmail(normalizedIdentifier);
    } else {
      if (!ValidationUtils.isValidUsername(normalizedIdentifier)) {
        return UserMessage.INVALID_PARAMETER;
      }
      userResult = userDao.get(normalizedIdentifier);
    }

    if (userResult.isEmpty()) {
      return UserMessage.USER_NOT_FOUND;
    }
    User user = userResult.get();
    String emailAddress = user.getEmail();
    if (!ValidationUtils.hasValue(emailAddress)) {
      return UserMessage.EMAIL_DOES_NOT_EXIST;
    }
    String resolvedUsername = user.getUsername();
    String id = SecurityUtils.generateRandomStringId();
    String jwt =
        SecurityUtils.createJWT(
            id, "KeepID", resolvedUsername, "Password Reset Confirmation", EXPIRATION_TIME_2_HOURS);
    Tokens newToken = new Tokens().setUsername(resolvedUsername).setResetJwt(jwt);
    tokenDao.replaceOne(resolvedUsername, newToken);
    try {
      String emailJWT = EmailUtil.getPasswordResetEmail("https://keep.id/reset-password/" + jwt);
      emailSender.sendEmail("Keep Id", emailAddress, "Password Reset Confirmation", emailJWT);
    } catch (EmailExceptions e) {
      return e;
    }
    return UserMessage.SUCCESS;
  }

  private boolean looksLikeEmailIdentifier(String identifier) {
    int at = identifier.indexOf('@');
    int dot = identifier.lastIndexOf('.');
    return at > 0 && dot > at + 1 && dot < identifier.length() - 1;
  }
}
