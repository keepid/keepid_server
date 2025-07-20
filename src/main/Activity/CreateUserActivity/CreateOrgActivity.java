package Activity.CreateUserActivity;

import Activity.Activity;
import java.util.ArrayList;
import java.util.List;

public class CreateOrgActivity extends Activity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(CreateOrgActivity.class.getSimpleName());
    return a;
  }

  private String organizationName;

  public CreateOrgActivity() {}

  public CreateOrgActivity(String username, String organizationName) {
    super(username);
    this.organizationName = organizationName;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }
}
