package Activity.UserActivity.AuthenticationActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import java.util.ArrayList;
import java.util.List;

public class AuthenticationActivity extends UserActivity {
  public AuthenticationActivity() {}

  public AuthenticationActivity(String username) {
    super(username);
  }

  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(AuthenticationActivity.class.getSimpleName());
    return a;
  }
}
