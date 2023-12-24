package User;

import Config.Message;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import Database.User.UserDao;
import Security.SecurityUtils;
import User.Services.CreateOptionalInfoService;
import User.Services.DeleteOptionalInfoService;
import User.Services.GetOptionalInfoService;
import User.Services.UpdateOptionalInfoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.checkerframework.checker.nullness.Opt;
import org.json.JSONObject;

import java.util.Date;
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

  public Handler updateInformation =
      ctx -> {
          JSONObject req = new JSONObject(ctx.body());
          ObjectMapper objectMapper = new ObjectMapper();
          CreateOptionalInfoService createOptionalInfoService = new CreateOptionalInfoService(
                  optInfoDao,
                  req.getString("username"),
                  // Parameters for Person
                  req.getString("firstName"),
                  req.getString("middleName"),
                  req.getString("lastName"),
                  req.getString("ssn"),
                  objectMapper.readValue(req.getString("birthDate"), Date.class),
                  // Parameters for BasicInfo
                  req.getString("genderAssignedAtBirth"),
                  req.getString("emailAddress"),
                  req.getString("phoneNumber"),
                  objectMapper.readValue(req.getString("mailingAddress"), Address.class),
                  objectMapper.readValue(req.getString("residentialAddress"), Address.class),
                  req.getBoolean("differentBirthName"),
                  req.getString("suffix"),
                  req.getString("birthFirstName"),
                  req.getString("birthMiddleName"),
                  req.getString("birthLastName"),
                  req.getString("birthSuffix"),
                  req.getString("stateIdNumber"),
                  req.getBoolean("haveDisability"),
                  // Parameters for DemographicInfo
                  req.getString("languagePreference"),
                  req.getBoolean("isEthnicityHispanicLatino"),
                  Race.valueOf(req.getString("race")),
                  req.getString("cityOfBirth"),
                  req.getString("stateOfBirth"),
                  req.getString("countryOfBirth"),
                  Citizenship.valueOf(req.getString("citizenship")),
                  // Parameters for FamilyInfo
                  objectMapper.readValue(req.getString("parents"), new TypeReference<List<Person>>() {}),
                  objectMapper.readValue(req.getString("legalGuardians"), new TypeReference<List<Person>>() {}),
                  objectMapper.readValue(req.getString("maritalStatus"), MaritalStatus.class),
                  objectMapper.readValue(req.getString("spouse"), Person.class),
                  objectMapper.readValue(req.getString("children"), new TypeReference<List<Person>>() {}),
                  objectMapper.readValue(req.getString("siblings"), new TypeReference<List<Person>>() {}),


                  // Parameters for VeteranStatus
                  req.getBoolean("isVeteran"),
                  req.getBoolean("isProtectedVeteran"),
                  req.getString("branch"),
                  req.getString("yearsOfService"),
                  req.getString("rank"),
                  req.getString("discharge")
          );
          OptionalUserInformation optionalUserInformation = createOptionalInfoService.build();
          UpdateOptionalInfoService updateOptionalInfoService = new UpdateOptionalInfoService(optInfoDao, optionalUserInformation);
          Message response = updateOptionalInfoService.executeAndGetResponse();
          ctx.result(response.toJSON().toString());
      };

  public Handler getInformation =
      ctx -> {
        GetOptionalInfoService getOptionalInfoService = new GetOptionalInfoService(optInfoDao,
                new String(ctx.pathParam("username")));
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
                      optInfoDao,
                      req.getString("username"),
                      // Parameters for Person
                      req.getString("firstName"),
                      req.getString("middleName"),
                      req.getString("lastName"),
                      req.getString("ssn"),
                      objectMapper.readValue(req.getString("birthDate"), Date.class),
                      // Parameters for BasicInfo
                      req.getString("genderAssignedAtBirth"),
                      req.getString("emailAddress"),
                      req.getString("phoneNumber"),
                      objectMapper.readValue(req.getString("mailingAddress"), Address.class),
                      objectMapper.readValue(req.getString("residentialAddress"), Address.class),
                      req.getBoolean("differentBirthName"),
                      req.getString("suffix"),
                      req.getString("birthFirstName"),
                      req.getString("birthMiddleName"),
                      req.getString("birthLastName"),
                      req.getString("birthSuffix"),
                      req.getString("stateIdNumber"),
                      req.getBoolean("haveDisability"),
                      // Parameters for DemographicInfo
                      req.getString("languagePreference"),
                      req.getBoolean("isEthnicityHispanicLatino"),
                      Race.valueOf(req.getString("race")),
                      req.getString("cityOfBirth"),
                      req.getString("stateOfBirth"),
                      req.getString("countryOfBirth"),
                      Citizenship.valueOf(req.getString("citizenship")),
                      // Parameters for FamilyInfo
                      objectMapper.readValue(req.getString("parents"), new TypeReference<List<Person>>() {}),
                      objectMapper.readValue(req.getString("legalGuardians"), new TypeReference<List<Person>>() {}),
                      objectMapper.readValue(req.getString("maritalStatus"), MaritalStatus.class),
                      objectMapper.readValue(req.getString("spouse"), Person.class),
                      objectMapper.readValue(req.getString("children"), new TypeReference<List<Person>>() {}),
                      objectMapper.readValue(req.getString("siblings"), new TypeReference<List<Person>>() {}),


                      // Parameters for VeteranStatus
                      req.getBoolean("isVeteran"),
                      req.getBoolean("isProtectedVeteran"),
                      req.getString("branch"),
                      req.getString("yearsOfService"),
                      req.getString("rank"),
                      req.getString("discharge")
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
