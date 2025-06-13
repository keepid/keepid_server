package Activity.UserActivity;

import Activity.Activity;
import java.util.ArrayList;
import java.util.List;

public class ChangeOptionalUserInformationActivity extends UserActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(ChangeOptionalUserInformationActivity.class.getSimpleName());
    return a;
  }

  public ChangeOptionalUserInformationActivity() {}

  public ChangeOptionalUserInformationActivity(String username) {
    super(username);
  }
}
