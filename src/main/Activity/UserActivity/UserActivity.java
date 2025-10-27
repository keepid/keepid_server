package Activity.UserActivity;

import Activity.Activity;
import java.util.ArrayList;
import java.util.List;

public class UserActivity extends Activity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    return a;
  }

  public UserActivity() {
    super();
  }

  public UserActivity(String username) {
    super(username);
  }

  public UserActivity(String username, String objectName) {
    super(username, objectName);
  }

  public UserActivity(String username, String objectName, boolean temporary) {
    super(username, objectName, temporary);
  }

  public UserActivity(String invokerUsername, String targetUsername, String objectName) {
    super(invokerUsername, targetUsername, objectName);
  }
}
