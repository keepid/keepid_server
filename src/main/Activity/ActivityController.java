package Activity;

import Database.Activity.ActivityDao;
import com.google.inject.Inject;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.List;

@Slf4j
public class ActivityController {
  ActivityDao activityDao;

  @Inject
  public ActivityController(ActivityDao activityDao) {
    this.activityDao = activityDao;
  }

  public void addActivity(Activity activity) {
    String type = activity.getType().stream().findFirst().orElse("UNKNOWN");
    log.debug("Adding an activity of type " + type);
    activityDao.save(activity);
  }

  public void findMyActivities(Context ctx) {
    JSONObject req = new JSONObject(ctx.body());
    String username = req.getString("username");
    List<Activity> res = activityDao.getAllFromUser(username);
    ctx.result(res.toString());
  }
}
