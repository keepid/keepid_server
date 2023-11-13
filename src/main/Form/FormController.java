package Form;

import Config.Message;
import Database.Form.FormDao;
import Form.Services.DeleteFormService;
import Form.Services.GetFormService;
import Form.Services.UploadFormService;
import Form.Services.MailFormService;
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
            log.info("Target form found");
            username = check.getUsername();
            orgName = check.getOrganization();
            userType = check.getUserType();
            orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
          } else {
            username = ctx.sessionAttribute("username");
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

  public Handler formGet =
      ctx -> {
        String username;
        String orgName;
        UserType userType;
        JSONObject req = new JSONObject(ctx.body());
        User check = userCheck(ctx.body());
        if (check == null && req.has("targetUser")) {
          log.info("Target form not Found");
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          boolean orgFlag;
          if (check != null && req.has("targetUser")) {
            log.info("Target form found");
            username = check.getUsername();
            orgName = check.getOrganization();
            userType = check.getUserType();
            orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
          } else {
            username = ctx.sessionAttribute("username");
            orgName = ctx.sessionAttribute("orgName");
            userType = ctx.sessionAttribute("privilegeLevel");
            orgFlag = true;
          }

          if (orgFlag) {
            String fileIDStr = req.getString("fileId");
            String isTemplateString = req.getString("isTemplate");
            GetFormService getFormService =
                new GetFormService(
                    formDao,
                    new ObjectId(fileIDStr),
                    username,
                    userType,
                    Boolean.valueOf(isTemplateString));
            Message response = getFormService.executeAndGetResponse();
            if (response == FormMessage.SUCCESS) {
              JSONObject result = getFormService.getJsonInformation();
              ctx.header("Content-Type", "application/form");
              ctx.result(result.toString());
            } else {
              ctx.result(response.toResponseString());
            }
          } else {
            ctx.result(UserMessage.CROSS_ORG_ACTION_DENIED.toResponseString());
          }
        }
      };

  public Handler formUpload =
      ctx -> {
        log.info("formUpload");
        String username;
        String organizationName;
        UserType privilegeLevel;
        Message response;
        JSONObject req;
        JSONObject form;
        String body = ctx.body();
        try {
          req = new JSONObject(body);
          form = (JSONObject) req.get("form");
        } catch (Exception e) {
          req = null;
          form = null;
        }
        if (req != null) {
          User check = userCheck(body);
          if (req != null && req.has("targetUser") && check == null) {
            log.info("Target User could not be found in the database");
            response = UserMessage.USER_NOT_FOUND;
          } else {
            boolean orgFlag;
            if (req != null && req.has("targetUser") && check != null) {
              log.info("Target User found, setting parameters.");
              username = check.getUsername();
              organizationName = check.getOrganization();
              privilegeLevel = check.getUserType();
              orgFlag = organizationName.equals(ctx.sessionAttribute("orgName"));
            } else {
              username = ctx.sessionAttribute("username");
              privilegeLevel = ctx.sessionAttribute("privilegeLevel");
              orgFlag = true;
            }
            if (orgFlag) {
              if (form == null) {
                log.info("File is null, invalid pdf");
                response = FormMessage.INVALID_FORM;
              } else {
                UploadFormService uploadService =
                    new UploadFormService(formDao, username, privilegeLevel, form);
                response = uploadService.executeAndGetResponse();
              }
            } else {
              response = UserMessage.CROSS_ORG_ACTION_DENIED;
            }
          }
        } else {
          response = FormMessage.INVALID_FORM;
        }

        ctx.result(response.toResponseString());
      };

    public Handler formMail =
            ctx -> {
                String username;
                UserType userType;
                JSONObject req = new JSONObject(ctx.body());
                User check = userCheck(ctx.body());
                if (check == null && req.has("targetUser")) {
                    ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
                } else {
                    boolean orgFlag;
                    if (check != null && req.has("targetUser")) {
                        log.info("Target form found");
                        username = check.getUsername();
                        userType = check.getUserType();
                        orgFlag = check.getOrganization().equals(ctx.sessionAttribute("orgName"));
                    } else {
                        username = ctx.sessionAttribute("username");
                        userType = ctx.sessionAttribute("privilegeLevel");
                        orgFlag = true;
                    }

                    if (orgFlag) {
                        String formIDStr = req.getString("formId");
                        ObjectId formId = new ObjectId(formIDStr);

                        MailFormService mailFormService =
                                new MailFormService(formDao, formId, username, userType);
                        ctx.result(mailFormService.executeAndGetResponse().toResponseString());
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
