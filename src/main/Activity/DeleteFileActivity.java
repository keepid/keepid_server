package Activity;

import File.FileType;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class DeleteFileActivity extends FileActivity {
  @Override
  List<String> construct() {
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
      String usernameOfInvoker, String documentOwner, FileType fileType, ObjectId id) {
    super(usernameOfInvoker, documentOwner, fileType, id);
  }
}
