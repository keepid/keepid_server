package UserTest;

import static org.assertj.core.api.Assertions.assertThat;

import Config.DeploymentLevel;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import Database.Token.TokenDao;
import Database.Token.TokenDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import OptionalUserInformation.DemographicInfo;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.OptionalInformation;
import User.User;
import User.UserType;
import java.io.IOException;
import java.security.GeneralSecurityException;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
    // Clear cookies to ensure clean state between tests
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
  public void updateUserProfile_ownProfile_dotNotation_success() {
    String username = "profile-update-client";
    String password = "profile-update-password";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login(username, password);

    JSONObject updateRequest = new JSONObject();
    updateRequest.put("optionalInformation.demographicInfo.languagePreference", "Spanish");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/update-user-profile")
            .body(updateRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedUser = userDao.get(username).orElseThrow();
    assertThat(updatedUser.getOptionalInformation()).isNotNull();
    assertThat(updatedUser.getOptionalInformation().getDemographicInfo()).isNotNull();
    assertThat(updatedUser.getOptionalInformation().getDemographicInfo().getLanguagePreference())
        .isEqualTo("Spanish");
  }

  @Test
  public void updateUserProfile_ownProfile_rootLevelField_success() {
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
  public void updateUserProfile_withoutSession_authFailure() {
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
  public void updateUserProfile_adminUpdatesClientInSameOrg_success() {
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
    updateRequest.put("optionalInformation.demographicInfo.languagePreference", "French");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/update-user-profile")
            .body(updateRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedClient = userDao.get(clientUsername).orElseThrow();
    assertThat(updatedClient.getOptionalInformation().getDemographicInfo().getLanguagePreference())
        .isEqualTo("French");
  }

  @Test
  public void updateUserProfile_clientTriesToUpdateAnotherClient_insufficientPrivilege() {
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

  // ---------- delete-profile-field tests ----------

  @Test
  public void deleteProfileField_ownProfile_success() {
    String username = "delete-field-client";
    String password = "delete-field-password";

    User user = EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .build();

    OptionalInformation optionalInfo = new OptionalInformation();
    DemographicInfo demographicInfo = new DemographicInfo();
    demographicInfo.setLanguagePreference("Spanish");
    optionalInfo.setDemographicInfo(demographicInfo);
    user.setOptionalInformation(optionalInfo);
    userDao.save(user);

    TestUtils.login(username, password);

    JSONObject deleteRequest = new JSONObject();
    deleteRequest.put("fieldPath", "optionalInformation.demographicInfo.languagePreference");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/delete-profile-field")
            .body(deleteRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedUser = userDao.get(username).orElseThrow();
    assertThat(updatedUser.getOptionalInformation().getDemographicInfo().getLanguagePreference())
        .isNull();
  }

  @Test
  public void deleteProfileField_withoutSession_authFailure() {
    JSONObject deleteRequest = new JSONObject();
    deleteRequest.put("fieldPath", "optionalInformation.demographicInfo.languagePreference");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/delete-profile-field")
            .body(deleteRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("AUTH_FAILURE");
  }

  @Test
  public void deleteProfileField_emptyFieldPath_invalidParameter() {
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
  public void deleteProfileField_adminDeletesClientField_success() {
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

    OptionalInformation optionalInfo = new OptionalInformation();
    DemographicInfo demographicInfo = new DemographicInfo();
    demographicInfo.setLanguagePreference("German");
    optionalInfo.setDemographicInfo(demographicInfo);
    clientUser.setOptionalInformation(optionalInfo);
    userDao.save(clientUser);

    TestUtils.login(adminUsername, password);

    JSONObject deleteRequest = new JSONObject();
    deleteRequest.put("username", clientUsername);
    deleteRequest.put("fieldPath", "optionalInformation.demographicInfo.languagePreference");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/delete-profile-field")
            .body(deleteRequest.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedClient = userDao.get(clientUsername).orElseThrow();
    assertThat(updatedClient.getOptionalInformation().getDemographicInfo().getLanguagePreference())
        .isNull();
  }

  // ---------- get-user-info authorization tests ----------

  @Test
  public void getUserInfo_adminGetsClientInSameOrg_success() {
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
  public void getUserInfo_clientTriesToGetAnotherClient_insufficientPrivilege() {
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
  public void getUserInfo_withoutSession_authFailure() {
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
  public void getUserInfo_ownProfile_noUsername_success() {
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
