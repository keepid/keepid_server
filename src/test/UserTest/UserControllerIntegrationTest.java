package UserTest;

import static org.assertj.core.api.Assertions.assertThat;

import Config.DeploymentLevel;
import Database.Organization.OrgDao;
import Database.Organization.OrgDaoFactory;
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

public class UserControllerIntegrationTest {

  private static UserDao userDao;
  private static OrgDao orgDao;

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    userDao = UserDaoFactory.create(DeploymentLevel.TEST);
    orgDao = OrgDaoFactory.create(DeploymentLevel.TEST);

    EntityFactory.createOrganization()
        .withOrgName("Broad Street Ministry")
        .withAddress("311 Broad Street")
        .withCity("Philadelphia")
        .withState("PA")
        .withZipcode("19104")
        .withEmail("mikedahl@broadstreetministry.org")
        .withPhoneNumber("1234567890")
        .buildAndPersist(orgDao);

    EntityFactory.createOrganization()
        .withOrgName("Test Org")
        .withWebsite("http://www.testorg.org")
        .withEIN("111222333")
        .withAddress("100 Test Ave")
        .withCity("New York")
        .withState("NY")
        .withZipcode("10003")
        .withEmail("contact@testorg.org")
        .buildAndPersist(orgDao);

    EntityFactory.createUser()
        .withFirstName("Mike")
        .withLastName("Dahl")
        .withBirthDate("06-16-1960")
        .withEmail("mikedahl@broadstreetministry.org")
        .withPhoneNumber("1234567890")
        .withOrgName("Broad Street Ministry")
        .withAddress("311 Broad Street")
        .withCity("Philadelphia")
        .withState("PA")
        .withZipcode("19104")
        .withUsername("adminBSM")
        .withPasswordToHash("adminBSM")
        .withUserType(UserType.Director)
        .buildAndPersist(userDao);

    // Workers and clients for getClients/getMembers tests
    EntityFactory.createUser()
        .withFirstName("Worker")
        .withLastName("Tff")
        .withEmail("workertff@broadstreetministry.org")
        .withUsername("workertffBSM")
        .withPasswordToHash("workertffBSM")
        .withOrgName("Broad Street Ministry")
        .withUserType(UserType.Worker)
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withFirstName("Client")
        .withLastName("Bsm")
        .withEmail("client1@broadstreetministry.org")
        .withUsername("client1BSM")
        .withPasswordToHash("client1BSM")
        .withOrgName("Broad Street Ministry")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);
  }

  @AfterClass
  public static void tearDown() {
    userDao.clear();
    orgDao.clear();
  }

  @Test
  public void loginUserWithNoUsernameTest() {
    JSONObject body = new JSONObject();
    body.put("password", "pass");
    body.put("username", "");

    HttpResponse<String> actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/login")
            .header("Accept", "*/*")
            .header("Content-Type", "text/plain")
            .body(body.toString())
            .asString();

    JSONObject actualResponseJSON = TestUtils.responseStringToJSON(actualResponse.getBody());

    assert (actualResponseJSON.has("firstName"));
    assertThat(actualResponseJSON.getString("firstName")).isEqualTo("");
    assert (actualResponseJSON.has("lastName"));
    assertThat(actualResponseJSON.getString("lastName")).isEqualTo("");
    assert (actualResponseJSON.has("organization"));
    assertThat(actualResponseJSON.getString("organization")).isEqualTo("");
    assert (actualResponseJSON.has("userRole"));
    assertThat(actualResponseJSON.getString("userRole")).isEqualTo("");
    assert (actualResponseJSON.has("status"));
    assertThat(actualResponseJSON.getString("status")).isEqualTo("AUTH_FAILURE");
  }

  @Test
  public void loginUserWithEmailTest() {
    JSONObject body = new JSONObject();
    body.put("username", "mikedahl@broadstreetministry.org");
    body.put("password", "adminBSM");

    HttpResponse<String> actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/login")
            .header("Accept", "*/*")
            .header("Content-Type", "text/plain")
            .body(body.toString())
            .asString();

    JSONObject actualResponseJSON = TestUtils.responseStringToJSON(actualResponse.getBody());
    assertThat(actualResponseJSON.getString("status")).isEqualTo("AUTH_SUCCESS");
    assertThat(actualResponseJSON.getString("firstName")).isEqualTo("Mike");
    assertThat(actualResponseJSON.getString("lastName")).isEqualTo("Dahl");
    assertThat(actualResponseJSON.getString("organization")).isEqualTo("Broad Street Ministry");
  }

  @Test
  public void testUserEncryption() {
    TestUtils.login("adminBSM", "adminBSM");
    JSONObject body = new JSONObject();
    body.put("username", "adminBSM");

    HttpResponse<String> actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-user-info").body(body.toString()).asString();
    JSONObject actualResponseJson = TestUtils.responseStringToJSON(actualResponse.getBody());
    assertThat(actualResponseJson.get("city").toString()).isEqualTo("Philadelphia");
    assertThat(actualResponseJson.get("firstName").toString()).isEqualTo("Mike");
    assertThat(actualResponseJson.get("lastName").toString()).isEqualTo("Dahl");
    assertThat(actualResponseJson.get("zipcode").toString()).isEqualTo("19104");
    assertThat(actualResponseJson.get("phone").toString()).isEqualTo("1234567890");
    assertThat(actualResponseJson.get("address").toString()).isEqualTo("311 Broad Street");
    assertThat(actualResponseJson.get("birthDate").toString()).isEqualTo("06-16-1960");
    assertThat(actualResponseJson.get("email").toString())
        .isEqualTo("mikedahl@broadstreetministry.org");
  }

  @Test
  public void noUsernameInRequestUsesCtxUsername() {
    TestUtils.login("adminBSM", "adminBSM");
    JSONObject body = new JSONObject();

    HttpResponse<String> actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-user-info").body(body.toString()).asString();
    JSONObject actualResponseJson = TestUtils.responseStringToJSON(actualResponse.getBody());
    assertThat(actualResponseJson.get("city").toString()).isEqualTo("Philadelphia");
    assertThat(actualResponseJson.get("firstName").toString()).isEqualTo("Mike");
    assertThat(actualResponseJson.get("lastName").toString()).isEqualTo("Dahl");
    assertThat(actualResponseJson.get("zipcode").toString()).isEqualTo("19104");
    assertThat(actualResponseJson.get("phone").toString()).isEqualTo("1234567890");
    assertThat(actualResponseJson.get("address").toString()).isEqualTo("311 Broad Street");
    assertThat(actualResponseJson.get("birthDate").toString()).isEqualTo("06-16-1960");
    assertThat(actualResponseJson.get("email").toString())
        .isEqualTo("mikedahl@broadstreetministry.org");
  }

  @Test
  public void noBodyUsesCtxUsername() {
    TestUtils.login("adminBSM", "adminBSM");

    HttpResponse<String> actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-user-info").asString();
    JSONObject actualResponseJson = TestUtils.responseStringToJSON(actualResponse.getBody());
    assertThat(actualResponseJson.get("city").toString()).isEqualTo("Philadelphia");
    assertThat(actualResponseJson.get("firstName").toString()).isEqualTo("Mike");
    assertThat(actualResponseJson.get("lastName").toString()).isEqualTo("Dahl");
    assertThat(actualResponseJson.get("zipcode").toString()).isEqualTo("19104");
    assertThat(actualResponseJson.get("phone").toString()).isEqualTo("1234567890");
    assertThat(actualResponseJson.get("address").toString()).isEqualTo("311 Broad Street");
    assertThat(actualResponseJson.get("birthDate").toString()).isEqualTo("06-16-1960");
    assertThat(actualResponseJson.get("email").toString())
        .isEqualTo("mikedahl@broadstreetministry.org");
  }

  @Test
  public void testGetMembersEncryption() {
    TestUtils.login("adminBSM", "adminBSM");
    JSONObject body = new JSONObject();
    body.put("name", "Mike Dahl");
    body.put("listType", "clients");
    body.put("currentPage", "1");
    body.put("itemsPerPage", "10");
    body.put("role", "Director");

    HttpResponse<String> actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-organization-members")
            .body(body.toString())
            .asString();
    JSONObject actualResponseJson = TestUtils.responseStringToJSON(actualResponse.getBody());
  }

  @Test
  public void createUserWithNullOrgNameTest() {
    JSONObject body = new JSONObject();
    body.put("firstname", "mel");
    body.put("lastname", "car");
    body.put("birthDate", "02-16-1998");
    body.put("email", "email@email");
    body.put("phonenumber", "1234567890");
    body.put("address", "123 park ave");
    body.put("city", "new york");
    body.put("state", "NY");
    body.put("zipcode", "10003");
    body.put("twoFactorOn", false);
    body.put("username", "testUser123");
    body.put("password", "testUser123");
    body.put("personRole", "Worker");
    body.put("orgName", "");

    HttpResponse<String> actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/create-invited-user")
            .body(body.toString())
            .asString();

    JSONObject actualResponseJSON = TestUtils.responseStringToJSON(actualResponse.getBody());

    assert (actualResponseJSON.has("status"));
    assertThat(actualResponseJSON.getString("status")).isEqualTo("INVALID_PARAMETER");
  }

  @Test
  public void createInvitedUserSuccessfullyTest() {
    JSONObject body = new JSONObject();
    body.put("firstname", "mel");
    body.put("lastname", "car");
    body.put("birthDate", "02-16-1998");
    body.put("email", "email@email");
    body.put("phonenumber", "1234567890");
    body.put("address", "123 park ave");
    body.put("city", "new york");
    body.put("state", "NY");
    body.put("zipcode", "10003");
    body.put("twoFactorOn", false);
    body.put("username", "testUser123");
    body.put("password", "testUser123");
    body.put("personRole", "Worker");
    body.put("orgName", "Test Org");

    HttpResponse<String> actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/create-invited-user")
            .body(body.toString())
            .asString();

    JSONObject actualResponseJSON = TestUtils.responseStringToJSON(actualResponse.getBody());

    assert (actualResponseJSON.has("status"));
    assertThat(actualResponseJSON.getString("status")).isEqualTo("ENROLL_SUCCESS");
  }

  @Test
  public void createUserWithNullRoleTest() {
    JSONObject body = new JSONObject();
    body.put("firstname", "mel");
    body.put("lastname", "car");
    body.put("birthDate", "02-16-1998");
    body.put("email", "email@email");
    body.put("phonenumber", "1234567890");
    body.put("address", "123 park ave");
    body.put("city", "new york");
    body.put("state", "NY");
    body.put("zipcode", "10003");
    body.put("twoFactorOn", false);
    body.put("username", "testUser123");
    body.put("password", "testUser123");
    body.put("personRole", "");
    body.put("orgName", "Test Org");

    HttpResponse<String> actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/create-invited-user")
            .body(body.toString())
            .asString();
    JSONObject actualResponseJSON = TestUtils.responseStringToJSON(actualResponse.getBody());

    assert (actualResponseJSON.has("status"));
    assertThat(actualResponseJSON.getString("status")).isEqualTo("INVALID_PRIVILEGE_TYPE");
  }

  @Test
  public void createInvitedUserDuplicateEmailFailsTest() {
    JSONObject firstBody = new JSONObject();
    firstBody.put("firstname", "first");
    firstBody.put("lastname", "user");
    firstBody.put("birthDate", "02-16-1998");
    firstBody.put("email", "dupe-email-test@keep.id");
    firstBody.put("phonenumber", "1234567890");
    firstBody.put("address", "123 park ave");
    firstBody.put("city", "new york");
    firstBody.put("state", "NY");
    firstBody.put("zipcode", "10003");
    firstBody.put("twoFactorOn", false);
    firstBody.put("username", "dupeEmailUserA");
    firstBody.put("password", "dupeEmailUserA");
    firstBody.put("personRole", "Worker");
    firstBody.put("orgName", "Test Org");

    HttpResponse<String> firstResponse =
        Unirest.post(TestUtils.getServerUrl() + "/create-invited-user")
            .body(firstBody.toString())
            .asString();
    JSONObject firstResponseJSON = TestUtils.responseStringToJSON(firstResponse.getBody());
    assertThat(firstResponseJSON.getString("status")).isEqualTo("ENROLL_SUCCESS");

    JSONObject secondBody = new JSONObject(firstBody.toString());
    secondBody.put("firstname", "second");
    secondBody.put("username", "dupeEmailUserB");
    secondBody.put("password", "dupeEmailUserB");

    HttpResponse<String> secondResponse =
        Unirest.post(TestUtils.getServerUrl() + "/create-invited-user")
            .body(secondBody.toString())
            .asString();
    JSONObject secondResponseJSON = TestUtils.responseStringToJSON(secondResponse.getBody());
    assertThat(secondResponseJSON.getString("status")).isEqualTo("EMAIL_ALREADY_EXISTS");
  }

  @Test
  public void getClients() {
    TestUtils.login("adminBSM", "adminBSM");

    JSONObject body = new JSONObject();
    body.put("name", "Worker Tff");
    body.put("listType", "clients");
    body.put("currentPage", "1");
    body.put("itemsPerPage", "10");
    body.put("role", "Worker");

    HttpResponse<String> actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-organization-members")
            .body(body.toString())
            .asString();
    JSONObject actualResponseJSON = TestUtils.responseStringToJSON(actualResponse.getBody());

    assert (actualResponseJSON.has("status"));
    assertThat(actualResponseJSON.getString("status")).isEqualTo("SUCCESS");
    assert (actualResponseJSON.has("numPeople"));
    assertThat(actualResponseJSON.getInt("numPeople")).isGreaterThan(0);
    assert (actualResponseJSON.has("people"));
  }

  @Test
  public void getMembers() {
    TestUtils.login("adminBSM", "adminBSM");

    JSONObject body = new JSONObject();
    body.put("name", "Worker Tff");
    body.put("listType", "members");
    body.put("currentPage", "1");
    body.put("itemsPerPage", "10");
    body.put("role", "Worker");

    HttpResponse<String> actualResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-organization-members")
            .body(body.toString())
            .asString();
    JSONObject actualResponseJSON = TestUtils.responseStringToJSON(actualResponse.getBody());

    assert (actualResponseJSON.has("status"));
    assertThat(actualResponseJSON.getString("status")).isEqualTo("SUCCESS");
    assert (actualResponseJSON.has("numPeople"));
    assertThat(actualResponseJSON.getInt("numPeople")).isGreaterThan(0);
    assert (actualResponseJSON.has("people"));
  }
}
