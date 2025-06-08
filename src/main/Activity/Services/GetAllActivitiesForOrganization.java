package Activity.Services;

import Activity.Activity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import User.UserMessage;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class GetAllActivitiesForOrganization implements Service {
  private final ActivityDao activityDao;
  private final String organization;
  private List<Activity> activities;

  public GetAllActivitiesForOrganization(ActivityDao activityDao, String organization) {
    this.activityDao = activityDao;
    this.organization = organization;
  }

  @Override
  public Message executeAndGetResponse() throws Exception {
    this.activities = activityDao.getAllFromOrganization(organization);
    return UserMessage.SUCCESS;
  }

  public List<JSONObject> getActivitiesArray() {
    Objects.requireNonNull(activities);
    return activities.stream().map(activity -> activity.serialize()).collect(Collectors.toList());
  }

  public String getOrganization() {
    Objects.requireNonNull(organization);
    return organization;
  }
}
