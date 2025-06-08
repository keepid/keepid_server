package Activity;

import java.util.ArrayList;
import java.util.List;

public class ApplicationActivity extends UserActivity {
  @Override
  List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(ApplicationActivity.class.getSimpleName());
    return a;
  }
}
