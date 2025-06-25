package Activity.UserActivity.ApplicationActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import java.util.ArrayList;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class ApplicationActivity extends UserActivity {
  @BsonProperty(value = "applicationOwnerUsername")
  private String applicationOwnerUsername;

  @BsonProperty(value = "applicationID")
  private String applicationID;

  @BsonProperty(value = "applicationName")
  private String applicationName;

  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(ApplicationActivity.class.getSimpleName());
    return a;
  }

  public ApplicationActivity() {}

  public ApplicationActivity(
      String usernameOfInvoker,
      String applicationOwnerUsername,
      String applicationID,
      String applicationName) {
    super(usernameOfInvoker, applicationOwnerUsername, applicationName);
    this.applicationOwnerUsername = applicationOwnerUsername;
    this.applicationID = applicationID;
    this.applicationName = applicationName;
  }
}
