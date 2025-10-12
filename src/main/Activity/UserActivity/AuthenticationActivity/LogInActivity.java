package Activity.UserActivity.AuthenticationActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import java.util.ArrayList;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class LogInActivity extends AuthenticationActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(AuthenticationActivity.class.getSimpleName());
    a.add(LogInActivity.class.getSimpleName());
    return a;
  }

  @BsonProperty(value = "isTwoFactor")
  private boolean isTwoFactor;

  public LogInActivity() {}

  public LogInActivity(String username, Boolean isTwoFactor) {
    super(username);
    this.isTwoFactor = isTwoFactor;
  }

  public boolean isTwoFactor() {
    return isTwoFactor;
  }

  public void setTwoFactor(boolean twoFactor) {
    isTwoFactor = twoFactor;
  }
}
