package Activity;

import File.FileType;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class ViewFileActivity extends FileActivity {
  @Override
  List<String> construct() {
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
      String usernameOfInvoker, String targetUsername, FileType fileType, ObjectId id) {
    super(usernameOfInvoker, targetUsername, fileType, id);
  }
}
