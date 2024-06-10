package OptionalUserInformation;

import Config.Message;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import OptionalUserInformation.Services.CreateOptionalInfoService;
import OptionalUserInformation.Services.DeleteOptionalInfoService;
import OptionalUserInformation.Services.GetOptionalInfoService;
import OptionalUserInformation.Services.UpdateOptionalInfoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import static User.UserController.mergeJSON;

@Slf4j
public class OptionalUserInformationController {
  private OptionalUserInformationDao optInfoDao;
  public OptionalUserInformationController(OptionalUserInformationDao optInfoDao) {
    this.optInfoDao = optInfoDao;
  }
  
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
                  objectMapper.readValue(req.getJSONObject("mailingAddress").toString(), Address.class),
                  objectMapper.readValue(req.getJSONObject("residentialAddress").toString(), Address.class),
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
                  req.getString("race").equals("") ? Race.UNSELECTED : Race.valueOf(req.getString("race")),
                  req.getString("cityOfBirth"),
                  req.getString("stateOfBirth"),
                  req.getString("countryOfBirth"),
                  req.getString("citizenship").equals("") ? Citizenship.UNSELECTED : Citizenship.valueOf(req.getString("citizenship")),
                  // Parameters for FamilyInfo
                  objectMapper.readValue(req.getJSONArray("parents").toString(), new TypeReference<List<Person>>() {}),
                  objectMapper.readValue(req.getJSONArray("legalGuardians").toString(), new TypeReference<List<Person>>() {}),
                  req.getString("maritalStatus").equals("") ? MaritalStatus.UNSELECTED : MaritalStatus.valueOf(req.getString("maritalStatus")),
                  objectMapper.readValue(req.getJSONObject("spouse").toString(), Person.class),
                  objectMapper.readValue(req.getJSONArray("children").toString(), new TypeReference<List<Person>>() {}),
                  objectMapper.readValue(req.getJSONArray("siblings").toString(), new TypeReference<List<Person>>() {}),


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
                      objectMapper.readValue(req.getJSONObject("mailingAddress").toString(), Address.class),
                      objectMapper.readValue(req.getJSONObject("residentialAddress").toString(), Address.class),
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
                      req.getString("race").equals("") ? Race.UNSELECTED : Race.valueOf(req.getString("race")),
                      req.getString("cityOfBirth"),
                      req.getString("stateOfBirth"),
                      req.getString("countryOfBirth"),
                      req.getString("citizenship").equals("") ? Citizenship.UNSELECTED : Citizenship.valueOf(req.getString("citizenship")),
                      // Parameters for FamilyInfo
                      objectMapper.readValue(req.getJSONArray("parents").toString(), new TypeReference<List<Person>>() {}),
                      objectMapper.readValue(req.getJSONArray("legalGuardians").toString(), new TypeReference<List<Person>>() {}),
                      req.getString("maritalStatus").equals("") ? MaritalStatus.UNSELECTED : MaritalStatus.valueOf(req.getString("maritalStatus")),
                      objectMapper.readValue(req.getJSONObject("spouse").toString(), Person.class),
                      objectMapper.readValue(req.getJSONArray("children").toString(), new TypeReference<List<Person>>() {}),
                      objectMapper.readValue(req.getJSONArray("siblings").toString(), new TypeReference<List<Person>>() {}),


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

}
