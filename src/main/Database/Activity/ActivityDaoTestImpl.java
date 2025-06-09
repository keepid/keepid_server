package Database.Activity;

import Activity.Activity;
import Config.DeploymentLevel;
import User.User;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;

public class ActivityDaoTestImpl implements ActivityDao {
  Map<ObjectId, Activity> activityMap;
  Map<String, User> userMap;

  public ActivityDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in memory test database in production or staging");
    }
    activityMap = new LinkedHashMap<>();
    userMap = new LinkedHashMap<>();
  }

  @Override
  public List<Activity> getAllFromUser(String username) {
    return activityMap.values().stream()
        .filter(
            activity ->
                activity.getInvokerUsername().equals(username)
                    || activity.getTargetUsername().equals(username))
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

  // This is so wrong
  private List<User> getUsersFromOrg(String orgName) {
    List<User> result = new ArrayList<>();
    for (User user : userMap.values()) {
      if (user.getOrganization().equals(orgName)) {
        result.add(user);
      }
    }
    return result;
  }

  @Override
  public List<Activity> getAllFromOrganization(String organization) {
    List<User> users = getUsersFromOrg(organization);
    Set<String> usernames =
        users.stream().map(user -> user.getUsername()).collect(Collectors.toSet());
    return activityMap.values().stream()
        .filter(
            activity ->
                usernames.contains(activity.getInvokerUsername())
                    || usernames.contains(activity.getTargetUsername()))
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
}
