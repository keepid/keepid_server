package Activity;

import Activity.Services.GetAllActivitiesForUser;
import Config.Message;
import Database.Activity.ActivityDao;
import User.UserMessage;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import static com.google.common.base.Preconditions.checkState;

@Slf4j
public class ActivityController {
  ActivityDao activityDao;

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
        checkState(ctx.sessionAttribute("username") != null);
        String username = req.getString("username");
        GetAllActivitiesForUser fas = new GetAllActivitiesForUser(activityDao, username);
        Message responseMessage = fas.executeAndGetResponse();
        JSONObject res = responseMessage.toJSON();
        if (responseMessage == UserMessage.SUCCESS) {
          res.put("username", fas.getUsername());
          res.put("activities", fas.getActivitiesArray());
        }
        ctx.result(res.toString());
      };
}
