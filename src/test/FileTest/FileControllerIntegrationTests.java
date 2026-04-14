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

  @Test
  public void attachPacketPartLazilyCreatesPacketAndPreventsDuplicatesTest() {
    String orgName = "Packet Org";
    EntityFactory.createOrganization().withOrgName(orgName).buildAndPersist(orgDao);

    String admin = "packetAdmin";
    String pass = "samePassword1!";
    EntityFactory.createUser()
        .withUsername(admin)
        .withPasswordToHash(pass)
        .withOrgName(orgName)
        .withUserType(UserType.Admin)
        .buildAndPersist(userDao);

    TestUtils.login(admin, pass);
    uploadTestPDF();
    uploadOrgDocumentPDF();

    JSONObject appReq = new JSONObject().put("fileType", "APPLICATION_PDF");
    HttpResponse<String> appListResp =
        Unirest.post(TestUtils.getServerUrl() + "/get-files").body(appReq.toString()).asString();
    JSONArray appDocs = TestUtils.responseStringToJSON(appListResp.getBody()).getJSONArray("documents");
    String applicationId = appDocs.getJSONObject(0).getString("id");

    JSONObject orgReq = new JSONObject().put("fileType", "ORG_DOCUMENT");
    HttpResponse<String> orgListResp =
        Unirest.post(TestUtils.getServerUrl() + "/get-files").body(orgReq.toString()).asString();
    JSONArray orgDocs = TestUtils.responseStringToJSON(orgListResp.getBody()).getJSONArray("documents");
    String orgDocId = orgDocs.getJSONObject(0).getString("id");

    JSONObject attachReq =
        new JSONObject().put("applicationId", applicationId).put("fileId", orgDocId);
    HttpResponse<String> attachResp =
        Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part").body(attachReq.toString()).asString();
    JSONObject attachJson = TestUtils.responseStringToJSON(attachResp.getBody());
    assertThat(attachJson.getString("status")).isEqualTo("SUCCESS");
    JSONObject packet = attachJson.getJSONObject("packet");
    assertThat(packet.getJSONArray("parts").length()).isEqualTo(2);
    assertThat(attachJson.getBoolean("alreadyAttached")).isFalse();

    HttpResponse<String> attachAgainResp =
        Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part").body(attachReq.toString()).asString();
    JSONObject attachAgainJson = TestUtils.responseStringToJSON(attachAgainResp.getBody());
    assertThat(attachAgainJson.getString("status")).isEqualTo("SUCCESS");
    assertThat(attachAgainJson.getBoolean("alreadyAttached")).isTrue();
    assertThat(attachAgainJson.getJSONObject("packet").getJSONArray("parts").length()).isEqualTo(2);

    JSONObject packetFetchReq = new JSONObject().put("applicationId", applicationId);
    HttpResponse<String> packetFetchResp =
        Unirest.post(TestUtils.getServerUrl() + "/get-packet-for-application")
            .body(packetFetchReq.toString())
            .asString();
    JSONObject packetFetchJson = TestUtils.responseStringToJSON(packetFetchResp.getBody());
    assertThat(packetFetchJson.getString("status")).isEqualTo("SUCCESS");
    assertThat(packetFetchJson.getJSONObject("packet").getJSONArray("parts").length()).isEqualTo(2);

    TestUtils.logout();
  }

  @Test
  public void detachAndReorderPacketPartsUpdatePersistedPacketTest() throws Exception {
    String orgName = "Packet Org Two";
    EntityFactory.createOrganization().withOrgName(orgName).buildAndPersist(orgDao);

    String admin = "packetAdminTwo";
    String pass = "samePassword1!";
    EntityFactory.createUser()
        .withUsername(admin)
        .withPasswordToHash(pass)
        .withOrgName(orgName)
        .withUserType(UserType.Admin)
        .buildAndPersist(userDao);

    TestUtils.login(admin, pass);
    uploadTestPDF();
    uploadOrgDocumentPDF();
    uploadOrgDocumentPDF("CIS_401_Final_Progress_Report_two.pdf");

    String applicationId =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-files")
                    .body(new JSONObject().put("fileType", "APPLICATION_PDF").toString())
                    .asString()
                    .getBody())
            .getJSONArray("documents")
            .getJSONObject(0)
            .getString("id");

    JSONArray orgDocuments =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-files")
                    .body(new JSONObject().put("fileType", "ORG_DOCUMENT").toString())
                    .asString()
                    .getBody())
            .getJSONArray("documents");
    String firstOrgDocId = orgDocuments.getJSONObject(0).getString("id");
    String secondOrgDocId = orgDocuments.getJSONObject(1).getString("id");

    Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part")
        .body(new JSONObject().put("applicationId", applicationId).put("fileId", firstOrgDocId).toString())
        .asString();
    Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part")
        .body(new JSONObject().put("applicationId", applicationId).put("fileId", secondOrgDocId).toString())
        .asString();

    JSONArray reordered = new JSONArray().put(secondOrgDocId).put(firstOrgDocId);
    HttpResponse<String> reorderResp =
        Unirest.post(TestUtils.getServerUrl() + "/reorder-packet-parts")
            .body(new JSONObject().put("applicationId", applicationId).put("orderedFileIds", reordered).toString())
            .asString();
    JSONObject reorderJson = TestUtils.responseStringToJSON(reorderResp.getBody());
    assertThat(reorderJson.getString("status")).isEqualTo("SUCCESS");
    JSONArray reorderedParts = reorderJson.getJSONObject("packet").getJSONArray("parts");
    assertThat(reorderedParts.getJSONObject(1).getString("fileId")).isEqualTo(secondOrgDocId);

    HttpResponse<String> detachResp =
        Unirest.post(TestUtils.getServerUrl() + "/detach-packet-part")
            .body(new JSONObject().put("applicationId", applicationId).put("fileId", secondOrgDocId).toString())
            .asString();
    JSONObject detachJson = TestUtils.responseStringToJSON(detachResp.getBody());
    assertThat(detachJson.getString("status")).isEqualTo("SUCCESS");
    JSONArray detachedParts = detachJson.getJSONObject("packet").getJSONArray("parts");
    assertThat(detachedParts.length()).isEqualTo(2);
    assertThat(detachedParts.getJSONObject(1).getString("fileId")).isEqualTo(firstOrgDocId);

    TestUtils.logout();
  }
}
