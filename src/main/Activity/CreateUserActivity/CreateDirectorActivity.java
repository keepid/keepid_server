package Activity.CreateUserActivity;

import Activity.Activity;
import java.util.ArrayList;
import java.util.List;

public class CreateDirectorActivity extends CreateUserActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(CreateUserActivity.class.getSimpleName());
    a.add(CreateDirectorActivity.class.getSimpleName());
    return a;
  }

  public CreateDirectorActivity() {
    super();
  }

  public CreateDirectorActivity(String username, String createdUsername) {
    super(username, createdUsername);
  }
}
