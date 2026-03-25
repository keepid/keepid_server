package UserTest;

import static org.assertj.core.api.Assertions.assertThat;

import Config.DeploymentLevel;
import Database.Organization.OrgDao;
import Database.Organization.OrgDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import Security.EmailUtil;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.UserType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EnrollWorkerEmailTest {

  private static UserDao userDao;
  private static OrgDao orgDao;

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    userDao = UserDaoFactory.create(DeploymentLevel.TEST);
    orgDao = OrgDaoFactory.create(DeploymentLevel.TEST);
    userDao.clear();
    orgDao.clear();

    EntityFactory.createOrganization()
        .withOrgName("Email Test Org")
        .withAddress("100 Test Ave")
        .withCity("New York")
        .withState("NY")
        .withZipcode("10003")
        .withEmail("contact@emailtestorg.org")
        .buildAndPersist(orgDao);

    EntityFactory.createUser()
        .withFirstName("Director")
        .withLastName("Email")
        .withBirthDate("01-01-1980")
        .withEmail("director@emailtestorg.org")
        .withOrgName("Email Test Org")
        .withUsername("directorEmail")
        .withPasswordToHash("directorEmail")
        .withUserType(UserType.Director)
        .buildAndPersist(userDao);
  }

  @AfterClass
  public static void tearDown() {
    userDao.clear();
    orgDao.clear();
  }

  @Test
  public void enrollmentWelcomeEmailContainsOrgName() {
    String html = EmailUtil.getEnrollmentWelcomeEmail("Alice", "Test Organization");
    assertThat(html).contains("Test Organization");
    assertThat(html).contains("Alice");
  }

  @Test
  public void enrollmentWelcomeEmailContainsLoginInstructions() {
    String html = EmailUtil.getEnrollmentWelcomeEmail("Bob", "Some Org");
    assertThat(html).contains("Google authentication");
    assertThat(html).contains("Forgot Password");
    assertThat(html).contains("keep.id");
  }

  @Test
  public void enrollmentWelcomeEmailIsHtml() {
    String html = EmailUtil.getEnrollmentWelcomeEmail("Test", "Org");
    assertThat(html).startsWith("<html>");
    assertThat(html).endsWith("</html>");
  }

  @Test
  public void enrollWorkerSuccessThenDuplicateEmailFails() {
    TestUtils.login("directorEmail", "directorEmail");

    JSONObject body = new JSONObject();
    body.put("firstname", "EmailTest");
    body.put("lastname", "Worker");
    body.put("birthDate", "04-10-1992");
    body.put("email", "emailtest-dupe@example.com");
    body.put("personRole", "Worker");

    HttpResponse<String> firstResponse =
        Unirest.post(TestUtils.getServerUrl() + "/enroll-worker")
            .body(body.toString())
            .asString();
    JSONObject firstJSON = TestUtils.responseStringToJSON(firstResponse.getBody());
    assertThat(firstJSON.getString("status")).isEqualTo("ENROLL_SUCCESS");

    JSONObject secondBody = new JSONObject();
    secondBody.put("firstname", "Another");
    secondBody.put("lastname", "Worker");
    secondBody.put("birthDate", "05-05-1993");
    secondBody.put("email", "emailtest-dupe@example.com");
    secondBody.put("personRole", "Worker");

    HttpResponse<String> secondResponse =
        Unirest.post(TestUtils.getServerUrl() + "/enroll-worker")
            .body(secondBody.toString())
            .asString();
    JSONObject secondJSON = TestUtils.responseStringToJSON(secondResponse.getBody());
    assertThat(secondJSON.getString("status")).isEqualTo("EMAIL_ALREADY_EXISTS");
  }

  @Test
  public void enrollWorkerInvalidRoleReturnsError() {
    TestUtils.login("directorEmail", "directorEmail");

    JSONObject body = new JSONObject();
    body.put("firstname", "Bad");
    body.put("lastname", "Role");
    body.put("birthDate", "06-06-1996");
    body.put("email", "badrole@example.com");
    body.put("personRole", "Client");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/enroll-worker")
            .body(body.toString())
            .asString();
    JSONObject json = TestUtils.responseStringToJSON(response.getBody());
    assertThat(json.getString("status")).isEqualTo("INVALID_PARAMETER");
  }

  @Test
  public void enrollWorkerNoSessionReturnsError() {
    TestUtils.logout();

    JSONObject body = new JSONObject();
    body.put("firstname", "No");
    body.put("lastname", "Session");
    body.put("birthDate", "07-07-1997");
    body.put("email", "nosession-email@example.com");
    body.put("personRole", "Worker");

    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/enroll-worker")
            .body(body.toString())
            .asString();
    JSONObject json = TestUtils.responseStringToJSON(response.getBody());
    assertThat(json.getString("status")).isEqualTo("SESSION_TOKEN_FAILURE");
  }
}
