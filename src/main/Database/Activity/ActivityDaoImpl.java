package Database.Activity;

import Activity.Activity;
import Config.DeploymentLevel;
import Config.MongoConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class ActivityDaoImpl implements ActivityDao {
  private final MongoCollection<Activity> activityCollection;

  public ActivityDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    activityCollection = db.getCollection("activity", Activity.class);
  }

  @Override // sorted by most recent created
  public List<Activity> getAllFromUser(String username) {
    return activityCollection.find(eq("username", username)).into(new ArrayList<>()).stream()
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
  //
  @Override
  public List<Activity> getUnnotifiedActivities() {
    return activityCollection.find(eq("notified", false))
        .into(new ArrayList<>()).stream()
        .sorted(Comparator.reverseOrder()) // optional
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

    activityCollection.insertOne(activity)
    // Trigger email notifications
    EmailNotifier.handle(activity);;
  }
}
