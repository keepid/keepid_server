package Activity;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.ArrayList;
import java.util.List;

public class CreateUserActivity extends Activity {

  @BsonProperty(value = "created")
  private String createdUsername;

  public CreateUserActivity() {}

  @Override
  List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(CreateUserActivity.class.getSimpleName());
    return a;
  }

  public CreateUserActivity(String usernameOfInvoker, String createdUsername) {
    super(usernameOfInvoker);
    this.createdUsername = createdUsername;
  }

  public String getCreatedUsername() {
    return createdUsername;
  }

  public void setCreatedUsername(String createdUsername) {
    this.createdUsername = createdUsername;
  }
}
