package Activity.UserActivity.ApplicationActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import java.util.ArrayList;
import java.util.List;

public class MailApplicationActivity extends ApplicationActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(ApplicationActivity.class.getSimpleName());
    a.add(MailApplicationActivity.class.getSimpleName());
    return a;
  }

  public MailApplicationActivity() {}

  public MailApplicationActivity(
      String usernameOfInvoker,
      String applicationOwnerUsername,
      String applicationID,
      String applicationName) {
    super(usernameOfInvoker, applicationOwnerUsername, applicationID, applicationName);
  }
}
