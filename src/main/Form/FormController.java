package Form;

import Config.Message;
import Database.Form.FormDao;
import Database.User.UserDao;
import Form.Services.DeleteFormService;
import Form.Services.GetFormService;
import Form.Services.UploadFormService;
import Security.EncryptionController;
import User.User;
import User.UserMessage;
import User.UserType;
import io.javalin.http.Handler;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;

@Slf4j
public class FormController {
  private FormDao formDao;
  private EncryptionController encryptionController;
  private UserDao userDao;

  public FormController(
      FormDao formDao, UserDao userDao, EncryptionController encryptionController) {
    this.formDao = formDao;
    this.userDao = userDao;
    this.encryptionController = encryptionController;
  }

  //  public Handler formTest =
  //      ctx -> {
  //        ObjectId formId = new ObjectId("6679fc62948ca978b0d31825");
  //        String username = "SAMPLE-CLIENT";
  //        GetFormService formService =
  //            new GetFormService(formDao, formId, username, UserType.Client, false);
  //        formService.executeAndGetResponse();
  //        System.out.println(formService.getJsonInformation());
  //      };

  public Handler formDelete =
      ctx -> {
        String username;
        String orgName;
        UserType userType;
        JSONObject req = new JSONObject(ctx.body());
        Optional<User> targetUser = userCheck(ctx.body());
        if (targetUser.isEmpty() && req.has("targetUser")) {
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          boolean orgFlag;
          if (targetUser.isPresent() && req.has("targetUser")) {
            log.info("Target form found");
            username = targetUser.get().getUsername();
            orgName = targetUser.get().getOrganization();
            userType = targetUser.get().getUserType();
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
        Optional<User> targetUser = userCheck(ctx.body());
        if (targetUser.isEmpty() && req.has("targetUser")) {
          log.info("Target form not Found");
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          boolean orgFlag;
          if (targetUser.isPresent() && req.has("targetUser")) {
            log.info("Target form found");
            username = targetUser.get().getUsername();
            orgName = targetUser.get().getOrganization();
            userType = targetUser.get().getUserType();
            orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
          } else {
            username = ctx.sessionAttribute("username");
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
          Optional<User> check = userCheck(body);
          if (req != null && req.has("targetUser") && check.isEmpty()) {
            log.info("Target User could not be found in the database");
            response = UserMessage.USER_NOT_FOUND;
          } else {
            boolean orgFlag;
            if (req != null && req.has("targetUser") && check.isPresent()) {
              log.info("Target User found, setting parameters.");
              username = check.get().getUsername();
              organizationName = check.get().getOrganization();
              privilegeLevel = check.get().getUserType();
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

  public Optional<User> userCheck(String req) {
    try {
      JSONObject reqJson = new JSONObject(req);
      if (reqJson.has("targetUser")) {
        return this.userDao.get(reqJson.getString("targetUser"));
      }
    } catch (JSONException e) {
      System.out.println(e);
    }
    return Optional.empty();
  }
}
