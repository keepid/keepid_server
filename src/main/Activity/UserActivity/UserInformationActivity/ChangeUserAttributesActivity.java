package Activity.UserActivity.UserInformationActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import java.util.ArrayList;
import java.util.List;

public class ChangeUserAttributesActivity extends UserInformationActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(UserInformationActivity.class.getSimpleName());
    a.add(ChangeUserAttributesActivity.class.getSimpleName());
    return a;
  }

  public ChangeUserAttributesActivity() {}

  public ChangeUserAttributesActivity(
      String usernameOfInvoker,
      String attributeName,
      String oldAttributeValue,
      String newAttributeValue) {
    super(usernameOfInvoker, attributeName, oldAttributeValue, newAttributeValue);
  }
}
