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
    for (Activity activity : activityMap.values()) {
      if (activity.getId().equals(id)) {
        return Optional.of(activity);
      }
    }
    return Optional.empty();
  }

  @Override
  public List<Activity> getAll() {
    return new ArrayList<Activity>(activityMap.values());
  }

  @Override
  public List<Activity> getAllFromUser(String username) {
    List<Activity> result = new ArrayList<>();
    for (Activity activity : activityMap.values()) {
      if (activity.getOwner().getUsername().equals(username)) {
        result.add(activity);
      }
    }
    return result;
  }

  @Override
  public int size() {
    return activityMap.size();
  }

  @Override
  public void save(Activity activity) {
    activityMap.put(activity.getId().toString(), activity);
  }

  @Override
  public void update(Activity activity) {
    activityMap.put(activity.getId().toString(), activity);
  }

  @Override
  public void delete(Activity activity) {
    activityMap.remove(activity.getId().toString());
  }

  @Override
  public void deleteAllFromOrg(String orgName) {
    for (Activity activity : activityMap.values()) {
      if (activity.getOwner().getOrganization().equals(orgName)) {
        activityMap.remove(activity.getId().toString());
      }
    }
  }

  @Override
  public void clear() {
    activityMap.clear();
  }
}
