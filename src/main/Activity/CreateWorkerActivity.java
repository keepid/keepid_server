package Activity;

import User.User;

import java.util.ArrayList;
import java.util.List;

public class CreateWorkerActivity extends CreateUserActivity {
  @Override
  List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(CreateUserActivity.class.getSimpleName());
    a.add(CreateWorkerActivity.class.getSimpleName());
    return a;
  }

  private User creator;

  public CreateWorkerActivity() {
    super();
  }

  public CreateWorkerActivity(String username, String createdUsername) {
    super(username, createdUsername);
  }

  public User getCreator() {
    return creator;
  }

  public void setCreator(User creator) {
    this.creator = creator;
  }
}
