package UserTest;

import Config.DeploymentLevel;
import Database.Organization.OrgDao;
import Database.Organization.OrgDaoFactory;
import Database.Token.TokenDao;
import Database.Token.TokenDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.UserType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class TwoFactorHandlerIntegrationTests {

  private static UserDao userDao;
  private static OrgDao orgDao;
  private static TokenDao tokenDao;

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    userDao = UserDaoFactory.create(DeploymentLevel.TEST);
    orgDao = OrgDaoFactory.create(DeploymentLevel.TEST);
    tokenDao = TokenDaoFactory.create(DeploymentLevel.TEST);

    EntityFactory.createOrganization()
        .withOrgName("2FA Token Org")
        .buildAndPersist(orgDao);

    EntityFactory.createUser()
        .withUsername("tokentest-valid")
        .withPasswordToHash("tokentest-valid")
        .withOrgName("2FA Token Org")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("tokentest-notoken")
        .withPasswordToHash("tokentest-notoken")
        .withOrgName("2FA Token Org")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("tokentest-expired")
        .withPasswordToHash("tokentest-expired")
        .withOrgName("2FA Token Org")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    // Valid token (expires Jan 1, 2090)
    EntityFactory.createTokens()
        .withUsername("tokentest-valid")
        .withTwoFactorCode("444555")
        .withTwoFactorExp(new Date(Long.valueOf("3786930000000")))
        .buildAndPersist(tokenDao);

    // Expired token (expired Jan 1, 1970)
    EntityFactory.createTokens()
        .withUsername("tokentest-expired")
        .withTwoFactorCode("123123")
        .withTwoFactorExp(new Date(Long.valueOf("0")))
        .buildAndPersist(tokenDao);
  }

  @AfterClass
  public static void tearDown() {
    userDao.clear();
    orgDao.clear();
    tokenDao.clear();
  }

  @Test
  public void verifyUserWithIncorrectUsernameTest() {
    JSONObject body = new JSONObject();
    body.put("username", "not-a-user");
    body.put("token", "000000");

    HttpResponse actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/two-factor")
            .header("Accept", "*/*")
            .header("Content-Type", "text/plain")
            .body(body.toString())
            .asString();

    JSONObject actualResponseJSON =
        TestUtils.responseStringToJSON(actualResponse.getBody().toString());

    assert (actualResponseJSON.has("message"));
    assertThat(actualResponseJSON.getString("message"))
        .isEqualTo("User does not exist in database.");
    assert (actualResponseJSON.has("status"));
    assertThat(actualResponseJSON.getString("status")).isEqualTo("USER_NOT_FOUND");
  }

  @Test
  public void verifyUserWithNoTokenTest() {
    JSONObject body = new JSONObject();
    body.put("username", "tokentest-notoken");
    body.put("token", "000000");

    HttpResponse actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/two-factor")
            .header("Accept", "*/*")
            .header("Content-Type", "text/plain")
            .body(body.toString())
            .asString();

    JSONObject actualResponseJSON =
        TestUtils.responseStringToJSON(actualResponse.getBody().toString());

    assert (actualResponseJSON.has("message"));
    assertThat(actualResponseJSON.getString("message")).isEqualTo("2fa token not found for user.");
    assert (actualResponseJSON.has("status"));
    assertThat(actualResponseJSON.getString("status")).isEqualTo("AUTH_FAILURE");
  }

  @Test
  public void verifyUserWithIncorrectTokenTest() {
    JSONObject body = new JSONObject();
    body.put("username", "tokentest-valid");
    body.put("token", "000000");

    HttpResponse actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/two-factor")
            .header("Accept", "*/*")
            .header("Content-Type", "text/plain")
            .body(body.toString())
            .asString();

    JSONObject actualResponseJSON =
        TestUtils.responseStringToJSON(actualResponse.getBody().toString());

    assert (actualResponseJSON.has("message"));
    assertThat(actualResponseJSON.getString("message")).isEqualTo("Invalid 2fa token.");
    assert (actualResponseJSON.has("status"));
    assertThat(actualResponseJSON.getString("status")).isEqualTo("AUTH_FAILURE");
  }

  @Test
  public void verifyUserWithExpiredTokenTest() {
    JSONObject body = new JSONObject();
    body.put("username", "tokentest-expired");
    body.put("token", "123123");

    HttpResponse actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/two-factor")
            .header("Accept", "*/*")
            .header("Content-Type", "text/plain")
            .body(body.toString())
            .asString();

    JSONObject actualResponseJSON =
        TestUtils.responseStringToJSON(actualResponse.getBody().toString());

    assert (actualResponseJSON.has("message"));
    assertThat(actualResponseJSON.getString("message")).isEqualTo("2FA link expired.");
    assert (actualResponseJSON.has("status"));
    assertThat(actualResponseJSON.getString("status")).isEqualTo("AUTH_FAILURE");
  }
}
