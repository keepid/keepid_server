package UserTest;

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
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChangeTwoFactorSettingIntegrationTests {
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.TEST);
  TokenDao tokenDao = TokenDaoFactory.create(DeploymentLevel.TEST);
  ActivityDao activityDao = ActivityDaoFactory.create(DeploymentLevel.TEST);

  @BeforeClass
  public static void setUp() throws GeneralSecurityException, IOException {
    TestUtils.startServer();
  }

  @After
  public void clear() {
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
  @Test
  public void changeTwoFactorFromOffToOnTest() throws Exception {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    User user =
        EntityFactory.createUser()
            .withUsername(username)
            .withPasswordToHash(password)
            .withTwoFactorState(false)
            .buildAndPersist(userDao);
    JSONObject requestPayload = new JSONObject().put("twoFactorOn", true);
    TestUtils.login(username, password);
    HttpResponse<String> createUserResponse =
        Unirest.post(TestUtils.getServerUrl() + "/change-two-factor-setting")
            .body(requestPayload.toString())
            .asString();
    assertTrue(createUserResponse.getBody().contains("SUCCESS"));
    User readUser = userDao.get(username).orElseThrow();
    assertTrue(readUser.getTwoFactorOn());
  }

  @Test
  public void changeTwoFactorFromOnToOffTest() throws Exception {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    User user =
        EntityFactory.createUser()
            .withUsername(username)
            .withPasswordToHash(password)
            .withTwoFactorState(true)
            .buildAndPersist(userDao);
    JSONObject requestPayload = new JSONObject().put("twoFactorOn", false);
    TestUtils.login(username, password);
    HttpResponse<String> createUserResponse =
        Unirest.post(TestUtils.getServerUrl() + "/change-two-factor-setting")
            .body(requestPayload.toString())
            .asString();
    assertTrue(createUserResponse.getBody().contains("SUCCESS"));
    User readUser = userDao.get(username).orElseThrow();
    assertFalse(readUser.getTwoFactorOn());
  }
}
