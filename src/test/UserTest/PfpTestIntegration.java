package UserTest;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Database.Organization.OrgDaoFactory;
import Database.Organization.OrgDao;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class PfpTestIntegration {
  private static String currentPfpFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources";

  private static UserDao userDao;
  private static OrgDao orgDao;

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    userDao = UserDaoFactory.create(DeploymentLevel.TEST);
    orgDao = OrgDaoFactory.create(DeploymentLevel.TEST);

    EntityFactory.createOrganization()
        .withOrgName("login history Org")
        .buildAndPersist(orgDao);

    EntityFactory.createUser()
        .withUsername("createAdminOwner")
        .withPasswordToHash("login-history-test")
        .withOrgName("login history Org")
        .withUserType(UserType.Director)
        .buildAndPersist(userDao);
  }

  @AfterClass
  public static void tearDown() {
    userDao.clear();
    orgDao.clear();
  }

  MongoDatabase db = MongoConfig.getDatabase(DeploymentLevel.TEST);

  @Ignore
  @Test
  public void uploadValidPDFTestExists() {
    TestUtils.login("createAdminOwner", "login-history-test");
    uploadTestPfp();
    getPfp();
    TestUtils.logout();
  }

  public static void uploadTestPfp() {
    File PDF1 = new File(currentPfpFolderPath + File.separator + "1.png");
    File PDF2 = new File(currentPfpFolderPath + File.separator + "2.png");
    File PDF3 = new File(currentPfpFolderPath + File.separator + "3.png");
    File PDF4 = new File(currentPfpFolderPath + File.separator + "4.png");
    HttpResponse<String> upload1 =
        Unirest.post(TestUtils.getServerUrl() + "/upload-pfp")
            .header("Content-Disposition", "attachment")
            .field("username", "asdf")
            .field("fileName", "ASDF")
            .field("file", PDF1, "image/png")
            .asString();
    HttpResponse<String> upload2 =
        Unirest.post(TestUtils.getServerUrl() + "/upload-pfp")
            .header("Content-Disposition", "attachment")
            .field("username", "asdf")
            .field("fileName", "ASDF")
            .field("file", PDF2, "image/png")
            .asString();
    HttpResponse<String> upload3 =
        Unirest.post(TestUtils.getServerUrl() + "/upload-pfp")
            .header("Content-Disposition", "attachment")
            .field("username", "asdf")
            .field("fileName", "ASDF")
            .field("file", PDF2, "image/png")
            .asString();
    HttpResponse<String> upload4 =
        Unirest.post(TestUtils.getServerUrl() + "/upload-pfp")
            .header("Content-Disposition", "attachment")
            .field("username", "asdf")
            .field("fileName", "ASDF")
            .field("file", PDF1, "image/png")
            .asString();
    HttpResponse<String> upload5 =
        Unirest.post(TestUtils.getServerUrl() + "/upload-pfp")
            .header("Content-Disposition", "attachment")
            .field("username", "asdf")
            .field("fileName", "ASDF")
            .field("file", PDF3, "image/png")
            .asString();
    HttpResponse<String> upload6 =
        Unirest.post(TestUtils.getServerUrl() + "/upload-pfp")
            .header("Content-Disposition", "attachment")
            .field("username", "asdf")
            .field("fileName", "ASDF")
            .field("file", PDF4, "image/png")
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(upload6.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
    HttpResponse get =
        Unirest.post(TestUtils.getServerUrl() + "/load-pfp")
            .body("{ \"username\": \"asdf\" }")
            .asString();
    assert (get.isSuccess());
  }

  public static void getPfp() {
    File examplePDF = new File(currentPfpFolderPath + File.separator + "mvc.png");
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload-pfp")
            .header("Content-Disposition", "attachment")
            .field("username", "asdf")
            .field("fileName", "ASDF")
            .field("file", examplePDF, "image/png")
            .asString();
    HttpResponse<byte[]> get =
        Unirest.post(TestUtils.getServerUrl() + "/load-pfp")
            .body("{ \"username\": \"asdf\" }")
            .asBytes();
    assert (get.isSuccess());
  }
}
