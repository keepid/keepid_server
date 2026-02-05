package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import User.UserInformation.*;
import TestUtils.EntityFactory;
import User.OptionalInformation;
import User.Services.UpdateUserProfileService;
import User.User;
import User.UserMessage;
import User.UserType;
import Validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class UpdateUserProfileServiceTest {
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);

  @After
  public void reset() {
    if (userDao != null) {
      userDao.clear();
    }
  }

  @Test
  public void updateRootLevelFields() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .withEmail("old@example.com")
        .withPhoneNumber("1234567890")
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    updateRequest.put("email", "new@example.com");
    updateRequest.put("phone", "9876543210");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertEquals("new@example.com", updatedUser.getEmail());
    assertEquals("9876543210", updatedUser.getPhone());
  }

  @Test
  public void updateNestedOptionalInformationFields() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    JSONObject person = new JSONObject();
    // firstName/lastName are ignored - they come from root level User fields
    person.put("middleName", "Middle");
    person.put("ssn", "123-45-6789");
    updateRequest.put("optionalInformation", new JSONObject());
    updateRequest.getJSONObject("optionalInformation").put("person", person);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getOptionalInformation());
    assertNotNull(updatedUser.getOptionalInformation().getPerson());
    // firstName/lastName should remain null in Person (they come from root level)
    assertNull(updatedUser.getOptionalInformation().getPerson().getFirstName());
    assertNull(updatedUser.getOptionalInformation().getPerson().getLastName());
    assertEquals("Middle", updatedUser.getOptionalInformation().getPerson().getMiddleName());
    assertEquals("123-45-6789", updatedUser.getOptionalInformation().getPerson().getSsn());
  }

  @Test
  public void mergeNestedObjectsDoesNotReplaceEntireObject() {
    // Create user with existing optionalInformation
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .build();

    OptionalInformation existingInfo = new OptionalInformation();
    Person existingPerson = new Person();
    // firstName/lastName should be null for user's own Person (they come from root
    // level)
    existingPerson.setMiddleName("M");
    existingPerson.setSsn("123-45-6789");
    existingInfo.setPerson(existingPerson);

    BasicInfo existingBasicInfo = new BasicInfo();
    existingBasicInfo.setGenderAssignedAtBirth("F");
    existingBasicInfo.setEmailAddress("jane@example.com");
    existingInfo.setBasicInfo(existingBasicInfo);

    user.setOptionalInformation(existingInfo);
    userDao.save(user);

    // Update only middleName in person, should preserve other fields
    JSONObject updateRequest = new JSONObject();
    JSONObject optionalInfo = new JSONObject();
    JSONObject person = new JSONObject();
    person.put("middleName", "M");
    optionalInfo.put("person", person);
    updateRequest.put("optionalInformation", optionalInfo);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getOptionalInformation());
    assertNotNull(updatedUser.getOptionalInformation().getPerson());
    // Should preserve existing fields
    assertEquals("123-45-6789", updatedUser.getOptionalInformation().getPerson().getSsn());
    // Should update middleName (firstName/lastName ignored - they come from root
    // level)
    assertEquals("M", updatedUser.getOptionalInformation().getPerson().getMiddleName());
    // Should preserve other nested objects
    assertNotNull(updatedUser.getOptionalInformation().getBasicInfo());
    assertEquals("F", updatedUser.getOptionalInformation().getBasicInfo().getGenderAssignedAtBirth());
  }

  @Test
  public void validateEmailFormat() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    updateRequest.put("email", "invalid-email");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertTrue(response instanceof ValidationException);
    JSONObject responseJSON = response.toJSON();
    assertEquals("INVALID_PARAMETER", responseJSON.getString("status"));
    assertEquals("Invalid Email", responseJSON.getString("message"));
  }

  @Test
  public void validatePhoneNumberFormat() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    updateRequest.put("phone", "invalid-phone");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertTrue(response instanceof ValidationException);
    JSONObject responseJSON = response.toJSON();
    assertEquals("INVALID_PARAMETER", responseJSON.getString("status"));
    assertEquals("Invalid Phone", responseJSON.getString("message"));
  }

  @Test
  public void updateNestedAddressFields() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    JSONObject optionalInfo = new JSONObject();
    JSONObject basicInfo = new JSONObject();
    JSONObject mailingAddress = new JSONObject();
    mailingAddress.put("streetAddress", "123 Main St");
    mailingAddress.put("city", "Philadelphia");
    mailingAddress.put("state", "PA");
    mailingAddress.put("zip", "19104");
    basicInfo.put("mailingAddress", mailingAddress);
    optionalInfo.put("basicInfo", basicInfo);
    updateRequest.put("optionalInformation", optionalInfo);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getOptionalInformation());
    assertNotNull(updatedUser.getOptionalInformation().getBasicInfo());
    assertNotNull(updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress());
    assertEquals("123 Main St",
        updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress().getStreetAddress());
    assertEquals("Philadelphia", updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress().getCity());
    assertEquals("PA", updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress().getState());
    assertEquals("19104", updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress().getZip());
  }

  @Test
  public void userNotFoundReturnsError() {
    JSONObject updateRequest = new JSONObject();
    updateRequest.put("email", "test@example.com");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "nonexistent", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.USER_NOT_FOUND, response);
  }

  @Test
  public void firstNameLastNameInPersonAreNotMutable() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .withFirstName("RootFirst")
        .withLastName("RootLast")
        .buildAndPersist(userDao);

    // Try to update firstName/lastName through optionalInformation.person
    // These should be ignored - they come from root level User fields
    JSONObject updateRequest = new JSONObject();
    JSONObject optionalInfo = new JSONObject();
    JSONObject person = new JSONObject();
    person.put("firstName", "PersonFirst"); // Should be ignored
    person.put("lastName", "PersonLast"); // Should be ignored
    person.put("middleName", "Middle");
    optionalInfo.put("person", person);
    updateRequest.put("optionalInformation", optionalInfo);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    // Root level names should remain unchanged
    assertEquals("RootFirst", updatedUser.getFirstName());
    assertEquals("RootLast", updatedUser.getLastName());

    // Person.firstName/lastName should remain null (not updated)
    assertNotNull(updatedUser.getOptionalInformation());
    assertNotNull(updatedUser.getOptionalInformation().getPerson());
    assertNull(updatedUser.getOptionalInformation().getPerson().getFirstName());
    assertNull(updatedUser.getOptionalInformation().getPerson().getLastName());
    // middleName should be updated
    assertEquals("Middle", updatedUser.getOptionalInformation().getPerson().getMiddleName());
  }

  @Test
  public void updateIndividualFieldWithDotNotation() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    // Update individual demographic field using dot notation
    JSONObject updateRequest = new JSONObject();
    updateRequest.put("optionalInformation.demographicInfo.languagePreference", "Spanish");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getOptionalInformation());
    assertNotNull(updatedUser.getOptionalInformation().getDemographicInfo());
    assertEquals("Spanish", updatedUser.getOptionalInformation().getDemographicInfo().getLanguagePreference());
  }

  @Test
  public void updateMultipleFieldsWithDotNotation() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    // Update multiple individual fields using dot notation
    JSONObject updateRequest = new JSONObject();
    updateRequest.put("optionalInformation.demographicInfo.languagePreference", "French");
    updateRequest.put("optionalInformation.demographicInfo.cityOfBirth", "New York");
    updateRequest.put("optionalInformation.person.middleName", "Middle");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getOptionalInformation());
    assertNotNull(updatedUser.getOptionalInformation().getDemographicInfo());
    assertEquals("French", updatedUser.getOptionalInformation().getDemographicInfo().getLanguagePreference());
    assertEquals("New York", updatedUser.getOptionalInformation().getDemographicInfo().getCityOfBirth());
    assertNotNull(updatedUser.getOptionalInformation().getPerson());
    assertEquals("Middle", updatedUser.getOptionalInformation().getPerson().getMiddleName());
  }

  @Test
  public void updateDemographicInfoNestedObjectWithRaceAndCitizenshipEnumSuccess() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject demographicInfo = new JSONObject();
    demographicInfo.put("languagePreference", "English");
    demographicInfo.put("race", "ASIAN");
    demographicInfo.put("citizenship", "US_CITIZEN");
    JSONObject optionalInformation = new JSONObject();
    optionalInformation.put("demographicInfo", demographicInfo);
    JSONObject updateRequest = new JSONObject();
    updateRequest.put("optionalInformation", optionalInformation);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getOptionalInformation());
    assertNotNull(updatedUser.getOptionalInformation().getDemographicInfo());
    assertEquals("English", updatedUser.getOptionalInformation().getDemographicInfo().getLanguagePreference());
    assertEquals(Race.ASIAN, updatedUser.getOptionalInformation().getDemographicInfo().getRace());
    assertEquals(Citizenship.US_CITIZEN, updatedUser.getOptionalInformation().getDemographicInfo().getCitizenship());
  }

  @Test
  public void updateDemographicInfoInvalidRaceEnumReturnsError() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    updateRequest.put("optionalInformation.demographicInfo.race", "INVALID_RACE_VALUE");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertTrue(response instanceof ValidationException);
    JSONObject responseJSON = response.toJSON();
    assertEquals("INVALID_PARAMETER", responseJSON.getString("status"));
  }

  @Test
  public void updateNestedAddressFieldWithDotNotation() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .build();

    OptionalInformation optionalInfo = new OptionalInformation();
    BasicInfo basicInfo = new BasicInfo();
    Address mailingAddress = new Address();
    mailingAddress.setStreetAddress("123 Main St");
    mailingAddress.setCity("Philadelphia");
    mailingAddress.setState("PA");
    mailingAddress.setZip("19104");
    basicInfo.setMailingAddress(mailingAddress);
    optionalInfo.setBasicInfo(basicInfo);
    user.setOptionalInformation(optionalInfo);
    userDao.save(user);

    // Update only the city in mailingAddress using dot notation
    JSONObject updateRequest = new JSONObject();
    updateRequest.put("optionalInformation.basicInfo.mailingAddress.city", "New York");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getOptionalInformation());
    assertNotNull(updatedUser.getOptionalInformation().getBasicInfo());
    assertNotNull(updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress());
    assertEquals("New York", updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress().getCity());
    // Other fields should be preserved
    assertEquals("123 Main St",
        updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress().getStreetAddress());
    assertEquals("PA", updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress().getState());
  }

  @Test
  public void updateFieldWithDotNotationIgnoresFirstNameLastNameInPerson() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .withFirstName("RootFirst")
        .withLastName("RootLast")
        .buildAndPersist(userDao);

    // Try to update firstName/lastName in Person using dot notation - should be
    // ignored
    JSONObject updateRequest = new JSONObject();
    updateRequest.put("optionalInformation.person.firstName", "PersonFirst");
    updateRequest.put("optionalInformation.person.lastName", "PersonLast");
    updateRequest.put("optionalInformation.person.middleName", "Middle");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    // Root level names should remain unchanged
    assertEquals("RootFirst", updatedUser.getFirstName());
    assertEquals("RootLast", updatedUser.getLastName());

    // Person.firstName/lastName should remain null (not updated)
    assertNotNull(updatedUser.getOptionalInformation());
    assertNotNull(updatedUser.getOptionalInformation().getPerson());
    assertNull(updatedUser.getOptionalInformation().getPerson().getFirstName());
    assertNull(updatedUser.getOptionalInformation().getPerson().getLastName());
    // middleName should be updated
    assertEquals("Middle", updatedUser.getOptionalInformation().getPerson().getMiddleName());
  }

  @Test
  public void updateFieldWithDotNotationValidatesEmail() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    // Try to update email with invalid format using dot notation
    JSONObject updateRequest = new JSONObject();
    updateRequest.put("optionalInformation.basicInfo.emailAddress", "invalid-email");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    // Should return validation error (not SUCCESS)
    assertNotEquals(UserMessage.SUCCESS.toJSON().toString(), response.toJSON().toString());
  }

  @Test
  public void updateFieldWithDotNotationAndNestedObjectBackwardCompatible() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    // Mix dot notation and nested object structure (backward compatible)
    JSONObject updateRequest = new JSONObject();
    updateRequest.put("optionalInformation.demographicInfo.languagePreference", "Spanish");
    // Also include nested object structure
    JSONObject optionalInfo = new JSONObject();
    JSONObject person = new JSONObject();
    person.put("middleName", "Middle");
    optionalInfo.put("person", person);
    updateRequest.put("optionalInformation", optionalInfo);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getOptionalInformation());
    // Both updates should be applied
    assertNotNull(updatedUser.getOptionalInformation().getDemographicInfo());
    assertEquals("Spanish", updatedUser.getOptionalInformation().getDemographicInfo().getLanguagePreference());
    assertNotNull(updatedUser.getOptionalInformation().getPerson());
    assertEquals("Middle", updatedUser.getOptionalInformation().getPerson().getMiddleName());
  }

  @Test
  public void nullValuesClearFields() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .build();

    OptionalInformation existingInfo = new OptionalInformation();
    Person existingPerson = new Person();
    existingPerson.setFirstName("Jane");
    existingPerson.setMiddleName("M");
    existingPerson.setLastName("Smith");
    existingInfo.setPerson(existingPerson);
    user.setOptionalInformation(existingInfo);
    userDao.save(user);

    // Set middleName to null to clear it
    JSONObject updateRequest = new JSONObject();
    JSONObject optionalInfo = new JSONObject();
    JSONObject person = new JSONObject();
    person.put("middleName", JSONObject.NULL);
    optionalInfo.put("person", person);
    updateRequest.put("optionalInformation", optionalInfo);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getOptionalInformation());
    assertNotNull(updatedUser.getOptionalInformation().getPerson());
    // firstName/lastName come from root level, not Person
    // middleName should be cleared (set to null)
    assertNull(updatedUser.getOptionalInformation().getPerson().getMiddleName());
  }
}
