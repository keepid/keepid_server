package Activity.UserActivity.AuthenticationActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import java.util.ArrayList;
import java.util.List;

public class Change2FAActivity extends AuthenticationActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(AuthenticationActivity.class.getSimpleName());
    a.add(Change2FAActivity.class.getSimpleName());
    return a;
  }

  public Change2FAActivity() {}

  public Change2FAActivity(String username, String isTwoFactorOn) {
    super(username, isTwoFactorOn);
  }
}
