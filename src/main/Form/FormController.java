package Form;

import Database.Form.FormDao;
import Form.Services.DeleteFormService;
import Security.EncryptionController;
import User.User;
import User.UserMessage;
import User.UserType;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class FormController {
  private MongoDatabase db;
  private FormDao formDao;
  private EncryptionController encryptionController;

  public FormController(MongoDatabase db, FormDao formDao) {
    this.db = db;
    this.formDao = formDao;
    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {

    }
  }

  public Handler formDelete =
      ctx -> {
        String username;
        String orgName;
        UserType userType;
        JSONObject req = new JSONObject(ctx.body());
        User check = userCheck(ctx.body());
        if (check == null && req.has("targetUser")) {
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          boolean orgFlag;
          if (check != null && req.has("targetUser")) {
            log.info("Target user found");
            username = check.getUsername();
            orgName = check.getOrganization();
            userType = check.getUserType();
            orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
          } else {
            username = ctx.sessionAttribute("username");
            orgName = ctx.sessionAttribute("orgName");
            userType = ctx.sessionAttribute("privilegeLevel");
            // User is in same org as themselves
            orgFlag = true;
          }

          if (orgFlag) {
            boolean isTemplate = Boolean.valueOf(req.getString("isTemplate"));
            String fileIDStr = req.getString("fileId");
            ObjectId fileId = new ObjectId(fileIDStr);

            DeleteFormService deleteFormService =
                new DeleteFormService(formDao, fileId, username, userType, isTemplate);
            ctx.result(deleteFormService.executeAndGetResponse().toResponseString());
          } else {
            ctx.result(UserMessage.CROSS_ORG_ACTION_DENIED.toResponseString());
          }
        }
      };

  public User userCheck(String req) {
    log.info("userCheck Helper started");
    String username;
    User user = null;
    try {
      JSONObject reqJson = new JSONObject(req);
      if (reqJson.has("targetUser")) {
        username = reqJson.getString("targetUser");
        MongoCollection<User> userCollection = this.db.getCollection("user", User.class);
        user = userCollection.find(eq("username", username)).first();
      }
    } catch (JSONException e) {

    }
    log.info("userCheck done");
    return user;
  }
}
