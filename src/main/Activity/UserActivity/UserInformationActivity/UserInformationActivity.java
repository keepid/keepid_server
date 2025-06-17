package Activity.UserActivity.UserInformationActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import java.util.ArrayList;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class UserInformationActivity extends UserActivity {
  @BsonProperty(value = "attributeName")
  private String attributeName;

  @BsonProperty(value = "oldAttributeValue")
  private String oldAttributeValue;

  @BsonProperty(value = "newAttributeValue")
  private String newAttributeValue;

  // Eventually deprecate
  public UserInformationActivity(String username) {
    super(username);
  }

  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(UserInformationActivity.class.getSimpleName());
    return a;
  }

  public UserInformationActivity() {}

  public UserInformationActivity(
      String username, String attributeName, String oldAttributeValue, String newAttributeValue) {
    super(username, attributeName);
    this.attributeName = attributeName;
    this.oldAttributeValue = oldAttributeValue;
    this.newAttributeValue = newAttributeValue;
  }
}
