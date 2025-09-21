package Activity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class Activity implements Comparable<Activity> {
  private ObjectId id;

  @BsonProperty(value = "occurredAt")
  private LocalDateTime occurredAt;

  @BsonProperty(value = "invokerUsername")
  private String invokerUsername;

  @BsonProperty(value = "targetUsername")
  private String targetUsername;

  @BsonProperty(value = "objectName")
  private String objectName;

  @BsonProperty(value = "type")
  private List<String> type;

  public Activity() {
    this.type = construct();
  }

  // For activities who do not need more info, like logging in
  public Activity(String invokerUsername) {
    this.invokerUsername = invokerUsername;
    this.targetUsername = invokerUsername;
    this.occurredAt = LocalDateTime.now();
    this.type = construct();
  }

  // For activities that are done by the user, for the user
  public Activity(String invokerUsername, String objectName) {
    this.invokerUsername = invokerUsername;
    this.targetUsername = invokerUsername;
    this.objectName = objectName;
    this.occurredAt = LocalDateTime.now();
    this.type = construct();
  }

  // For activities done by a user for another user
  public Activity(String invokerUsername, String targetUsername, String objectName) {
    this.invokerUsername = invokerUsername;
    this.targetUsername = targetUsername;
    this.objectName = objectName;
    this.occurredAt = LocalDateTime.now();
    this.type = construct();
  }

  public List<String> construct() {
    List<String> a = new ArrayList<>();
    a.add(Activity.class.getSimpleName());
    return a;
  }

  public List<String> getType() {
    return type;
  }

  public Activity setType(List<String> type) {
    this.type = type;
    return this;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  public Activity setOccurredAt(LocalDateTime occurredAt) {
    this.occurredAt = occurredAt;
    return this;
  }

  public String getInvokerUsername() {
    return invokerUsername;
  }

  public String getTargetUsername() {
    return targetUsername;
  }

  public String getObjectName() {
    return objectName;
  }

  public ObjectId getId() {
    return id;
  }

  public Activity setId(ObjectId id) {
    this.id = id;
    return this;
  }

  public Activity setInvokerUsername(String invokerUsername) {
    this.invokerUsername = invokerUsername;
    return this;
  }

  public Activity setObjectName(String objectName) {
    this.objectName = objectName;
    return this;
  }

  public Activity setTargetUsername(String targetUsername) {
    this.targetUsername = targetUsername;
    return this;
  }

  // default sort is by occurred at, then invoker username, then target username, then name, finally
  // type
  private Comparator<Activity> getComparator() {
    return Comparator.comparing(
            Activity::getOccurredAt, Comparator.nullsLast(LocalDateTime::compareTo))
        .thenComparing(Activity::getInvokerUsername, Comparator.nullsLast(String::compareTo))
        .thenComparing(Activity::getTargetUsername, Comparator.nullsLast(String::compareTo))
        .thenComparing(Activity::getObjectName, Comparator.nullsLast(String::compareTo))
        .thenComparing(
            activity -> {
              List<String> type = activity.getType();
              if (type == null) {
                return null;
              }
              // join into a single string for lexicographic comparison
              return String.join(",", type);
            },
            Comparator.nullsLast(String::compareTo));
  }

  public JSONObject serialize() {
    JSONObject activityJson = new JSONObject();
    activityJson.put("_id", id);
    activityJson.put("occurredAt", occurredAt);
    activityJson.put("invokerUsername", invokerUsername);
    activityJson.put("targetUsername", targetUsername);
    activityJson.put("objectName", objectName);
    activityJson.put("type", this.getType().get(this.getType().size() - 1));
    return activityJson;
  }

  @Override
  public int compareTo(@NotNull Activity otherActivity) {
    return this.getComparator().compare(this, otherActivity);
  }
}
