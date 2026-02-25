package UserTest;

import static org.assertj.core.api.Assertions.assertThat;

import Config.DeploymentLevel;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import Database.Token.TokenDao;
import Database.Token.TokenDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.Address;
import User.Name;
import User.User;
import User.UserType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Integration tests for the user-information routes.
 * Tests hit the HTTP endpoints directly (POST /get-user-info, /update-user-profile, /delete-profile-field).
 */
public class UserInformationIntegrationTests {

  private static final String TEST_ORG = "UserInfo Test Org";

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
    Unirest.config().reset();
    userDao.clear();
    tokenDao.clear();
    activityDao.clear();
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  // ---------- update-user-profile tests ----------

  @Test
  public void updateUserProfileCurrentNameSuccess() {
    String username = "profile-update-client";
    String password = "profile-update-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login(username, password);

    JSONObject nameObj = new JSONObject();
    nameObj.put("first", "UpdatedFirst");
    nameObj.put("last", "UpdatedLast");
    JSONObject updateRequest = new JSONObject();
    updateRequest.put("currentName", nameObj);

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/update-user-profile")
            .body(updateRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedUser = userDao.get(username).orElseThrow();
    assertThat(updatedUser.getCurrentName()).isNotNull();
    assertThat(updatedUser.getCurrentName().getFirst()).isEqualTo("UpdatedFirst");
    assertThat(updatedUser.getCurrentName().getLast()).isEqualTo("UpdatedLast");
  }

  @Test
  public void updateUserProfileRootLevelEmailSuccess() {
    String username = "profile-update-root";
    String password = "profile-update-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .withEmail("old@example.com")
        .buildAndPersist(userDao);

    TestUtils.login(username, password);

    JSONObject updateRequest = new JSONObject();
    updateRequest.put("email", "new@example.com");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/update-user-profile")
            .body(updateRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedUser = userDao.get(username).orElseThrow();
    assertThat(updatedUser.getEmail()).isEqualTo("new@example.com");
  }

  @Test
  public void updateUserProfileWithoutSessionAuthFailure() {
    JSONObject updateRequest = new JSONObject();
    updateRequest.put("email", "new@example.com");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/update-user-profile")
            .body(updateRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("AUTH_FAILURE");
  }

  @Test
  public void updateUserProfileAdminUpdatesClientInSameOrgSuccess() {
    String adminUsername = "profile-admin";
    String clientUsername = "profile-client";
    String password = "shared-password";

    EntityFactory.createUser()
        .withUsername(adminUsername)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Admin)
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername(clientUsername)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login(adminUsername, password);

    JSONObject updateRequest = new JSONObject();
    updateRequest.put("username", clientUsername);
    updateRequest.put("sex", "Female");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/update-user-profile")
            .body(updateRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedClient = userDao.get(clientUsername).orElseThrow();
    assertThat(updatedClient.getSex()).isEqualTo("Female");
  }

  @Test
  public void updateUserProfileClientTriesToUpdateAnotherClientInsufficientPrivilege() {
    String client1Username = "client-one";
    String client2Username = "client-two";
    String password = "shared-password";

    EntityFactory.createUser()
        .withUsername(client1Username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername(client2Username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login(client1Username, password);

    JSONObject updateRequest = new JSONObject();
    updateRequest.put("username", client2Username);
    updateRequest.put("email", "hacked@example.com");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/update-user-profile")
            .body(updateRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("INSUFFICIENT_PRIVILEGE");
  }

  @Test
  public void updateUserProfileMailAddressSuccess() {
    String username = "profile-mail-addr";
    String password = "profile-mail-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login(username, password);

    JSONObject mailAddr = new JSONObject();
    mailAddr.put("line1", "PO Box 456");
    mailAddr.put("city", "New York");
    mailAddr.put("state", "NY");
    mailAddr.put("zip", "10001");
    JSONObject updateRequest = new JSONObject();
    updateRequest.put("mailAddress", mailAddr);

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/update-user-profile")
            .header("Content-Type", "application/json")
            .body(updateRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedUser = userDao.get(username).orElseThrow();
    assertThat(updatedUser.getMailAddress()).isNotNull();
    assertThat(updatedUser.getMailAddress().getLine1()).isEqualTo("PO Box 456");
    assertThat(updatedUser.getMailAddress().getCity()).isEqualTo("New York");
    assertThat(updatedUser.getMailAddress().getState()).isEqualTo("NY");
    assertThat(updatedUser.getMailAddress().getZip()).isEqualTo("10001");
  }

  // ---------- delete-profile-field tests ----------

  @Test
  public void deleteProfileFieldOwnProfileSuccess() {
    String username = "delete-field-client";
    String password = "delete-field-password";

    User user = EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .build();
    user.setSex("Male");
    userDao.save(user);

    TestUtils.login(username, password);

    JSONObject deleteRequest = new JSONObject();
    deleteRequest.put("fieldPath", "sex");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/delete-profile-field")
            .body(deleteRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedUser = userDao.get(username).orElseThrow();
    assertThat(updatedUser.getSex()).isNull();
  }

  @Test
  public void deleteProfileFieldWithoutSessionAuthFailure() {
    JSONObject deleteRequest = new JSONObject();
    deleteRequest.put("fieldPath", "sex");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/delete-profile-field")
            .body(deleteRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("AUTH_FAILURE");
  }

  @Test
  public void deleteProfileFieldEmptyFieldPathInvalidParameter() {
    String username = "delete-field-empty";
    String password = "delete-field-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login(username, password);

    JSONObject deleteRequest = new JSONObject();
    deleteRequest.put("fieldPath", "");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/delete-profile-field")
            .body(deleteRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("INVALID_PARAMETER");
  }

  @Test
  public void deleteProfileFieldAdminDeletesClientFieldSuccess() {
    String adminUsername = "delete-admin";
    String clientUsername = "delete-client";
    String password = "shared-password";

    EntityFactory.createUser()
        .withUsername(adminUsername)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Admin)
        .buildAndPersist(userDao);

    User clientUser = EntityFactory.createUser()
        .withUsername(clientUsername)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .build();
    clientUser.setMotherName(new Name("Jane", null, "Doe", null, "Smith"));
    userDao.save(clientUser);

    TestUtils.login(adminUsername, password);

    JSONObject deleteRequest = new JSONObject();
    deleteRequest.put("username", clientUsername);
    deleteRequest.put("fieldPath", "motherName");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/delete-profile-field")
            .body(deleteRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedClient = userDao.get(clientUsername).orElseThrow();
    assertThat(updatedClient.getMotherName()).isNull();
  }

  // ---------- get-user-info authorization tests ----------

  @Test
  public void getUserInfoAdminGetsClientInSameOrgSuccess() {
    String adminUsername = "getinfo-admin";
    String clientUsername = "getinfo-client";
    String password = "shared-password";

    EntityFactory.createUser()
        .withUsername(adminUsername)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Admin)
        .withFirstName("Admin")
        .withLastName("User")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername(clientUsername)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .withFirstName("Client")
        .withLastName("User")
        .withEmail("client@example.com")
        .buildAndPersist(userDao);

    TestUtils.login(adminUsername, password);

    JSONObject getRequest = new JSONObject();
    getRequest.put("username", clientUsername);

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/get-user-info")
            .body(getRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");
    assertThat(responseJson.getString("firstName")).isEqualTo("Client");
    assertThat(responseJson.getString("lastName")).isEqualTo("User");
    assertThat(responseJson.getString("email")).isEqualTo("client@example.com");
  }

  @Test
  public void getUserInfoClientTriesToGetAnotherClientInsufficientPrivilege() {
    String client1Username = "getinfo-client1";
    String client2Username = "getinfo-client2";
    String password = "shared-password";

    EntityFactory.createUser()
        .withUsername(client1Username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername(client2Username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login(client1Username, password);

    JSONObject getRequest = new JSONObject();
    getRequest.put("username", client2Username);

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/get-user-info")
            .body(getRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("INSUFFICIENT_PRIVILEGE");
  }

  @Test
  public void getUserInfoWithoutSessionAuthFailure() {
    JSONObject getRequest = new JSONObject();
    getRequest.put("username", "any-user");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/get-user-info")
            .body(getRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("AUTH_FAILURE");
  }

  @Test
  public void getUserInfoOwnProfileNoUsernameSuccess() {
    String username = "getinfo-own";
    String password = "getinfo-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .withFirstName("Self")
        .withLastName("User")
        .buildAndPersist(userDao);

    TestUtils.login(username, password);

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/get-user-info")
            .body(new JSONObject().toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");
    assertThat(responseJson.getString("firstName")).isEqualTo("Self");
    assertThat(responseJson.getString("lastName")).isEqualTo("User");
  }
}
