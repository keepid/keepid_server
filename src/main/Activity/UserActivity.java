package Activity;

import java.util.ArrayList;
import java.util.List;

public class UserActivity extends Activity {
  @Override
  List<String> construct() {
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
}
