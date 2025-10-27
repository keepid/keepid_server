package Activity.UserActivity.AuthenticationActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import java.util.ArrayList;
import java.util.List;

public class ChangePasswordActivity extends AuthenticationActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(AuthenticationActivity.class.getSimpleName());
    a.add(ChangePasswordActivity.class.getSimpleName());
    return a;
  }

  public ChangePasswordActivity() {}

  public ChangePasswordActivity(String username) {
    super(username);
  }
}
