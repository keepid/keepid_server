package Activity;

import File.FileType;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;

public class DownloadFileActivity extends FileActivity {
  @Override
  List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(FileActivity.class.getSimpleName());
    a.add(DownloadFileActivity.class.getSimpleName());
    return a;
  }

  public DownloadFileActivity() {
    super();
  }

  public DownloadFileActivity(
      String usernameOfInvoker, String targetUsername, FileType fileType, ObjectId id) {
    super(usernameOfInvoker, targetUsername, fileType, id);
  }
}
