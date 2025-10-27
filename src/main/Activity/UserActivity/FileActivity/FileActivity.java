package Activity.UserActivity.FileActivity;

import Activity.Activity;
import Activity.UserActivity.UserActivity;
import File.FileType;
import java.util.ArrayList;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

public class FileActivity extends UserActivity {
  //  @BsonProperty(value = "documentOwnerUsername")
  //  private String documentOwnerUsername;

  @BsonProperty(value = "documentType")
  private String documentType;

  //  @BsonProperty(value = "documentID")
  //  private ObjectId documentID;
  //
  //  @BsonProperty(value = "filename")
  //  private String filename;

  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(FileActivity.class.getSimpleName());
    return a;
  }

  public FileActivity() {}

  public FileActivity(
      String usernameOfInvoker,
      String documentOwnerUsername,
      FileType documentType,
      ObjectId documentID,
      String filename) {
    super(usernameOfInvoker, documentOwnerUsername, filename);
    //    this.documentOwnerUsername = documentOwnerUsername;
    this.documentType = documentType.toString();
    //    this.documentID = documentID;
    //    this.filename = filename;
  }

  public String getDocumentType() {
    return documentType;
  }

  public void setDocumentType(String documentType) {
    this.documentType = documentType;
  }
}
