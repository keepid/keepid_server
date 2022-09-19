package Activity;

import File.FileType;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

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
      String usernameOfInvoker, String targetUsername, FileType pdfType, ObjectId id) {
    super(usernameOfInvoker, targetUsername, pdfType, id);
  }
}
