package User;

import Config.Message;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import Database.User.UserDao;
import Security.SecurityUtils;
import User.Services.CreateOptionalInfoService;
import User.Services.DeleteOptionalInfoService;
import User.Services.GetOptionalInfoService;
import User.Services.UpdateOptionalInfoService;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.checkerframework.checker.nullness.Opt;
import org.json.JSONObject;

import java.util.List;
import java.util.Optional;

import static User.UserController.mergeJSON;

@Slf4j
public class OptionalUserInformationController {

  private UserDao userDao;
  private OptionalUserInformationDao optInfoDao;
  public OptionalUserInformationController(UserDao userDao) {
    this.userDao = userDao;
  }

  public Handler signup =
      ctx -> {
        User payload = ctx.bodyAsClass(User.class);
        String hash = SecurityUtils.hashPassword(payload.getPassword());
        if (hash == null) {
          log.error("Could not hash password");
          ctx.result(UserMessage.HASH_FAILURE.toResponseString());
        }
        verifyBaseUser(payload);
        payload.setPassword(hash);
        userDao.save(payload);
        ctx.result(UserMessage.SUCCESS.toResponseString());
      };

  public Handler changeInformation =
      ctx -> {
          JSONObject req = new JSONObject(ctx.body());
          ObjectMapper objectMapper = new ObjectMapper();
          UpdateOptionalInfoService updateOptionalInfoService = new UpdateOptionalInfoService(
                  objectMapper,
                  optInfoDao,
                  req.get("optionalUserInformation").toString()
          );
          Message response = updateOptionalInfoService.executeAndGetResponse();
          ctx.result(response.toJSON().toString());
      };

  public Handler getInformation =
      ctx -> {
        GetOptionalInfoService getOptionalInfoService = new GetOptionalInfoService(optInfoDao,
                new ObjectId(ctx.pathParam("id")));
        Message response = getOptionalInfoService.executeAndGetResponse();
        if(response != UserMessage.SUCCESS){
            ctx.result(response.toJSON().toString());
        } else{
            JSONObject result = getOptionalInfoService.getOptionalInformationFields();
            JSONObject mergedInfo = mergeJSON(response.toJSON(), result);
            ctx.result(mergedInfo.toString());
        }
      };


  public Handler saveInformation =
          ctx -> {
            JSONObject req = new JSONObject(ctx.body());
            ObjectMapper objectMapper = new ObjectMapper();
            CreateOptionalInfoService createOptionalInfoService = new CreateOptionalInfoService(
                    objectMapper,
                    optInfoDao,
                    req.get("optionalUserInformation").toString()
            );
            Message response = createOptionalInfoService.executeAndGetResponse();
            ctx.result(response.toJSON().toString());
          };

  public Handler deleteInformation =
          ctx -> {
              JSONObject req = new JSONObject(ctx.body());
              DeleteOptionalInfoService deleteOptionalInfoService = new DeleteOptionalInfoService(
                      optInfoDao,
                      req.get("username").toString()
              );
              Message response = deleteOptionalInfoService.executeAndGetResponse();
              ctx.result(response.toJSON().toString());
          };


  private void verifyBaseUser(User user) throws Exception {
    if (user.getSelf() == null) {
      throw new Exception();
    }
    if (user.getPassword() == null) {
      throw new Exception();
    }
    if (user.getPassword() != null && user.getPassword().equals("")) {
      throw new Exception();
    }
  }
}
