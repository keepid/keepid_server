package Database.Activity;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.or;

import Activity.Activity;
import Config.DeploymentLevel;
import Config.MongoConfig;
import User.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;

public class ActivityDaoImpl implements ActivityDao {
  private final MongoCollection<Activity> activityCollection;
  private final MongoCollection<User> userCollection;

  public ActivityDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    activityCollection = db.getCollection("activity", Activity.class);
    userCollection = db.getCollection("user", User.class);
  }

  @Override // sorted by most recent created
  public List<Activity> getAllFromUser(String username) {
    return activityCollection
        .find(or(eq("invokerUsername", username), eq("targetUsername", username)))
        .into(new ArrayList<>())
        .stream()
        .sorted(Comparator.reverseOrder())
        .collect(Collectors.toList());
  }

  @Override
  public List<Activity> getAllFileActivitiesFromUser(String username) {
    return getAllFromUser(username).stream()
        .filter(activity -> activity.getType().contains("FileActivity"))
        .sorted(Comparator.reverseOrder())
        .collect(Collectors.toList());
  }

  @Override
  public List<Activity> getAllFromOrganization(String organization) {
    List<User> users =
        userCollection.find(eq("organization", organization)).into(new ArrayList<>());
    Set<String> usernames =
        users.stream().map(user -> user.getUsername()).collect(Collectors.toSet());
    return activityCollection
        .find(or(in("invokerUsername", usernames), in("targetUsername", usernames)))
        .into(new ArrayList<>())
        .stream()
        .sorted(Comparator.reverseOrder())
        .collect(Collectors.toList());
  }

  @Override
  public List<Activity> getAllFromUserBetweenInclusive(
      String username, LocalDateTime startTime, LocalDateTime endTime) {
    return getAllFromUser(username).stream()
        .filter(
            activity ->
                activity.getOccurredAt().isAfter(startTime)
                    && activity.getOccurredAt().isBefore(endTime))
        .sorted(Comparator.reverseOrder())
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Activity> get(ObjectId id) {
    return Optional.ofNullable(activityCollection.find(eq("_id", id)).first());
  }

  @Override
  public List<Activity> getAll() {
    return activityCollection.find().into(new ArrayList<>()).stream()
        .sorted(Comparator.reverseOrder())
        .collect(Collectors.toList());
  }

  @Override
  public int size() {
    return (int) activityCollection.countDocuments();
  }

  @Override
  public void clear() {
    activityCollection.drop();
  }

  @Override
  public void delete(Activity activity) {
    activityCollection.deleteOne(eq("_id", activity.getId()));
  }

  @Override
  public void update(Activity activity) {
    activityCollection.replaceOne(eq("_id", activity.getId()), activity);
  }

  @Override
  public void save(Activity activity) {
    activityCollection.insertOne(activity);
  }
}
