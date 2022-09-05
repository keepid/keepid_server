package UserTest;

import Activity.ChangeUserAttributesActivity;
import Config.DeploymentLevel;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import Database.Token.TokenDao;
import Database.Token.TokenDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import Security.AccountSecurityController;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.User;
import io.javalin.http.Context;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.mockito.Mockito.*;

public class ChangeAccountSettingsIntegrationTests {
  private Context ctx;
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.TEST);
  ActivityDao activityDao = ActivityDaoFactory.create(DeploymentLevel.TEST);
  TokenDao tokenDao = TokenDaoFactory.create(DeploymentLevel.TEST);

  @BeforeClass
  public static void setUp() throws GeneralSecurityException, IOException {
    TestUtils.startServer();
  }

  @Before
  public void initialize() {
    ctx = mock(Context.class);
  }

  @After
  public void reset() {
    userDao.clear();
    tokenDao.clear();
    activityDao.clear();
    clearInvocations(ctx);
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  // Make sure to enable .env file configurations for these tests
  // TODO: Swap new SecurityUtils() for a mock that correctly (or incorrectly hashes passwords.
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
  public void changeFirstNameTest() throws Exception {
    String username = "account-settings-test";
    String password = "account-settings-test-password";
    User user =
        EntityFactory.createUser()
            .withUsername(username)
            .withPasswordToHash(password)
            .withFirstName("David")
            .buildAndPersist(userDao);
    String newFirstName = "Sarah";

    String inputString =
        "{\"password\":" + password + ",\"key\":\"firstName\",\"value\":" + newFirstName + "}";

    when(ctx.body()).thenReturn(inputString);
    when(ctx.sessionAttribute("username")).thenReturn(username);

    JSONObject body = new JSONObject();
    body.put("username", username);

    AccountSecurityController asc = new AccountSecurityController(userDao, tokenDao, activityDao);
    asc.changeAccountSetting.handle(ctx);

    TestUtils.login(username, password);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(body.toString())
            .asString();
    assert (findResponse
        .getBody()
        .toString()
        .contains(ChangeUserAttributesActivity.class.getSimpleName()));
    TestUtils.logout();
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

    String inputString =
        "{\"password\":" + password + ",\"key\":\"lastName\",\"value\":" + newLastName + "}";

    when(ctx.body()).thenReturn(inputString);
    when(ctx.sessionAttribute("username")).thenReturn(username);

    AccountSecurityController asc = new AccountSecurityController(userDao, tokenDao, activityDao);
    asc.changeAccountSetting.handle(ctx);

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

    String inputString =
        "{\"password\":" + password + ",\"key\":\"birthDate\",\"value\":" + newBirthDate + "}";

    when(ctx.body()).thenReturn(inputString);
    when(ctx.sessionAttribute("username")).thenReturn(username);

    JSONObject body = new JSONObject();
    body.put("username", username);

    AccountSecurityController asc = new AccountSecurityController(userDao, tokenDao, activityDao);
    asc.changeAccountSetting.handle(ctx);
    TestUtils.login(username, password);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(body.toString())
            .asString();
    TestUtils.logout();
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

    String inputString =
        "{\"password\":" + password + ",\"key\":\"phone\",\"value\":" + newPhone + "}";

    when(ctx.body()).thenReturn(inputString);
    when(ctx.sessionAttribute("username")).thenReturn(username);

    AccountSecurityController asc = new AccountSecurityController(userDao, tokenDao, activityDao);
    asc.changeAccountSetting.handle(ctx);

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

    String inputString =
        "{\"password\":" + password + ",\"key\":\"email\",\"value\":" + newEmail + "}";

    when(ctx.body()).thenReturn(inputString);
    when(ctx.sessionAttribute("username")).thenReturn(username);

    JSONObject body = new JSONObject();
    body.put("username", username);

    AccountSecurityController asc = new AccountSecurityController(userDao, tokenDao, activityDao);
    asc.changeAccountSetting.handle(ctx);
    TestUtils.login(username, password);
    HttpResponse findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(body.toString())
            .asString();
    TestUtils.logout();
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

    String inputString =
        "{\"password\":" + password + ",\"key\":\"address\",\"value\":" + newAddress + "}";

    when(ctx.body()).thenReturn(inputString);
    when(ctx.sessionAttribute("username")).thenReturn(username);

    AccountSecurityController asc = new AccountSecurityController(userDao, tokenDao, activityDao);
    asc.changeAccountSetting.handle(ctx);

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

    String inputString =
        "{\"password\":" + password + ",\"key\":\"city\",\"value\":" + newCity + "}";

    when(ctx.body()).thenReturn(inputString);
    when(ctx.sessionAttribute("username")).thenReturn(username);

    AccountSecurityController asc = new AccountSecurityController(userDao, tokenDao, activityDao);
    asc.changeAccountSetting.handle(ctx);

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

    String inputString =
        "{\"password\":" + password + ",\"key\":\"state\",\"value\":" + newState + "}";

    when(ctx.body()).thenReturn(inputString);
    when(ctx.sessionAttribute("username")).thenReturn(username);

    AccountSecurityController asc = new AccountSecurityController(userDao, tokenDao, activityDao);
    asc.changeAccountSetting.handle(ctx);

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

    String inputString =
        "{\"password\":" + password + ",\"key\":\"zipcode\",\"value\":" + newZipcode + "}";

    when(ctx.body()).thenReturn(inputString);
    when(ctx.sessionAttribute("username")).thenReturn(username);

    AccountSecurityController asc = new AccountSecurityController(userDao, tokenDao, activityDao);
    asc.changeAccountSetting.handle(ctx);

    assert (isCorrectAttribute(username, "zipcode", newZipcode));
  }
}
