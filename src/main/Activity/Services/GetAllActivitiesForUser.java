package Activity.Services;

import Activity.Activity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import User.UserMessage;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GetAllActivitiesForUser implements Service {
  private final ActivityDao activityDao;
  private final String username;
  private List<Activity> activities;

  public GetAllActivitiesForUser(ActivityDao activityDao, String username) {
    this.activityDao = activityDao;
    this.username = username;
  }

  @Override
  public Message executeAndGetResponse() {
    this.activities = activityDao.getAllFromUser(username);
    return UserMessage.SUCCESS;
  }

  public List<JSONObject> getActivitiesArray() {
    Objects.requireNonNull(activities);
    return activities.stream().map(activity -> activity.serialize()).collect(Collectors.toList());
  }

  public String getUsername() {
    Objects.requireNonNull(username);
    return username;
  }
}
