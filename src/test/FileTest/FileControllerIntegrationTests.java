package FileTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.UserType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static FileTest.FileControllerIntegrationTestHelperMethods.uploadTestPDF;
import static org.assertj.core.api.Assertions.assertThat;

public class FileControllerIntegrationTests {
  private final UserDao userDao = UserDaoFactory.create(DeploymentLevel.TEST);

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
  }

  @After
  public void clear() {
    if (userDao != null) {
      userDao.clear();
    }
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void uploadValidPDFTest() {
    String username = "username1";
    String password = "password1";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    uploadTestPDF();
    TestUtils.logout();
  }

  @Test
  public void getFilesReturnsUploadedApplicationMetadataTest() {
    String username = "username2";
    String password = "password2";
    EntityFactory.createUser()
        .withUsername(username)
        .withPasswordToHash(password)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login(username, password);
    uploadTestPDF();

    JSONObject req = new JSONObject().put("fileType", "APPLICATION_PDF");
    HttpResponse<String> getFilesResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-files").body(req.toString()).asString();
    JSONObject responseJson = TestUtils.responseStringToJSON(getFilesResponse.getBody());

    assertThat(responseJson.getString("status")).isEqualTo("SUCCESS");
    assertThat(responseJson.has("documents")).isTrue();

    JSONArray documents = responseJson.getJSONArray("documents");
    assertThat(documents.length()).isGreaterThan(0);

    JSONObject firstDocument = documents.getJSONObject(0);
    assertThat(firstDocument.has("id")).isTrue();
    assertThat(firstDocument.has("uploader")).isTrue();
    assertThat(firstDocument.has("uploadDate")).isTrue();
    assertThat(firstDocument.has("filename")).isTrue();
    assertThat(firstDocument.getString("uploader")).isEqualTo(username);

    TestUtils.logout();
  }
}
