package Database.Activity;

import Activity.Activity;
import Config.DeploymentLevel;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ActivityDaoTestImpl implements ActivityDao {
  Map<ObjectId, Activity> activityMap;

  public ActivityDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in memory test database in production or staging");
    }
    activityMap = new LinkedHashMap<>();
  }

  @Override
  public List<Activity> getAllFromUser(String username) {
    return activityMap.values().stream()
        .filter(activity -> activity.getUsername().equals(username))
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
    return Optional.ofNullable(activityMap.get(id));
  }

  @Override
  public List<Activity> getAll() {
    return activityMap.values().stream()
        .sorted(Comparator.reverseOrder())
        .collect(Collectors.toList());
  }

  @Override
  public int size() {
    return activityMap.size();
  }

  @Override
  public void clear() {
    activityMap.clear();
  }

  @Override
  public void delete(Activity activity) {
    activityMap.remove(activity.getId());
  }

  @Override
  public void update(Activity activity) {
    activityMap.put(activity.getId(), activity);
  }

  @Override
  public void save(Activity activity) {
    activityMap.put(activity.getId(), activity);
  }

  @Override
  public List<Activity> findUnnotified(int limit) {
    return new ArrayList<>();
  }
  @Override
  public List<Activity> getUnnotifiedActivities() {
    return Collections.emptyList();
  }

}
