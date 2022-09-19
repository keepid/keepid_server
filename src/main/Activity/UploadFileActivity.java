package Activity;

import File.FileType;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class UploadFileActivity extends FileActivity {
  @Override
  List<String> construct() {
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
      String usernameOfInvoker, String targeUsername, FileType fileType, ObjectId id) {
    super(usernameOfInvoker, targeUsername, fileType, id);
  }
}
