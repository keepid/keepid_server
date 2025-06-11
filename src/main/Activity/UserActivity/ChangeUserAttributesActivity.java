package Activity.UserActivity;

import Activity.Activity;
import java.util.ArrayList;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class ChangeUserAttributesActivity extends UserActivity {
  @Override
  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    a.add(UserActivity.class.getSimpleName());
    a.add(ChangeUserAttributesActivity.class.getSimpleName());
    return a;
  }

  @BsonProperty(value = "attributeName")
  private String attributeName;

  @BsonProperty(value = "oldAttributeValue")
  private String oldAttributeValue;

  @BsonProperty(value = "newAttributeValue")
  private String newAttributeValue;

  public ChangeUserAttributesActivity() {}

  public ChangeUserAttributesActivity(
      String usernameOfInvoker,
      String attributeName,
      String oldAttributeValue,
      String newAttributeValue) {
    super(usernameOfInvoker, attributeName);
    this.attributeName = attributeName;
    this.oldAttributeValue = oldAttributeValue;
    this.newAttributeValue = newAttributeValue;
  }

  public String getAttributeName() {
    return attributeName;
  }

  public void setAttributeName(String attributeName) {
    this.attributeName = attributeName;
  }

  public String getOldAttributeValue() {
    return oldAttributeValue;
  }

  public void setOldAttributeValue(String oldAttributeValue) {
    this.oldAttributeValue = oldAttributeValue;
  }

  public String getNewAttributeValue() {
    return newAttributeValue;
  }

  public void setNewAttributeValue(String newAttributeValue) {
    this.newAttributeValue = newAttributeValue;
  }
}
