package Activity;

import Database.Activity.ActivityDao;
import com.google.inject.Inject;
import io.javalin.http.Handler;
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
    String type = activity.getType().get(activity.getType().size() - 1);
    log.info("Trying to add an activity of type " + type);
    activityDao.save(activity);
    log.info("Successfully added an activity of type " + type);
  }

  public Handler findMyActivities =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String username = req.getString("username");
        List<Activity> res = activityDao.getAllFromUser(username);
        ctx.result(res.toString());
      };
}
