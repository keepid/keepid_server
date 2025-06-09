package Activity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

// Refactor this, can consider removing getters and setters. Also, get other activities to work
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

  public Activity(String creatorUsername) {
    this.invokerUsername = creatorUsername;
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

  // Deprecate
  public String getUsername() {
    return invokerUsername;
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

  // Deprecate
  public Activity setUsername(String username) {
    this.invokerUsername = username;
    return this;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  // default sort is by occurred at, then invoker username, then target username, then name, finally
  // type
  private Comparator<Activity> getComparator() {
    return Comparator.comparing(Activity::getOccurredAt)
        .thenComparing(Activity::getInvokerUsername)
        .thenComparing(Activity::getTargetUsername)
        .thenComparing(Activity::getObjectName)
        .thenComparingInt(
            activity ->
                activity.getType().stream()
                    .flatMap(type -> Stream.of(type.hashCode()))
                    .reduce(Integer::sum)
                    .orElse(0));
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
