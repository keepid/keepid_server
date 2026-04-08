package FileTest;

import Config.DeploymentLevel;
import Database.Organization.OrgDao;
import Database.Organization.OrgDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.UserType;
import java.nio.charset.StandardCharsets;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static FileTest.FileControllerIntegrationTestHelperMethods.uploadOrgDocumentPDF;
import static FileTest.FileControllerIntegrationTestHelperMethods.uploadTestPDF;
import static org.assertj.core.api.Assertions.assertThat;

public class FileControllerIntegrationTests {
  private final UserDao userDao = UserDaoFactory.create(DeploymentLevel.TEST);
  private final OrgDao orgDao = OrgDaoFactory.create(DeploymentLevel.TEST);

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
  }

  @After
  public void clear() {
    if (userDao != null) {
      userDao.clear();
    }
    if (orgDao != null) {
      orgDao.clear();
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

  @Test
  public void twoAdminsSameOrgOrgDocumentUploadListDownloadTest() {
    String orgName = "Shared Org For Org Docs";
    EntityFactory.createOrganization().withOrgName(orgName).buildAndPersist(orgDao);

    String admin1 = "orgDocAdminOne";
    String admin2 = "orgDocAdminTwo";
    String pass = "samePassword1!";
    EntityFactory.createUser()
        .withUsername(admin1)
        .withPasswordToHash(pass)
        .withOrgName(orgName)
        .withUserType(UserType.Admin)
        .buildAndPersist(userDao);
    EntityFactory.createUser()
        .withUsername(admin2)
        .withPasswordToHash(pass)
        .withOrgName(orgName)
        .withUserType(UserType.Admin)
        .buildAndPersist(userDao);

    TestUtils.login(admin1, pass);
    uploadOrgDocumentPDF();

    JSONObject getReq = new JSONObject().put("fileType", "ORG_DOCUMENT");
    HttpResponse<String> listResp =
        Unirest.post(TestUtils.getServerUrl() + "/get-files").body(getReq.toString()).asString();
    JSONObject listJson = TestUtils.responseStringToJSON(listResp.getBody());
    assertThat(listJson.getString("status")).isEqualTo("SUCCESS");
    JSONArray documents = listJson.getJSONArray("documents");
    assertThat(documents.length()).isEqualTo(1);
    String fileId = documents.getJSONObject(0).getString("id");

    TestUtils.logout();
    TestUtils.login(admin2, pass);

    JSONObject dlReq = new JSONObject().put("fileId", fileId).put("fileType", "ORG_DOCUMENT");
    HttpResponse<byte[]> dlResp =
        Unirest.post(TestUtils.getServerUrl() + "/download-file")
            .body(dlReq.toString())
            .asBytes();
    assertThat(dlResp.getStatus()).isEqualTo(200);
    assertThat(dlResp.getHeaders().getFirst("Content-Type").toLowerCase()).contains("application/pdf");
    byte[] bodyBytes = dlResp.getBody();
    assertThat(bodyBytes.length).isGreaterThan(100);
    assertThat(new String(bodyBytes, 0, Math.min(4, bodyBytes.length), StandardCharsets.US_ASCII))
        .isEqualTo("%PDF");

    TestUtils.logout();
  }
}
