package Activity.UserActivity.FileActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import File.FileType;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;

public class DeleteFileActivity extends FileActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(FileActivity.class.getSimpleName());
    a.add(DeleteFileActivity.class.getSimpleName());
    return a;
  }

  public DeleteFileActivity() {
    super();
  }

  public DeleteFileActivity(
      String usernameOfInvoker,
      String documentOwner,
      FileType fileType,
      ObjectId id,
      String filename) {
    super(usernameOfInvoker, documentOwner, fileType, id, filename);
  }
}
