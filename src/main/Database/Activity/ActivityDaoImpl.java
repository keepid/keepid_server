package Database.Activity;

import Activity.Activity;
import Config.MongoTestConfig;
import com.google.inject.Inject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActivityDaoImpl implements ActivityDao {
  private final MongoCollection<Activity> activityCollection;

  @Inject
  public ActivityDaoImpl(MongoTestConfig mongoTestConfig) {
    this.activityCollection =
        mongoTestConfig.getDatabase().getCollection("activity", Activity.class);
  }

  @Override
  public Optional<Activity> get(ObjectId id) {
    return null;
  }

  @Override
  public List<Activity> getAllFromUser(String username) {
    return activityCollection.find(Filters.eq("owner.username", username)).into(new ArrayList<>());
  }

  @Override
  public List<Activity> getAll() {
    return activityCollection.find().into(new ArrayList<>());
  }

  @Override
  public int size() {
    return (int) activityCollection.countDocuments();
  }

  @Override
  public void save(Activity activity) {
    activityCollection.insertOne(activity);
  }

  @Override
  public void update(Activity activity) {
    activityCollection.replaceOne(
        Filters.eq("owner.username", activity.getOwner().getUsername()), activity);
  }

  @Override
  public void delete(Activity activity) {
    activityCollection.deleteOne(Filters.eq("owner.username", activity.getOwner().getUsername()));
  }

  @Override
  public void deleteAllFromOrg(String orgName) {
    activityCollection.deleteMany(Filters.eq("owner.organization"));
  }

  @Override
  public void clear() {
    activityCollection.drop();
  }
}
