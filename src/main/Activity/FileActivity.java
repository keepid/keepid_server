package Activity;

import File.FileType;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class FileActivity extends UserActivity {
  @BsonProperty(value = "documentOwnerUsername")
  private String documentOwnerUsername;

  @BsonProperty(value = "documentType")
  private String documentType;

  @BsonProperty(value = "documentID")
  private ObjectId documentID;

  @Override
  List<String> construct() {
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
      ObjectId documentID) {
    super(usernameOfInvoker);
    this.documentOwnerUsername = documentOwnerUsername;
    this.documentType = documentType.toString();
    this.documentID = documentID;
  }

  public String getDocumentType() {
    return documentType;
  }

  public void setDocumentType(String documentType) {
    this.documentType = documentType;
  }

  public String getDocumentOwnerUsername() {
    return documentOwnerUsername;
  }

  public void setDocumentOwnerUsername(String documentOwnerUsername) {
    this.documentOwnerUsername = documentOwnerUsername;
  }

  public ObjectId getDocumentID() {
    return documentID;
  }

  public void setDocumentID(ObjectId documentID) {
    this.documentID = documentID;
  }
}
