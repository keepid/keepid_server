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
import org.bson.types.ObjectId;
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
    String attachedCloneId = attachJson.getString("attachedFileId");
    JSONObject attachmentPartJson = packet.getJSONArray("parts").getJSONObject(1);
    assertThat(attachmentPartJson.getString("fileId")).isNotEqualTo(orgDocId);
    assertThat(attachmentPartJson.getString("sourceFileId")).isEqualTo(orgDocId);
    JSONObject orgListAfterAttachJson =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-files")
                    .body(new JSONObject().put("fileType", "ORG_DOCUMENT").toString())
                    .asString()
                    .getBody());
    assertThat(orgListAfterAttachJson.getJSONArray("documents").length()).isEqualTo(1);

    HttpResponse<String> attachAgainResp =
        Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part").body(attachReq.toString()).asString();
    JSONObject attachAgainJson = TestUtils.responseStringToJSON(attachAgainResp.getBody());
    assertThat(attachAgainJson.getString("status")).isEqualTo("SUCCESS");
    assertThat(attachAgainJson.getBoolean("alreadyAttached")).isTrue();
    assertThat(attachAgainJson.getString("attachedFileId")).isEqualTo(attachedCloneId);
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

    JSONObject attachFirstJson =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part")
                    .body(new JSONObject().put("applicationId", applicationId).put("fileId", firstOrgDocId).toString())
                    .asString()
                    .getBody());
    JSONObject attachSecondJson =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part")
                    .body(new JSONObject().put("applicationId", applicationId).put("fileId", secondOrgDocId).toString())
                    .asString()
                    .getBody());
    String firstAttachedCloneId = attachFirstJson.getString("attachedFileId");
    String secondAttachedCloneId = attachSecondJson.getString("attachedFileId");

    JSONArray reordered = new JSONArray().put(secondAttachedCloneId).put(firstAttachedCloneId);
    HttpResponse<String> reorderResp =
        Unirest.post(TestUtils.getServerUrl() + "/reorder-packet-parts")
            .body(new JSONObject().put("applicationId", applicationId).put("orderedFileIds", reordered).toString())
            .asString();
    JSONObject reorderJson = TestUtils.responseStringToJSON(reorderResp.getBody());
    assertThat(reorderJson.getString("status")).isEqualTo("SUCCESS");
    JSONArray reorderedParts = reorderJson.getJSONObject("packet").getJSONArray("parts");
    assertThat(reorderedParts.getJSONObject(1).getString("fileId")).isEqualTo(secondAttachedCloneId);
    assertThat(reorderedParts.getJSONObject(1).getString("sourceFileId")).isEqualTo(secondOrgDocId);

    HttpResponse<String> detachResp =
        Unirest.post(TestUtils.getServerUrl() + "/detach-packet-part")
            .body(new JSONObject().put("applicationId", applicationId).put("fileId", secondAttachedCloneId).toString())
            .asString();
    JSONObject detachJson = TestUtils.responseStringToJSON(detachResp.getBody());
    assertThat(detachJson.getString("status")).isEqualTo("SUCCESS");
    JSONArray detachedParts = detachJson.getJSONObject("packet").getJSONArray("parts");
    assertThat(detachedParts.length()).isEqualTo(2);
    assertThat(detachedParts.getJSONObject(1).getString("fileId")).isEqualTo(firstAttachedCloneId);
    assertThat(detachedParts.getJSONObject(1).getString("sourceFileId")).isEqualTo(firstOrgDocId);

    TestUtils.logout();
  }

  @Test
  public void updateApplicationAttachmentPdfAllowsCloneAndRejectsSourceOrgDocumentTest() {
    String orgName = "Packet Org Edit";
    EntityFactory.createOrganization().withOrgName(orgName).buildAndPersist(orgDao);

    String admin = "packetEditor";
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

    String sourceOrgDocId =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-files")
                    .body(new JSONObject().put("fileType", "ORG_DOCUMENT").toString())
                    .asString()
                    .getBody())
            .getJSONArray("documents")
            .getJSONObject(0)
            .getString("id");

    JSONObject attachJson =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part")
                    .body(new JSONObject().put("applicationId", applicationId).put("fileId", sourceOrgDocId).toString())
                    .asString()
                    .getBody());
    String cloneAttachmentId = attachJson.getString("attachedFileId");

    java.io.File examplePDF =
        new java.io.File(
            java.nio.file.Paths.get("").toAbsolutePath().toString()
                + java.io.File.separator
                + "src"
                + java.io.File.separator
                + "test"
                + java.io.File.separator
                + "resources"
                + java.io.File.separator
                + "CIS_401_Final_Progress_Report.pdf");

    HttpResponse<String> updateCloneResp =
        Unirest.post(TestUtils.getServerUrl() + "/update-application-attachment-pdf")
            .header("Content-Disposition", "attachment")
            .field("applicationId", applicationId)
            .field("fileId", cloneAttachmentId)
            .field("file", examplePDF, "application/pdf")
            .asString();
    JSONObject updateCloneJson = TestUtils.responseStringToJSON(updateCloneResp.getBody());
    assertThat(updateCloneJson.getString("status")).isEqualTo("SUCCESS");
    assertThat(updateCloneJson.getString("fileId")).isEqualTo(cloneAttachmentId);

    HttpResponse<String> updateSourceResp =
        Unirest.post(TestUtils.getServerUrl() + "/update-application-attachment-pdf")
            .header("Content-Disposition", "attachment")
            .field("applicationId", applicationId)
            .field("fileId", sourceOrgDocId)
            .field("file", examplePDF, "application/pdf")
            .asString();
    JSONObject updateSourceJson = TestUtils.responseStringToJSON(updateSourceResp.getBody());
    assertThat(updateSourceJson.getString("status")).isEqualTo("NO_SUCH_FILE");

    TestUtils.logout();
  }

  @Test
  public void updateApplicationAttachmentPdfRejectsClientRoleTest() {
    String orgName = "Packet Org Client Denied";
    EntityFactory.createOrganization().withOrgName(orgName).buildAndPersist(orgDao);

    String admin = "packetAdminEditor";
    String client = "packetClientEditor";
    String pass = "samePassword1!";
    EntityFactory.createUser()
        .withUsername(admin)
        .withPasswordToHash(pass)
        .withOrgName(orgName)
        .withUserType(UserType.Admin)
        .buildAndPersist(userDao);
    EntityFactory.createUser()
        .withUsername(client)
        .withPasswordToHash(pass)
        .withOrgName(orgName)
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    TestUtils.login(admin, pass);
    uploadTestPDF();
    uploadOrgDocumentPDF();

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

    String sourceOrgDocId =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-files")
                    .body(new JSONObject().put("fileType", "ORG_DOCUMENT").toString())
                    .asString()
                    .getBody())
            .getJSONArray("documents")
            .getJSONObject(0)
            .getString("id");

    JSONObject attachJson =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part")
                    .body(new JSONObject().put("applicationId", applicationId).put("fileId", sourceOrgDocId).toString())
                    .asString()
                    .getBody());
    String cloneAttachmentId = attachJson.getString("attachedFileId");
    TestUtils.logout();

    TestUtils.login(client, pass);
    java.io.File examplePDF =
        new java.io.File(
            java.nio.file.Paths.get("").toAbsolutePath().toString()
                + java.io.File.separator
                + "src"
                + java.io.File.separator
                + "test"
                + java.io.File.separator
                + "resources"
                + java.io.File.separator
                + "CIS_401_Final_Progress_Report.pdf");

    HttpResponse<String> updateCloneResp =
        Unirest.post(TestUtils.getServerUrl() + "/update-application-attachment-pdf")
            .header("Content-Disposition", "attachment")
            .field("applicationId", applicationId)
            .field("fileId", cloneAttachmentId)
            .field("file", examplePDF, "application/pdf")
            .asString();
    JSONObject updateCloneJson = TestUtils.responseStringToJSON(updateCloneResp.getBody());
    assertThat(updateCloneJson.getString("status")).isEqualTo("INSUFFICIENT_PRIVILEGE");

    TestUtils.logout();
  }

  @Test
  public void uploadCompletedPdfReturnsPersistedApplicationIdForCreateAndReplaceTest() {
    String orgName = "Completed Upload Org";
    EntityFactory.createOrganization().withOrgName(orgName).buildAndPersist(orgDao);

    String admin = "completedUploadAdmin";
    String pass = "samePassword1!";
    EntityFactory.createUser()
        .withUsername(admin)
        .withPasswordToHash(pass)
        .withOrgName(orgName)
        .withUserType(UserType.Admin)
        .buildAndPersist(userDao);

    TestUtils.login(admin, pass);
    uploadOrgDocumentPDF();

    String nonApplicationFileId =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-files")
                    .body(new JSONObject().put("fileType", "ORG_DOCUMENT").toString())
                    .asString()
                    .getBody())
            .getJSONArray("documents")
            .getJSONObject(0)
            .getString("id");

    java.io.File examplePDF =
        new java.io.File(
            java.nio.file.Paths.get("").toAbsolutePath().toString()
                + java.io.File.separator
                + "src"
                + java.io.File.separator
                + "test"
                + java.io.File.separator
                + "resources"
                + java.io.File.separator
                + "CIS_401_Final_Progress_Report.pdf");

    HttpResponse<String> createResp =
        Unirest.post(TestUtils.getServerUrl() + "/upload-completed-pdf-2")
            .header("Content-Disposition", "attachment")
            .field("applicationId", nonApplicationFileId)
            .field("formAnswers", new JSONObject().put("fieldA", "valueA").toString())
            .field("clientUsername", "")
            .field("file", examplePDF, "application/pdf")
            .asString();
    JSONObject createJson = TestUtils.responseStringToJSON(createResp.getBody());
    assertThat(createJson.getString("status")).isEqualTo("SUCCESS");
    assertThat(createJson.has("applicationId")).isTrue();
    assertThat(createJson.has("fileId")).isTrue();
    String persistedApplicationId = createJson.getString("applicationId");
    assertThat(createJson.getString("fileId")).isEqualTo(persistedApplicationId);
    assertThat(persistedApplicationId).isNotEqualTo(nonApplicationFileId);

    HttpResponse<String> replaceResp =
        Unirest.post(TestUtils.getServerUrl() + "/upload-completed-pdf-2")
            .header("Content-Disposition", "attachment")
            .field("applicationId", persistedApplicationId)
            .field("formAnswers", new JSONObject().put("fieldA", "valueB").toString())
            .field("clientUsername", "")
            .field("file", examplePDF, "application/pdf")
            .asString();
    JSONObject replaceJson = TestUtils.responseStringToJSON(replaceResp.getBody());
    assertThat(replaceJson.getString("status")).isEqualTo("SUCCESS");
    assertThat(replaceJson.getString("applicationId")).isEqualTo(persistedApplicationId);
    assertThat(replaceJson.getString("fileId")).isEqualTo(persistedApplicationId);

    TestUtils.logout();
  }

  @Test
  public void packetStartsAbsentAndIsPersistedOnFirstAttachTest() {
    String orgName = "Packet Bootstrap Org";
    EntityFactory.createOrganization().withOrgName(orgName).buildAndPersist(orgDao);

    String admin = "packetBootstrapAdmin";
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
    String orgDocId =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-files")
                    .body(new JSONObject().put("fileType", "ORG_DOCUMENT").toString())
                    .asString()
                    .getBody())
            .getJSONArray("documents")
            .getJSONObject(0)
            .getString("id");

    JSONObject beforeAttachPacketJson =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-packet-for-application")
                    .body(new JSONObject().put("applicationId", applicationId).toString())
                    .asString()
                    .getBody());
    assertThat(beforeAttachPacketJson.getString("status")).isEqualTo("SUCCESS");
    boolean hasAttachmentPartBefore = false;
    if (!beforeAttachPacketJson.isNull("packet")) {
      JSONArray beforeParts = beforeAttachPacketJson.getJSONObject("packet").getJSONArray("parts");
      for (int i = 0; i < beforeParts.length(); i += 1) {
        if ("ORG_ATTACHMENT".equals(beforeParts.getJSONObject(i).getString("partType"))) {
          hasAttachmentPartBefore = true;
        }
      }
    }
    assertThat(hasAttachmentPartBefore).isFalse();

    JSONObject attachJson =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part")
                    .body(new JSONObject().put("applicationId", applicationId).put("fileId", orgDocId).toString())
                    .asString()
                    .getBody());
    assertThat(attachJson.getString("status")).isEqualTo("SUCCESS");
    assertThat(attachJson.getJSONObject("packet").getJSONArray("parts").length()).isEqualTo(2);

    JSONObject afterAttachPacketJson =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-packet-for-application")
                    .body(new JSONObject().put("applicationId", applicationId).toString())
                    .asString()
                    .getBody());
    assertThat(afterAttachPacketJson.getString("status")).isEqualTo("SUCCESS");
    JSONArray parts = afterAttachPacketJson.getJSONObject("packet").getJSONArray("parts");
    assertThat(parts.length()).isEqualTo(2);
    assertThat(parts.getJSONObject(1).getString("sourceFileId")).isEqualTo(orgDocId);

    TestUtils.logout();
  }

  @Test
  public void attachPacketPartFailsForUnknownApplicationWithoutCreatingPacketTest() {
    String orgName = "Packet Attach Failure Org";
    EntityFactory.createOrganization().withOrgName(orgName).buildAndPersist(orgDao);

    String admin = "packetAttachFailureAdmin";
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

    String realApplicationId =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-files")
                    .body(new JSONObject().put("fileType", "APPLICATION_PDF").toString())
                    .asString()
                    .getBody())
            .getJSONArray("documents")
            .getJSONObject(0)
            .getString("id");
    String orgDocId =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-files")
                    .body(new JSONObject().put("fileType", "ORG_DOCUMENT").toString())
                    .asString()
                    .getBody())
            .getJSONArray("documents")
            .getJSONObject(0)
            .getString("id");

    String unknownApplicationId = new ObjectId().toString();
    JSONObject failedAttachJson =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part")
                    .body(
                        new JSONObject()
                            .put("applicationId", unknownApplicationId)
                            .put("fileId", orgDocId)
                            .toString())
                    .asString()
                    .getBody());
    assertThat(failedAttachJson.getString("status")).isEqualTo("NO_SUCH_FILE");

    JSONObject realApplicationPacketJson =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-packet-for-application")
                    .body(new JSONObject().put("applicationId", realApplicationId).toString())
                    .asString()
                    .getBody());
    assertThat(realApplicationPacketJson.getString("status")).isEqualTo("SUCCESS");
    boolean hasAttachmentPart = false;
    if (!realApplicationPacketJson.isNull("packet")) {
      JSONArray realApplicationParts =
          realApplicationPacketJson.getJSONObject("packet").getJSONArray("parts");
      for (int i = 0; i < realApplicationParts.length(); i += 1) {
        if ("ORG_ATTACHMENT".equals(realApplicationParts.getJSONObject(i).getString("partType"))) {
          hasAttachmentPart = true;
        }
      }
    }
    assertThat(hasAttachmentPart).isFalse();

    TestUtils.logout();
  }

  /**
   * End-to-end smoke test for the new render endpoint. Uploads a base PDF, attaches an org doc,
   * then verifies the merged response is real PDF bytes whose page count exceeds the base alone
   * (proving attachments are being concatenated for the same code path the client now uses for
   * Print/Download).
   */
  @Test
  public void renderApplicationPacketReturnsMergedPdfBytesTest() throws Exception {
    String orgName = "Render Packet Org";
    EntityFactory.createOrganization().withOrgName(orgName).buildAndPersist(orgDao);

    String admin = "renderPacketAdmin";
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
    String orgDocId =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/get-files")
                    .body(new JSONObject().put("fileType", "ORG_DOCUMENT").toString())
                    .asString()
                    .getBody())
            .getJSONArray("documents")
            .getJSONObject(0)
            .getString("id");

    // Capture page counts of the base + attachment so we can assert the merge happened.
    HttpResponse<byte[]> baseResp =
        Unirest.post(TestUtils.getServerUrl() + "/render-application-packet")
            .field("applicationId", applicationId)
            .asBytes();
    assertThat(baseResp.getStatus()).isEqualTo(200);
    assertThat(baseResp.getHeaders().getFirst("Content-Type")).contains("application/pdf");
    int baseOnlyPages =
        org.apache.pdfbox.Loader.loadPDF(baseResp.getBody()).getNumberOfPages();
    assertThat(baseOnlyPages).isGreaterThan(0);

    // Now attach the org doc and re-render. Page count must increase (attachment got merged).
    JSONObject attachJson =
        TestUtils
            .responseStringToJSON(
                Unirest.post(TestUtils.getServerUrl() + "/attach-packet-part")
                    .body(
                        new JSONObject()
                            .put("applicationId", applicationId)
                            .put("fileId", orgDocId)
                            .toString())
                    .asString()
                    .getBody());
    assertThat(attachJson.getString("status")).isEqualTo("SUCCESS");

    HttpResponse<byte[]> mergedResp =
        Unirest.post(TestUtils.getServerUrl() + "/render-application-packet")
            .field("applicationId", applicationId)
            .asBytes();
    assertThat(mergedResp.getStatus()).isEqualTo(200);
    assertThat(mergedResp.getHeaders().getFirst("Content-Type")).contains("application/pdf");
    int mergedPages =
        org.apache.pdfbox.Loader.loadPDF(mergedResp.getBody()).getNumberOfPages();
    assertThat(mergedPages).isGreaterThan(baseOnlyPages);

    TestUtils.logout();
  }

  /**
   * Verifies the {@code mainPdf} multipart override path: a single-page synthetic PDF passed as
   * the override should produce a render whose first part has 1 page (override page count),
   * regardless of how many pages the stored application PDF has.
   */
  @Test
  public void renderApplicationPacketHonorsMainPdfOverrideTest() throws Exception {
    String orgName = "Render Override Org";
    EntityFactory.createOrganization().withOrgName(orgName).buildAndPersist(orgDao);

    String admin = "renderOverrideAdmin";
    String pass = "samePassword1!";
    EntityFactory.createUser()
        .withUsername(admin)
        .withPasswordToHash(pass)
        .withOrgName(orgName)
        .withUserType(UserType.Admin)
        .buildAndPersist(userDao);

    TestUtils.login(admin, pass);
    uploadTestPDF();

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

    int storedPageCount;
    {
      HttpResponse<byte[]> storedResp =
          Unirest.post(TestUtils.getServerUrl() + "/render-application-packet")
              .field("applicationId", applicationId)
              .asBytes();
      assertThat(storedResp.getStatus()).isEqualTo(200);
      storedPageCount =
          org.apache.pdfbox.Loader.loadPDF(storedResp.getBody()).getNumberOfPages();
    }

    // Build a synthetic 1-page PDF in memory and feed it as the override.
    byte[] overrideBytes;
    try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
      doc.addPage(
          new org.apache.pdfbox.pdmodel.PDPage(
              org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER));
      java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
      doc.save(out);
      overrideBytes = out.toByteArray();
    }
    java.io.File overrideFile = java.io.File.createTempFile("override-", ".pdf");
    overrideFile.deleteOnExit();
    java.nio.file.Files.write(overrideFile.toPath(), overrideBytes);

    HttpResponse<byte[]> overrideResp =
        Unirest.post(TestUtils.getServerUrl() + "/render-application-packet")
            .field("applicationId", applicationId)
            .field("mainPdf", overrideFile, "application/pdf")
            .asBytes();
    assertThat(overrideResp.getStatus()).isEqualTo(200);
    int overridePageCount =
        org.apache.pdfbox.Loader.loadPDF(overrideResp.getBody()).getNumberOfPages();
    assertThat(overridePageCount).isEqualTo(1);

    // Sanity: the override actually changed something. If the stored PDF was already 1 page this
    // assertion is vacuous, so guard it.
    if (storedPageCount != 1) {
      assertThat(overridePageCount).isNotEqualTo(storedPageCount);
    }

    TestUtils.logout();
  }
}
