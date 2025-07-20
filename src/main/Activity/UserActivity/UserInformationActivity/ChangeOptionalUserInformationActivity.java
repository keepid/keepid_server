package Activity.UserActivity.UserInformationActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import java.util.ArrayList;
import java.util.List;

public class ChangeOptionalUserInformationActivity extends UserInformationActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(UserInformationActivity.class.getSimpleName());
    a.add(ChangeOptionalUserInformationActivity.class.getSimpleName());
    return a;
  }

  public ChangeOptionalUserInformationActivity() {}

  // Eventually deprecate
  public ChangeOptionalUserInformationActivity(String username) {
    super(username);
  }

  public ChangeOptionalUserInformationActivity(
      String username, String attributeName, String oldAttributeValue, String newAttributeValue) {
    super(username, attributeName, oldAttributeValue, newAttributeValue);
  }
}
