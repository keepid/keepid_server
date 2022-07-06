package PDFTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import Security.EncryptionUtils;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.User;
import User.UserType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

import static PDFTest.PdfControllerIntegrationTestHelperMethods.delete;
import static PDFTest.PdfControllerIntegrationTestHelperMethods.uploadFileAndGetFileId;
import static org.assertj.core.api.Assertions.assertThat;

public class PdfControllerIntegrationTarget {

  public static UserDao userDao;

  private static EncryptionUtils encryptionUtils;
  public static String username = "adminBSM";

  public static String currentPDFFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "PDFTest";

  public static String resourcesFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources";

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
    userDao = UserDaoFactory.create(DeploymentLevel.TEST);
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  // Test copied from previous file
  @Test
  public void getDocumentsTargetUser() throws IOException, GeneralSecurityException {

    String clientUsername = "clientclient123456";

    User client =
        EntityFactory.createUser()
            .withUsername(clientUsername)
            .withUserType(UserType.Client)
            .withPassword(clientUsername)
            .withOrgName("keepid")
            .withFirstName("client")
            .withLastName("client")
            .buildAndPersist(userDao);

    String workerUsername = "workerworker123456";
    User worker =
        EntityFactory.createUser()
            .withUsername(workerUsername)
            .withUserType(UserType.Worker)
            .withPassword(workerUsername)
            .withOrgName("keepid")
            .withFirstName("worker")
            .withLastName("worker")
            .buildAndPersist(userDao);

    clearAllDocuments(worker, client);

    TestUtils.login(client.getUsername(), client.getPassword());
    File applicationPDF = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "FORM");
    TestUtils.logout();

    TestUtils.login(worker.getUsername(), worker.getPassword());

    JSONObject body = new JSONObject();
    body.put("pdfType", "FORM");
    body.put("annotated", false);
    body.put("targetUser", client.getUsername());
    HttpResponse<String> getForm =
        Unirest.post(TestUtils.getServerUrl() + "/get-documents").body(body.toString()).asString();
    JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());
    String downId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
    // String fileId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
    assertThat(fileId).isEqualTo(downId);
    TestUtils.logout();
  }

  @Test
  public void getDocumentsTargetUserWorker() throws IOException, GeneralSecurityException {

    String clientUsername = "clientclient123456";
    User client =
        EntityFactory.createUser()
            .withUsername(clientUsername)
            .withUserType(UserType.Client)
            .withPassword(clientUsername)
            .withOrgName("keepid")
            .withFirstName("client")
            .withLastName("client")
            .buildAndPersist(userDao);
    // .buildAndPersist(UserDaoFactory.create(DeploymentLevel.TEST));

    String workerUsername = "workerworker123456";
    User worker =
        EntityFactory.createUser()
            .withUsername(workerUsername)
            .withUserType(UserType.Worker)
            .withPassword(workerUsername)
            .withOrgName("keepid")
            .withFirstName("worker")
            .withLastName("worker")
            .buildAndPersist(userDao);
    // .buildAndPersist(UserDaoFactory.create(DeploymentLevel.TEST));

    clearAllDocuments(worker, client);

    TestUtils.login(client.getUsername(), client.getPassword());
    File applicationPDF = new File(resourcesFolderPath + File.separator + "downloaded_form.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "IDENTIFICATION");
    TestUtils.logout();

    TestUtils.login(worker.getUsername(), worker.getPassword());

    JSONObject body = new JSONObject();
    body.put("targetUser", "client1YMCA");
    HttpResponse<String> getForm =
        Unirest.post(TestUtils.getServerUrl() + "/get-documents").body(body.toString()).asString();
    JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());
    String downId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
    TestUtils.logout();

    assertThat(fileId).isEqualTo(downId);
  }

  // @Test
  // public void getDocumentsTargetUserClearAll() throws IOException, GeneralSecurityException {
  //   TestUtils.login("workertttYMCA", "workertttYMCA");
  //   File applicationPDF = new File(resourcesFolderPath + File.separator + "downloaded_form.pdf");
  //   String fileId = uploadFileAndGetFileId(applicationPDF, "FORM");
  //   TestUtils.logout();
  //   TestUtils.login("workertttYMCA", "workertttYMCA");
  //
  //   JSONObject body = new JSONObject();
  //   body.put("pdfType", "FORM");
  //   body.put("annotated", false);
  //   body.put("targetUser", "client1YMCA");
  //   HttpResponse<String> getForm =
  //       Unirest.post(TestUtils.getServerUrl() +
  // "/get-documents").body(body.toString()).asString();
  //   JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());
  //   String downId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
  //   // String fileId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
  //   assertThat(fileId).isEqualTo(downId);
  //   TestUtils.logout();
  // }
  //
  // @Test
  // public void uploadDocumentsTargetUserWorker() throws IOException, GeneralSecurityException {
  //   TestUtils.login("workertttYMCA", "workertttYMCA");
  //   File applicationPDF = new File(resourcesFolderPath + File.separator + "downloaded_form.pdf");
  //   String fileId = uploadFileAndGetFileIdTarget(applicationPDF, "FORM", "client1YMCA");
  //   TestUtils.logout();
  //   TestUtils.login("client1YMCA", "client1YMCA");
  //
  //   JSONObject body = new JSONObject();
  //   body.put("pdfType", "FORM");
  //   body.put("annotated", false);
  //   body.put("targetUser", "client1YMCA");
  //   HttpResponse<String> getForm =
  //       Unirest.post(TestUtils.getServerUrl() +
  // "/get-documents").body(body.toString()).asString();
  //   JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());
  //   String downId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
  //   // String fileId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
  //   assertThat(fileId).isEqualTo(downId);
  //   TestUtils.logout();
  // }
  //
  // @Test
  // public void getDocsOfTarget() throws GeneralSecurityException, IOException {
  //   TestUtils.login("client1YMCA", "client1YMCA");
  //   File applicationPDF = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
  //   String fileId = uploadFileAndGetFileIdTarget(applicationPDF, "FORM", "client1YMCA");
  //   TestUtils.logout();
  //
  //   TestUtils.login("workertttYMCA", "workertttYMCA");
  //   assertThat(fileId).isEqualTo(getDocumentsTarget("client1YMCA"));
  // }
  //
  // @Test
  // public void deleteDocsTarget() throws GeneralSecurityException, IOException {
  //
  //   TestUtils.login("workertttYMCA", "workertttYMCA");
  //   File applicationPDF = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
  //   String fileId = uploadFileAndGetFileIdTarget(applicationPDF, "FORM", "client1YMCA");
  //   TestUtils.logout();
  //
  //   TestUtils.login("client1YMCA", "client1YMCA");
  //   TestUtils.login("workertttYMCA", "workertttYMCA");
  //   assertThat(fileId).isEqualTo(getDocumentsTarget("client1YMCA"));
  // }
  //
  // // should not allow a worker out of the organization to view or upload documents on behalf of a
  // // different client
  // @Test
  // public void getDocumentsWorkerOutOfOrg() throws IOException, GeneralSecurityException {
  //   TestUtils.login("client1YMCA", "client1YMCA");
  //   File applicationPDF = new File(resourcesFolderPath + File.separator + "downloaded_form.pdf");
  //   String fileId = uploadFileAndGetFileId(applicationPDF, "FORM");
  //   TestUtils.logout();
  //   TestUtils.login("workertttBSM", "workertttBSM");
  //
  //   JSONObject body = new JSONObject();
  //   body.put("pdfType", "FORM");
  //   body.put("annotated", false);
  //   body.put("targetUser", "client1YMCA");
  //   HttpResponse<String> getForm =
  //       Unirest.post(TestUtils.getServerUrl() +
  // "/get-documents").body(body.toString()).asString();
  //   JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());
  //   String downId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
  //   // String fileId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
  //   assertThat(fileId).isEqualTo(downId);
  //   assertThat(getForm.getStatus()).isEqualTo(401);
  //   TestUtils.logout();
  // }
  //
  @BeforeEach
  public static void setUpTarget() {
    userDao = UserDaoFactory.create(DeploymentLevel.TEST);
  }

  @BeforeEach
  public static void clearAllDocuments(User worker, User targetUser)
      throws IOException, GeneralSecurityException {
    TestUtils.login(worker.getUsername(), worker.getPassword());
    String[] pdfTypes = {"FORM", "FORM", "APPLICATION", "IDENTIFICATION"};
    boolean[] annotated = {false, true, false, false};
    for (int j = 0; j < pdfTypes.length; j++) {
      JSONObject body = new JSONObject();
      body.put("pdfType", pdfTypes[j]);
      body.put("annotated", annotated[j]);
      body.put("targetUser", targetUser.getUsername());
      HttpResponse<String> getAllDocuments =
          Unirest.post(TestUtils.getServerUrl() + "/get-documents")
              .body(body.toString())
              .asString();
      JSONObject getAllDocumentsJSON = TestUtils.responseStringToJSON(getAllDocuments.getBody());
      assertThat(getAllDocumentsJSON.getString("status")).isEqualTo("SUCCESS");

      JSONArray arr = getAllDocumentsJSON.getJSONArray("documents");
      System.out.println(arr);
      for (int i = 0; i < arr.length(); i++) {
        String fileId = arr.getJSONObject(i).getString("id");
        delete(fileId, pdfTypes[j]);
      }
    }
    TestUtils.logout();
  }

  @AfterEach
  public static void tearDownTarget() {
    userDao.clear();
  }
}
