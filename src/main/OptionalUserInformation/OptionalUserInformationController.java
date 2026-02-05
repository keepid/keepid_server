package OptionalUserInformation;

import static User.UserController.mergeJSON;

import Config.Message;
import Database.Activity.ActivityDao;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import OptionalUserInformation.Services.CreateOptionalInfoService;
import OptionalUserInformation.Services.DeleteOptionalInfoService;
import OptionalUserInformation.Services.GetOptionalInfoService;
import OptionalUserInformation.Services.UpdateOptionalInfoService;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class OptionalUserInformationController {
  private OptionalUserInformationDao optInfoDao;
  private ActivityDao activityDao;

  public OptionalUserInformationController(
      OptionalUserInformationDao optInfoDao, ActivityDao activityDao) {
    this.optInfoDao = optInfoDao;
    this.activityDao = activityDao;
  }

  public Handler updateInformation =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        CreateOptionalInfoService createOptionalInfoService =
            OptionalUserInformationRequestParser.parseAndCreate(optInfoDao, req);
        OptionalUserInformation optionalUserInformation = createOptionalInfoService.build();
        UpdateOptionalInfoService updateOptionalInfoService =
            new UpdateOptionalInfoService(optInfoDao, activityDao, optionalUserInformation);
        Message response = updateOptionalInfoService.executeAndGetResponse();
        ctx.result(response.toJSON().toString());
      };

  public Handler getInformation =
      ctx -> {
        GetOptionalInfoService getOptionalInfoService =
            new GetOptionalInfoService(optInfoDao, new String(ctx.pathParam("username")));
        Message response = getOptionalInfoService.executeAndGetResponse();
        if (response != UserMessage.SUCCESS) {
          ctx.result(response.toJSON().toString());
        } else {
          JSONObject result = getOptionalInfoService.getOptionalInformationFields();
          JSONObject mergedInfo = mergeJSON(response.toJSON(), result);
          ctx.result(mergedInfo.toString());
        }
      };

  public Handler saveInformation =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        CreateOptionalInfoService createOptionalInfoService =
            OptionalUserInformationRequestParser.parseAndCreate(optInfoDao, req);
        Message response = createOptionalInfoService.executeAndGetResponse();
        ctx.result(response.toJSON().toString());
      };

  public Handler deleteInformation =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        DeleteOptionalInfoService deleteOptionalInfoService =
            new DeleteOptionalInfoService(optInfoDao, req.get("username").toString());
        Message response = deleteOptionalInfoService.executeAndGetResponse();
        ctx.result(response.toJSON().toString());
      };
}
