package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import User.Address;
import User.Name;
import User.Services.UpdateUserProfileService;
import User.User;
import User.UserMessage;
import User.UserType;
import Validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

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
  public void updateEmail() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .withEmail("old@example.com")
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    updateRequest.put("email", "new@example.com");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertEquals("new@example.com", updatedUser.getEmail());
  }

  @Test
  public void updateCurrentName() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .withFirstName("OldFirst")
        .withLastName("OldLast")
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    JSONObject nameJson = new JSONObject();
    nameJson.put("first", "NewFirst");
    nameJson.put("last", "NewLast");
    nameJson.put("middle", "M");
    nameJson.put("suffix", "Jr");
    updateRequest.put("currentName", nameJson);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertEquals("NewFirst", updatedUser.getCurrentName().getFirst());
    assertEquals("NewLast", updatedUser.getCurrentName().getLast());
    assertEquals("M", updatedUser.getCurrentName().getMiddle());
    assertEquals("Jr", updatedUser.getCurrentName().getSuffix());
  }

  @Test
  public void updatePersonalAddress() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    JSONObject addressJson = new JSONObject();
    addressJson.put("line1", "456 New St");
    addressJson.put("city", "New York");
    addressJson.put("state", "NY");
    addressJson.put("zip", "10001");
    updateRequest.put("personalAddress", addressJson);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getPersonalAddress());
    assertEquals("456 New St", updatedUser.getPersonalAddress().getLine1());
    assertEquals("New York", updatedUser.getPersonalAddress().getCity());
    assertEquals("NY", updatedUser.getPersonalAddress().getState());
    assertEquals("10001", updatedUser.getPersonalAddress().getZip());
  }

  @Test
  public void updateMailAddress() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    JSONObject addressJson = new JSONObject();
    addressJson.put("line1", "789 Mail Ave");
    addressJson.put("city", "Chicago");
    addressJson.put("state", "IL");
    addressJson.put("zip", "60601");
    updateRequest.put("mailAddress", addressJson);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getMailAddress());
    assertEquals("789 Mail Ave", updatedUser.getMailAddress().getLine1());
    assertEquals("Chicago", updatedUser.getMailAddress().getCity());
  }

  @Test
  public void updateSex() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    updateRequest.put("sex", "Male");

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertEquals("Male", updatedUser.getSex());
  }

  @Test
  public void updateMotherName() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    JSONObject motherJson = new JSONObject();
    motherJson.put("first", "Mary");
    motherJson.put("last", "Smith");
    motherJson.put("maiden", "Jones");
    updateRequest.put("motherName", motherJson);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getMotherName());
    assertEquals("Mary", updatedUser.getMotherName().getFirst());
    assertEquals("Smith", updatedUser.getMotherName().getLast());
    assertEquals("Jones", updatedUser.getMotherName().getMaiden());
  }

  @Test
  public void updateFatherName() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    JSONObject fatherJson = new JSONObject();
    fatherJson.put("first", "John");
    fatherJson.put("last", "Smith");
    updateRequest.put("fatherName", fatherJson);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getFatherName());
    assertEquals("John", updatedUser.getFatherName().getFirst());
    assertEquals("Smith", updatedUser.getFatherName().getLast());
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
  public void updateNameHistory() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject updateRequest = new JSONObject();
    org.json.JSONArray historyArray = new org.json.JSONArray();
    JSONObject prevName = new JSONObject();
    prevName.put("first", "PrevFirst");
    prevName.put("last", "PrevLast");
    prevName.put("maiden", "PrevMaiden");
    historyArray.put(prevName);
    updateRequest.put("nameHistory", historyArray);

    UpdateUserProfileService service = new UpdateUserProfileService(userDao, "testuser", updateRequest);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    User updatedUser = userDao.get("testuser").orElse(null);
    assertNotNull(updatedUser);
    assertNotNull(updatedUser.getNameHistory());
    assertEquals(1, updatedUser.getNameHistory().size());
    assertEquals("PrevFirst", updatedUser.getNameHistory().get(0).getFirst());
    assertEquals("PrevMaiden", updatedUser.getNameHistory().get(0).getMaiden());
  }
}
