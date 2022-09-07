package Activity;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Activity implements Comparable<Activity> {
  private ObjectId id;

  @BsonProperty(value = "occurredAt")
  private LocalDateTime occurredAt;

  @BsonProperty(value = "username")
  private String username;

  @BsonProperty(value = "type")
  private List<String> type;

  public Activity() {
    this.type = construct();
  }

  Activity(String creatorUsername) {
    this.username = creatorUsername;
    this.occurredAt = LocalDateTime.now();
    this.type = construct();
  }

  List<String> construct() {
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

  public String getUsername() {
    return username;
  }

  public ObjectId getId() {
    return id;
  }

  public Activity setUsername(String username) {
    this.username = username;
    return this;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  // default sort is by occurred at, and then by username
  private Comparator<Activity> getComparator() {
    return Comparator.comparing(Activity::getOccurredAt)
        .thenComparing(Activity::getUsername)
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
    activityJson.put("username", username);
    activityJson.put("occurredAt", occurredAt);
    activityJson.put("type", this.getType().get(this.getType().size() - 1));
    return activityJson;
  }

  @Override
  public int compareTo(@NotNull Activity otherActivity) {
    return this.getComparator().compare(this, otherActivity);
  }
}
