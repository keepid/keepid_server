package Activity.UserActivity.AuthenticationActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import java.util.ArrayList;
import java.util.List;

public class RecoverPasswordActivity extends AuthenticationActivity {
  private String oldPasswordHash;
  private String newPasswordHash;
  private String recoveryEmail;

  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(AuthenticationActivity.class.getSimpleName());
    a.add(RecoverPasswordActivity.class.getSimpleName());
    return a;
  }

  public RecoverPasswordActivity() {}

  public RecoverPasswordActivity(
      String username, String oldPasswordHash, String newPasswordHash, String recoveryEmail) {
    super(username);
    this.oldPasswordHash = oldPasswordHash;
    this.newPasswordHash = newPasswordHash;
    this.recoveryEmail = recoveryEmail;
  }

  public String getOldPasswordHash() {
    return oldPasswordHash;
  }

  public void setOldPasswordHash(String oldPasswordHash) {
    this.oldPasswordHash = oldPasswordHash;
  }

  public String getNewPasswordHash() {
    return newPasswordHash;
  }

  public void setNewPasswordHash(String newPasswordHash) {
    this.newPasswordHash = newPasswordHash;
  }

  public String getRecoveryEmail() {
    return recoveryEmail;
  }

  public void setRecoveryEmail(String recoveryEmail) {
    this.recoveryEmail = recoveryEmail;
  }
}
