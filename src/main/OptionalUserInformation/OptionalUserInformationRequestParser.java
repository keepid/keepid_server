package OptionalUserInformation;

import Database.OptionalUserInformation.OptionalUserInformationDao;
import OptionalUserInformation.Services.CreateOptionalInfoService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.SimpleDateFormat;
import java.util.List;
import org.json.JSONObject;

/**
 * Parses optional user information from JSON request body.
 * Extracted to avoid VerifyError from type erasure when parsing in controller lambdas.
 */
public class OptionalUserInformationRequestParser {

  public static CreateOptionalInfoService parseAndCreate(
      OptionalUserInformationDao optInfoDao,
      JSONObject req)
      throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    Address mailingAddress =
        objectMapper.readValue(
            req.getJSONObject("mailingAddress").toString(), Address.class);
    Address residentialAddress =
        objectMapper.readValue(
            req.getJSONObject("residentialAddress").toString(), Address.class);
    Race race =
        req.getString("race").equals("")
            ? Race.UNSELECTED
            : Race.valueOf(req.getString("race"));
    Citizenship citizenship =
        req.getString("citizenship").equals("")
            ? Citizenship.UNSELECTED
            : Citizenship.valueOf(req.getString("citizenship"));
    List<Person> parents =
        objectMapper.readValue(
            req.getJSONArray("parents").toString(), new TypeReference<List<Person>>() {});
    List<Person> legalGuardians =
        objectMapper.readValue(
            req.getJSONArray("legalGuardians").toString(),
            new TypeReference<List<Person>>() {});
    MaritalStatus maritalStatus =
        req.getString("maritalStatus").equals("")
            ? MaritalStatus.UNSELECTED
            : MaritalStatus.valueOf(req.getString("maritalStatus"));
    Person spouse =
        objectMapper.readValue(req.getJSONObject("spouse").toString(), Person.class);
    List<Person> children =
        objectMapper.readValue(
            req.getJSONArray("children").toString(), new TypeReference<List<Person>>() {});
    List<Person> siblings =
        objectMapper.readValue(
            req.getJSONArray("siblings").toString(), new TypeReference<List<Person>>() {});

    return new CreateOptionalInfoService(
        optInfoDao,
        req.getString("username"),
        req.getString("firstName"),
        req.getString("middleName"),
        req.getString("lastName"),
        req.getString("ssn"),
        new SimpleDateFormat("yyyy-MM-dd").parse(req.getString("birthDate")),
        req.getString("genderAssignedAtBirth"),
        req.getString("emailAddress"),
        req.getString("phoneNumber"),
        mailingAddress,
        residentialAddress,
        req.getBoolean("differentBirthName"),
        req.getString("suffix"),
        req.getString("birthFirstName"),
        req.getString("birthMiddleName"),
        req.getString("birthLastName"),
        req.getString("birthSuffix"),
        req.getString("stateIdNumber"),
        req.getBoolean("haveDisability"),
        req.getString("languagePreference"),
        req.getBoolean("isEthnicityHispanicLatino"),
        race,
        req.getString("cityOfBirth"),
        req.getString("stateOfBirth"),
        req.getString("countryOfBirth"),
        citizenship,
        parents,
        legalGuardians,
        maritalStatus,
        spouse,
        children,
        siblings,
        req.getBoolean("isVeteran"),
        req.getBoolean("isProtectedVeteran"),
        req.getString("branch"),
        req.getString("yearsOfService"),
        req.getString("rank"),
        req.getString("discharge"));
  }
}
