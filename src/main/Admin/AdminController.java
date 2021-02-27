package Admin;

import Config.Message;
import Config.MongoTestConfig;
import Database.Activity.ActivityDao;
import Database.Organization.OrgDao;
import Database.User.UserDao;
import com.google.inject.Inject;
import com.mongodb.client.MongoDatabase;
import io.javalin.http.Context;
import org.json.JSONObject;

public class AdminController {
  UserDao userDao;
  ActivityDao activityDao;
  OrgDao orgDao;
  MongoDatabase db;

  @Inject
  public AdminController(
      UserDao userDao, ActivityDao activityDao, OrgDao orgDao, MongoTestConfig mongoTestConfig) {
    this.userDao = userDao;
    this.activityDao = activityDao;
    this.orgDao = orgDao;
    this.db = mongoTestConfig.getDatabase();
  }

  public void deleteOrg(Context ctx) {
    JSONObject req = new JSONObject(ctx.body());
    String orgName = req.getString("orgName");

    JSONObject responseBuilder = new JSONObject();
    Message responseMessage;

    // Delete orgs
    orgDao.delete(orgName);

    // Delete users
    userDao.deleteAllFromOrg(orgName);

    // TODO(xander) enable
    //         Delete files
    //    DeleteFileService files = new DeleteFileService(db, orgName);
    //    responseMessage = files.executeAndGetResponse();
    //    if (responseMessage != AdminMessages.SUCCESS) {
    //      ctx.result(responseMessage.toJSON().toString());
    //      return;
    //    }

    // Delete activities
    activityDao.deleteAllFromOrg(orgName);

    ctx.result(responseBuilder.toString());
  }
}
