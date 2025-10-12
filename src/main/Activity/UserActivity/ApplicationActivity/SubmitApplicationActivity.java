package Activity.UserActivity.ApplicationActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import java.util.ArrayList;
import java.util.List;

public class SubmitApplicationActivity extends ApplicationActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(ApplicationActivity.class.getSimpleName());
    a.add(SubmitApplicationActivity.class.getSimpleName());
    return a;
  }

  public SubmitApplicationActivity() {}

  // Since this requires the client's signature, I assume invoker is the same as target
  public SubmitApplicationActivity(
      String usernameOfInvoker,
      String applicationOwnerUsername,
      String applicationID,
      String applicationName) {
    super(usernameOfInvoker, applicationOwnerUsername, applicationID, applicationName);
  }
}
