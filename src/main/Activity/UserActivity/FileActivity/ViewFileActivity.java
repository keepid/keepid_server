package Activity.UserActivity.FileActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import File.FileType;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;

public class ViewFileActivity extends FileActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(FileActivity.class.getSimpleName());
    a.add(ViewFileActivity.class.getSimpleName());
    return a;
  }

  public ViewFileActivity() {
    super();
  }

  public ViewFileActivity(
      String usernameOfInvoker,
      String targetUsername,
      FileType fileType,
      ObjectId id,
      String filename) {
    super(usernameOfInvoker, targetUsername, fileType, id, filename);
  }
}
