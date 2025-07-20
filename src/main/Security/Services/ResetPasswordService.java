package Security.Services;

import static Security.SecurityUtils.hashPassword;

import Activity.UserActivity.AuthenticationActivity.RecoverPasswordActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.Token.TokenDao;
import Database.User.UserDao;
import Security.SecurityUtils;
import Security.Tokens;
import User.User;
import User.UserMessage;
import Validation.ValidationUtils;
import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.Optional;

public class ResetPasswordService implements Service {
  private UserDao userDao;
  private TokenDao tokenDao;
  private ActivityDao activityDao;
  private String jwt;
  private String newPassword;

  public ResetPasswordService(
      UserDao userDao, TokenDao tokenDao, ActivityDao activityDao, String jwt, String newPassword) {
    this.userDao = userDao;
    this.tokenDao = tokenDao;
    this.activityDao = activityDao;
    this.jwt = jwt;
    this.newPassword = newPassword;
  }

  @Override
  public Message executeAndGetResponse() {
    // Decode the JWT. If invalid, return AUTH_FAILURE.
    if (!ValidationUtils.isValidPassword(newPassword)) {
      return UserMessage.INVALID_PARAMETER.withMessage("Invalid Password");
    }
    Claims claim;
    try {
      claim = SecurityUtils.decodeJWT(jwt);
    } catch (Exception e) {
      return UserMessage.AUTH_FAILURE.withMessage("Invalid reset link.");
    }

    String username = claim.getAudience();
    Optional<User> userResult = userDao.get(username);
    if (userResult.isEmpty()) {
      return UserMessage.USER_NOT_FOUND;
    }
    User user = userResult.get();
    long nowMillis = System.currentTimeMillis();
    Date now = new Date(nowMillis);
    if (claim.getExpiration().compareTo(now) < 0) {
      return UserMessage.AUTH_FAILURE.withMessage("Reset link expired.");
    }
    // Check if reset token exists.
    Optional<Tokens> tokenResult = tokenDao.get(username);
    if (tokenResult.isEmpty()) {
      return UserMessage.AUTH_FAILURE.withMessage("Reset token not found for user.");
    }
    Tokens tokens = tokenResult.get();
    String storedJWT = tokens.getResetJwt();
    if (storedJWT == null) {
      return UserMessage.AUTH_FAILURE.withMessage("Reset token not found for user.");
    }
    if (!storedJWT.equals(jwt)) {
      return UserMessage.AUTH_FAILURE.withMessage("Invalid reset token.");
    }
    tokenDao.removeTokenIfLast(username, tokens, Tokens.TokenType.PASSWORD_RESET);
    userDao.resetPassword(user, hashPassword(newPassword));
    recordRecoverPasswordActivity(username);
    return UserMessage.SUCCESS;
  }

  private void recordRecoverPasswordActivity(String username) {
    RecoverPasswordActivity recoverPasswordActivity = new RecoverPasswordActivity(username);
    activityDao.save(recoverPasswordActivity);
  }
}
