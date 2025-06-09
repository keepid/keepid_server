package Activity.UserActivity.FileActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import File.FileType;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;

public class UploadFileActivity extends FileActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(FileActivity.class.getSimpleName());
    a.add(UploadFileActivity.class.getSimpleName());
    return a;
  }

  public UploadFileActivity() {
    super();
  }

  public UploadFileActivity(
      String usernameOfInvoker, String targetUsername, FileType fileType, ObjectId id) {
    super(usernameOfInvoker, targetUsername, fileType, id);
  }
}
