package UserTest;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import Database.Organization.OrgDao;
import Database.Organization.OrgDaoFactory;
import Database.Token.TokenDao;
import Database.Token.TokenDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import Security.SecurityUtils;
import Security.Services.ChangePasswordService;
import Security.Services.ForgotPasswordService;
import Security.Services.ResetPasswordService;
import Security.EmailSenderFactory;
import Security.Tokens;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.User;
import User.UserMessage;
import User.UserType;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChangePasswordIntegrationTests {
  private static final int EXPIRATION_TIME_2_HOURS = 7200000;

  private static UserDao staticUserDao;
  private static OrgDao orgDao;
  private static TokenDao staticTokenDao;
  private static ActivityDao staticActivityDao;

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    staticUserDao = UserDaoFactory.create(DeploymentLevel.TEST);
    orgDao = OrgDaoFactory.create(DeploymentLevel.TEST);
    staticTokenDao = TokenDaoFactory.create(DeploymentLevel.TEST);
    staticActivityDao = ActivityDaoFactory.create(DeploymentLevel.TEST);

    EntityFactory.createOrganization()
        .withOrgName("Password Settings Org")
        .buildAndPersist(orgDao);

    EntityFactory.createUser()
        .withUsername("password-reset-test")
        .withPasswordToHash("a4d3jgHow0")
        .withEmail("contact@example.com")
        .withOrgName("Password Settings Org")
        .withUserType(UserType.Client)
        .buildAndPersist(staticUserDao);
  }

  @AfterClass
  public static void tearDown() {
    staticUserDao.clear();
    orgDao.clear();
    staticTokenDao.clear();
    staticActivityDao.clear();
  }

  MongoDatabase db = MongoConfig.getDatabase(DeploymentLevel.TEST);
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.TEST);
  TokenDao tokenDao = TokenDaoFactory.create(DeploymentLevel.TEST);
  ActivityDao activityDao = ActivityDaoFactory.create(DeploymentLevel.TEST);

  // Make sure to enable .env file configurations for these tests

  // We will switch between these two passwords for simplicity
  String password1 = "a4d3jgHow0";
  String password2 = "9d46kHPkl3";

  private boolean isCorrectPassword(String username, String possiblePassword) {
    MongoCollection<User> userCollection = db.getCollection("user", User.class);
    User user = userCollection.find(eq("username", username)).first();
    if (user == null) {
      return false;
    }
    Argon2 argon2 = Argon2Factory.create();
    char[] possiblePasswordArr = possiblePassword.toCharArray();
    String passwordHash = user.getPassword();
    return argon2.verify(passwordHash, possiblePasswordArr);
  }

  @Test
  public void forgotPasswordCreatesTokenTest() {
    String username = "password-reset-test";
    ForgotPasswordService forgotPasswordService =
        new ForgotPasswordService(
            userDao, tokenDao, username, EmailSenderFactory.forDeploymentLevel(DeploymentLevel.TEST));
    Message returnMessage = forgotPasswordService.executeAndGetResponse();
    assertEquals(UserMessage.SUCCESS, returnMessage);
    Tokens tokens = tokenDao.get(username).get();
    assertEquals(1, tokens.numTokens());
    tokenDao.removeTokenIfLast(username, tokens, Tokens.TokenType.PASSWORD_RESET);
  }

  @Test
  public void forgotPasswordWithEmailCreatesTokenTest() {
    String username = "password-reset-test";
    String email = "contact@example.com";
    ForgotPasswordService forgotPasswordService =
        new ForgotPasswordService(
            userDao, tokenDao, email, EmailSenderFactory.forDeploymentLevel(DeploymentLevel.TEST));
    Message returnMessage = forgotPasswordService.executeAndGetResponse();
    assertEquals(UserMessage.SUCCESS, returnMessage);
    Tokens tokens = tokenDao.get(username).get();
    assertEquals(1, tokens.numTokens());
    tokenDao.removeTokenIfLast(username, tokens, Tokens.TokenType.PASSWORD_RESET);
  }

  @Test
  public void resetPasswordWithJWTTest() throws Exception {
    String username = "password-reset-test";
    String id = SecurityUtils.generateRandomStringId();
    String jwt =
        SecurityUtils.createJWT(
            id, "KeepID", username, "Password Reset Confirmation", EXPIRATION_TIME_2_HOURS);
    ResetPasswordService forgotPasswordService =
        new ResetPasswordService(userDao, tokenDao, activityDao, jwt, username);
    Message returnMessage = forgotPasswordService.executeAndGetResponse();
    assertEquals(UserMessage.AUTH_FAILURE, returnMessage);
    Optional<Tokens> tokens = tokenDao.get(username);
    assertTrue(tokens.isEmpty());
  }

  //
  //  @Test
  //  public void changePasswordWhileLoggedInTest() throws Exception {
  //    String username = "password-reset-test";
  //    String oldPassword = "";
  //    String newPassword = "";
  //
  //    if (isCorrectPassword(username, password1)) {
  //      oldPassword = password1;
  //      newPassword = password2;
  //    } else if (isCorrectPassword(username, password2)) {
  //      oldPassword = password2;
  //      newPassword = password1;
  //    } else {
  //      throw new Exception("Current test password doesn't match examples");
  //    }
  //
  //    String inputString =
  //        "{\"oldPassword\":" + oldPassword + ",\"newPassword\":" + newPassword + "}";
  //
  //    when(ctx.body()).thenReturn(inputString);
  //    when(ctx.sessionAttribute("username")).thenReturn(username);
  //
  //    AccountSecurityController asc = new AccountSecurityController(db);
  //    asc.changePassword.handle(ctx);
  //
  //    assert (isCorrectPassword(username, newPassword));
  //  }

  @Test
  public void changePasswordHelperTest() throws Exception {
    String username = "password-reset-test";
    String oldPassword = "";
    String newPassword = "";

    if (isCorrectPassword(username, password1)) {
      oldPassword = password1;
      newPassword = password2;
    } else if (isCorrectPassword(username, password2)) {
      oldPassword = password2;
      newPassword = password1;
    } else {
      throw new Exception("Current test password doesn't match examples");
    }

    Message result =
        ChangePasswordService.changePassword(
            userDao, username, activityDao, oldPassword, newPassword);
    assert (result == UserMessage.AUTH_SUCCESS);
  }
}
