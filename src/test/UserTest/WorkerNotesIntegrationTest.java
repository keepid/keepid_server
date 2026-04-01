package UserTest;

import static org.assertj.core.api.Assertions.assertThat;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
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

public class WorkerNotesIntegrationTest {

  private static final String TEST_ORG = "WorkerNotes Test Org";
  private static final String PASSWORD = "testPassword123";

  UserDao userDao = UserDaoFactory.create(DeploymentLevel.TEST);

  @BeforeClass
  public static void setUp() throws GeneralSecurityException, IOException {
    TestUtils.startServer();
  }

  @After
  public void reset() {
    TestUtils.logout();
    Unirest.config().reset();
    userDao.clear();
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void workerCanSaveNotesOnClientProfile() {
    EntityFactory.createUser()
        .withUsername("wn-worker")
        .withPasswordToHash(PASSWORD)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Worker)
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("wn-client")
        .withPasswordToHash(PASSWORD)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login("wn-worker", PASSWORD);

    JSONObject body = new JSONObject();
    body.put("username", "wn-client");
    body.put("workerNotes", "Client needs housing assistance");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/save-worker-notes")
            .body(body.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedClient = userDao.get("wn-client").orElseThrow();
    assertThat(updatedClient.getWorkerNotes()).isEqualTo("Client needs housing assistance");
  }

  @Test
  public void adminCanSaveNotesOnClientProfile() {
    EntityFactory.createUser()
        .withUsername("wn-admin")
        .withPasswordToHash(PASSWORD)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Admin)
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("wn-client2")
        .withPasswordToHash(PASSWORD)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login("wn-admin", PASSWORD);

    JSONObject body = new JSONObject();
    body.put("username", "wn-client2");
    body.put("workerNotes", "Admin note about client");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/save-worker-notes")
            .body(body.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");

    User updatedClient = userDao.get("wn-client2").orElseThrow();
    assertThat(updatedClient.getWorkerNotes()).isEqualTo("Admin note about client");
  }

  @Test
  public void clientCannotSaveWorkerNotes() {
    EntityFactory.createUser()
        .withUsername("wn-client-attacker")
        .withPasswordToHash(PASSWORD)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("wn-client-target")
        .withPasswordToHash(PASSWORD)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login("wn-client-attacker", PASSWORD);

    JSONObject body = new JSONObject();
    body.put("username", "wn-client-target");
    body.put("workerNotes", "Should not be saved");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/save-worker-notes")
            .body(body.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("INSUFFICIENT_PRIVILEGE");

    User targetClient = userDao.get("wn-client-target").orElseThrow();
    assertThat(targetClient.getWorkerNotes()).isNull();
  }

  @Test
  public void unauthenticatedUserCannotSaveWorkerNotes() {
    JSONObject body = new JSONObject();
    body.put("username", "anyone");
    body.put("workerNotes", "Should not be saved");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/save-worker-notes")
            .body(body.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("INSUFFICIENT_PRIVILEGE");
  }

  @Test
  public void workerNotesReturnedInGetUserInfo() {
    EntityFactory.createUser()
        .withUsername("wn-worker3")
        .withPasswordToHash(PASSWORD)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Worker)
        .buildAndPersist(userDao);

    User client = EntityFactory.createUser()
        .withUsername("wn-client3")
        .withPasswordToHash(PASSWORD)
        .withOrgName(TEST_ORG)
        .withUserType(UserType.Client)
        .build();
    client.setWorkerNotes("Pre-existing notes");
    userDao.save(client);

    TestUtils.login("wn-worker3", PASSWORD);

    JSONObject getBody = new JSONObject();
    getBody.put("username", "wn-client3");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/get-user-info")
            .body(getBody.toString())
            .asString();

    JSONObject responseJson = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");
    assertThat(responseJson.getString("workerNotes")).isEqualTo("Pre-existing notes");
  }
}
