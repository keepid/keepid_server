package UserTest;

import static org.junit.Assert.assertTrue;

import Activity.UserActivity.ChangeUserAttributesActivity;
import Config.DeploymentLevel;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import Database.Token.TokenDao;
import Database.Token.TokenDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.User;
import java.io.IOException;
import java.security.GeneralSecurityException;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChangeAccountSettingsIntegrationTests {
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.TEST);
  ActivityDao activityDao = ActivityDaoFactory.create(DeploymentLevel.TEST);
  TokenDao tokenDao = TokenDaoFactory.create(DeploymentLevel.TEST);

  @BeforeClass
  public static void setUp() throws GeneralSecurityException, IOException {
    TestUtils.startServer();
  }

  @After
  public void reset() {
    TestUtils.logout();
    userDao.clear();
    tokenDao.clear();
    activityDao.clear();
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  // Make sure to enable .env file configurations for these tests
  private boolean isCorrectAttribute(String username, String attribute, String possibleValue) {
    User user = userDao.get(username).orElseThrow();
    switch (attribute) {
      case "firstName":
        String currentFirstName = user.getFirstName();
        return (currentFirstName.equals(possibleValue));
      case "lastName":
        String currentLastName = user.getLastName();
        return (currentLastName.equals(possibleValue));
      case "birthDate":
        String currentBirthDate = user.getBirthDate();
        return (currentBirthDate.equals(possibleValue));
      case "phone":
        String currentPhone = user.getPhone();
        return (currentPhone.equals(possibleValue));
      case "email":
        String currentEmail = user.getEmail();
        return (currentEmail.equals(possibleValue));
      case "address":
        String currentAddress = user.getAddress();
        return (currentAddress.equals(possibleValue));
      case "city":
        String currentCity = user.getCity();
        return (currentCity.equals(possibleValue));
      case "state":
        String currentState = user.getState();
        return (currentState.equals(possibleValue));
      case "zipcode":
        String currentZipcode = user.getZipcode();
        return (currentZipcode.equals(possibleValue));
      default:
        return false;
    }
  }

  @Test
  public void changeFirstNameTest() {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    User user =
        EntityFactory.createUser()
            .withUsername(username)
            .withPasswordToHash(password)
            .withFirstName("David")
            .buildAndPersist(userDao);
    String newFirstName = "Sarah";
    JSONObject requestPayload =
        new JSONObject()
            .put("password", password)
            .put("key", "firstName")
            .put("value", newFirstName);
    TestUtils.login(username, password);
    HttpResponse<String> createUserResponse =
        Unirest.post(TestUtils.getServerUrl() + "/change-account-setting")
            .body(requestPayload.toString())
            .asString();
    assertTrue(createUserResponse.getBody().contains("SUCCESS"));
    JSONObject getActivitiesPayload = new JSONObject().put("username", username);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(getActivitiesPayload.toString())
            .asString();
    assert (findResponse
        .getBody()
        .toString()
        .contains(ChangeUserAttributesActivity.class.getSimpleName()));
    assert (isCorrectAttribute(username, "firstName", newFirstName));
  }

  @Test
  public void changeLastNameTest() throws Exception {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withLastName("Smith")
        .buildAndPersist(userDao);
    String newLastName = "Jones";
    JSONObject requestPayload =
        new JSONObject().put("password", password).put("key", "lastName").put("value", newLastName);
    TestUtils.login(username, password);
    HttpResponse<String> createUserResponse =
        Unirest.post(TestUtils.getServerUrl() + "/change-account-setting")
            .body(requestPayload.toString())
            .asString();
    assertTrue(createUserResponse.getBody().contains("SUCCESS"));
    JSONObject getActivitiesPayload = new JSONObject().put("username", username);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(getActivitiesPayload.toString())
            .asString();
    assert (findResponse
        .getBody()
        .toString()
        .contains(ChangeUserAttributesActivity.class.getSimpleName()));
    assert (isCorrectAttribute(username, "lastName", newLastName));
  }

  @Test
  public void changeBirthDateTest() throws Exception {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withBirthDate("01-25-1965")
        .buildAndPersist(userDao);

    String newBirthDate = "05-23-2002";
    JSONObject requestPayload =
        new JSONObject()
            .put("password", password)
            .put("key", "birthDate")
            .put("value", newBirthDate);
    TestUtils.login(username, password);
    HttpResponse<String> createUserResponse =
        Unirest.post(TestUtils.getServerUrl() + "/change-account-setting")
            .body(requestPayload.toString())
            .asString();
    assertTrue(createUserResponse.getBody().contains("SUCCESS"));
    JSONObject getActivitiesPayload = new JSONObject().put("username", username);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(getActivitiesPayload.toString())
            .asString();
    assert (findResponse
        .getBody()
        .toString()
        .contains(ChangeUserAttributesActivity.class.getSimpleName()));
    assert (isCorrectAttribute(username, "birthDate", newBirthDate));
  }

  @Test
  public void changePhoneTest() throws Exception {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withPhoneNumber("215-123-4567")
        .buildAndPersist(userDao);
    String newPhone = "412-123-3456";
    JSONObject requestPayload =
        new JSONObject().put("password", password).put("key", "phone").put("value", newPhone);
    TestUtils.login(username, password);
    HttpResponse<String> createUserResponse =
        Unirest.post(TestUtils.getServerUrl() + "/change-account-setting")
            .body(requestPayload.toString())
            .asString();
    assertTrue(createUserResponse.getBody().contains("SUCCESS"));
    JSONObject getActivitiesPayload = new JSONObject().put("username", username);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(getActivitiesPayload.toString())
            .asString();
    assert (findResponse
        .getBody()
        .toString()
        .contains(ChangeUserAttributesActivity.class.getSimpleName()));
    assert (isCorrectAttribute(username, "phone", newPhone));
  }

  @Test
  public void changeEmailTest() throws Exception {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withEmail("contact1@example.com")
        .buildAndPersist(userDao);
    String newEmail = "contact2@example.com";
    JSONObject requestPayload =
        new JSONObject().put("password", password).put("key", "email").put("value", newEmail);
    TestUtils.login(username, password);
    HttpResponse<String> createUserResponse =
        Unirest.post(TestUtils.getServerUrl() + "/change-account-setting")
            .body(requestPayload.toString())
            .asString();
    assertTrue(createUserResponse.getBody().contains("SUCCESS"));
    JSONObject getActivitiesPayload = new JSONObject().put("username", username);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(getActivitiesPayload.toString())
            .asString();
    assert (findResponse
        .getBody()
        .toString()
        .contains(ChangeUserAttributesActivity.class.getSimpleName()));
    assert (isCorrectAttribute(username, "email", newEmail));
  }

  @Test
  public void changeAddressTest() throws Exception {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withAddress("123 SampleStreet")
        .buildAndPersist(userDao);

    String newAddress = "321 RandomStreet";
    JSONObject requestPayload =
        new JSONObject().put("password", password).put("key", "address").put("value", newAddress);
    TestUtils.login(username, password);
    HttpResponse<String> createUserResponse =
        Unirest.post(TestUtils.getServerUrl() + "/change-account-setting")
            .body(requestPayload.toString())
            .asString();
    assertTrue(createUserResponse.getBody().contains("SUCCESS"));
    JSONObject getActivitiesPayload = new JSONObject().put("username", username);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(getActivitiesPayload.toString())
            .asString();
    assert (findResponse
        .getBody()
        .toString()
        .contains(ChangeUserAttributesActivity.class.getSimpleName()));
    assert (isCorrectAttribute(username, "address", newAddress));
  }

  @Test
  public void changeCityTest() throws Exception {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withCity("Chicago")
        .buildAndPersist(userDao);
    String newCity = "New York";

    JSONObject requestPayload =
        new JSONObject().put("password", password).put("key", "city").put("value", newCity);
    TestUtils.login(username, password);
    HttpResponse<String> createUserResponse =
        Unirest.post(TestUtils.getServerUrl() + "/change-account-setting")
            .body(requestPayload.toString())
            .asString();
    assertTrue(createUserResponse.getBody().contains("SUCCESS"));
    JSONObject getActivitiesPayload = new JSONObject().put("username", username);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(getActivitiesPayload.toString())
            .asString();
    assert (findResponse
        .getBody()
        .toString()
        .contains(ChangeUserAttributesActivity.class.getSimpleName()));
    assert (isCorrectAttribute(username, "city", newCity));
  }

  @Test
  public void changeStateTest() throws Exception {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withState("PA")
        .buildAndPersist(userDao);
    String newState = "GA";

    JSONObject requestPayload =
        new JSONObject().put("password", password).put("key", "state").put("value", newState);
    TestUtils.login(username, password);
    HttpResponse<String> createUserResponse =
        Unirest.post(TestUtils.getServerUrl() + "/change-account-setting")
            .body(requestPayload.toString())
            .asString();
    assertTrue(createUserResponse.getBody().contains("SUCCESS"));
    JSONObject getActivitiesPayload = new JSONObject().put("username", username);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(getActivitiesPayload.toString())
            .asString();
    assert (findResponse
        .getBody()
        .toString()
        .contains(ChangeUserAttributesActivity.class.getSimpleName()));
    assert (isCorrectAttribute(username, "state", newState));
  }

  @Test
  public void changeZipcodeTest() throws Exception {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withZipcode("19091")
        .buildAndPersist(userDao);
    String newZipcode = "19012";
    JSONObject requestPayload =
        new JSONObject().put("password", password).put("key", "zipcode").put("value", newZipcode);
    TestUtils.login(username, password);
    HttpResponse<String> createUserResponse =
        Unirest.post(TestUtils.getServerUrl() + "/change-account-setting")
            .body(requestPayload.toString())
            .asString();
    assertTrue(createUserResponse.getBody().contains("SUCCESS"));
    JSONObject getActivitiesPayload = new JSONObject().put("username", username);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(getActivitiesPayload.toString())
            .asString();
    assert (findResponse
        .getBody()
        .toString()
        .contains(ChangeUserAttributesActivity.class.getSimpleName()));
    assert (isCorrectAttribute(username, "zipcode", newZipcode));
  }
}
