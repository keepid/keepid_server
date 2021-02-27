package Database.Activity;

import Activity.Activity;
import org.bson.types.ObjectId;

import java.util.*;

public class ActivityDaoTestImpl implements ActivityDao {

  Map<String, Activity> activityMap;

  public ActivityDaoTestImpl() {
    activityMap = new HashMap<String, Activity>();
  }

  @Override
  public Optional<Activity> get(ObjectId id) {
    return null;
  }

  @Override
  public List<Activity> getAll() {
    return null;
  }

  @Override
  public List<Activity> getAllFromUser(String username) {
    return Collections.singletonList(activityMap.get(username));
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public void save(Activity activity) {}

  @Override
  public void update(Activity activity) {}

  @Override
  public void delete(Activity activity) {}

  @Override
  public void deleteAllFromOrg(String orgName) {}

  @Override
  public void clear() {}
}
