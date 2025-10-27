package Activity.CreateUserActivity;

import Activity.Activity;
import java.util.ArrayList;
import java.util.List;

public class CreateAdminActivity extends CreateUserActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(CreateUserActivity.class.getSimpleName());
    a.add(CreateAdminActivity.class.getSimpleName());
    return a;
  }

  public CreateAdminActivity() {
    super();
  }

  public CreateAdminActivity(String username, String createdUsername) {
    super(username, createdUsername);
  }
}
