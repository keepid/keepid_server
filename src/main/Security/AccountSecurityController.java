package Security;

import Config.Message;
import Database.Activity.ActivityDao;
import Database.Token.TokenDao;
import Database.User.UserDao;
import Security.Services.*;
import User.UserMessage;
import io.javalin.http.Handler;
import org.json.JSONObject;

public class AccountSecurityController {
  private UserDao userDao;
  private TokenDao tokenDao;
  private ActivityDao activityDao;
  private EncryptionUtils encryptionUtils;

  public AccountSecurityController(UserDao userDao, TokenDao tokenDao, ActivityDao activityDao) {
    this.userDao = userDao;
    this.tokenDao = tokenDao;
    this.activityDao = activityDao;
    this.encryptionUtils = EncryptionUtils.getInstance();
  }

  public Handler forgotPassword =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String loginIdentifier =
            req.optString("username", req.optString("identifier", req.optString("email", "")));
        ForgotPasswordService forgotPasswordService =
            new ForgotPasswordService(userDao, tokenDao, loginIdentifier);
        ctx.result(forgotPasswordService.executeAndGetResponse().toResponseString());
      };

  // Changes the password of a logged in user.
  public Handler changePassword =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String username = ctx.sessionAttribute("username");
        String oldPassword = req.getString("oldPassword");
        String newPassword = req.getString("newPassword");
        ChangePasswordService changePasswordService =
            new ChangePasswordService(userDao, username, activityDao, oldPassword, newPassword);
        ctx.result(changePasswordService.executeAndGetResponse().toResponseString());
      };

  public Handler changeAccountSetting =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String username = ctx.sessionAttribute("username");
        String password = req.getString("password");
        String key = req.getString("key");
        String value = req.get("value").toString();
        ChangeAccountSettingService changeAccountSettingService =
            new ChangeAccountSettingService(userDao, activityDao, username, password, key, value);
        ctx.result(changeAccountSettingService.executeAndGetResponse().toResponseString());
      };

  public Handler resetPassword =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        // Decode the JWT. If invalid, return AUTH_FAILURE.
        String jwt = req.getString("jwt");
        String newPassword = req.getString("newPassword");
        ResetPasswordService resetPasswordService =
            new ResetPasswordService(userDao, tokenDao, activityDao, jwt, newPassword);
        ctx.result(resetPasswordService.executeAndGetResponse().toResponseString());
      };

}
