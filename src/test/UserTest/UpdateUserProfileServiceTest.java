package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import OptionalUserInformation.*;
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
    User user = EntityFactory.createUser()
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
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    JSONObject person = new JSONObject();
    person.put("firstName", "John");
    person.put("lastName", "Doe");
    person.put("middleName", "Middle");
    updateRequest.put("optionalInformation", new JSONObject());
    updateRequest.getJSONObject("optionalInformation").put("person", person);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    
    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getOptionalInformation());
    assertNotNull(updatedUser.getOptionalInformation().getPerson());
    assertEquals("John", updatedUser.getOptionalInformation().getPerson().getFirstName());
    assertEquals("Doe", updatedUser.getOptionalInformation().getPerson().getLastName());
    assertEquals("Middle", updatedUser.getOptionalInformation().getPerson().getMiddleName());
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
    existingPerson.setFirstName("Jane");
    existingPerson.setLastName("Smith");
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
    assertEquals("Jane", updatedUser.getOptionalInformation().getPerson().getFirstName());
    assertEquals("Smith", updatedUser.getOptionalInformation().getPerson().getLastName());
    assertEquals("123-45-6789", updatedUser.getOptionalInformation().getPerson().getSsn());
    // Should add new field
    assertEquals("M", updatedUser.getOptionalInformation().getPerson().getMiddleName());
    // Should preserve other nested objects
    assertNotNull(updatedUser.getOptionalInformation().getBasicInfo());
    assertEquals("F", updatedUser.getOptionalInformation().getBasicInfo().getGenderAssignedAtBirth());
  }

  @Test
  public void validateEmailFormat() {
    User user = EntityFactory.createUser()
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
    User user = EntityFactory.createUser()
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
    assertEquals("123 Main St", updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress().getStreetAddress());
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
    assertEquals("Jane", updatedUser.getOptionalInformation().getPerson().getFirstName());
    assertNull(updatedUser.getOptionalInformation().getPerson().getMiddleName());
    assertEquals("Smith", updatedUser.getOptionalInformation().getPerson().getLastName());
  }
}
