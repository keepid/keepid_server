package PDFTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import PDF.PDFType;
import TestUtils.TestUtils;
import User.UserType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import static PDFTest.PDFTestUtils.*;
import static TestUtils.EntityFactory.createUser;
import static org.assertj.core.api.Assertions.assertThat;

public class CrudPDFServiceTest {
  private UserDao userDao;

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
  }

  @Before
  public void initialize() {
    this.userDao = UserDaoFactory.create(DeploymentLevel.TEST);
  }

  @After
  public void reset() {
    this.userDao.clear();
    TestUtils.logout();
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  // ------------------ UPLOAD PDF TESTS ------------------------ //

  @Test
  public void uploadValidPDFTest() {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    uploadTestPDF();
  }

  @Test
  public void uploadFormTest() {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    uploadTestFormPDF();
    TestUtils.logout();
  }

  @Test
  public void uploadAnnotatedPDFFormTest() {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    uploadTestAnnotatedFormPDF();
  }

  @Test
  public void uploadImageToPDFTest() {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);

    File file = new File(resourcesFolderPath + File.separator + "1.png");
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
            .header("Content-Disposition", "attachment")
            .field("pdfType", PDFType.IDENTIFICATION_DOCUMENT)
            .field("file", file)
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  @Test
  public void uploadValidPDFTestExists() {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    uploadTestPDF();
    searchTestPDF();
  }

  public static JSONObject searchTestPDF() {
    JSONObject body = new JSONObject();
    body.put("pdfType", "COMPLETED_APPLICATION");
    HttpResponse<String> getAllDocuments =
        Unirest.post(TestUtils.getServerUrl() + "/get-documents").body(body.toString()).asString();
    JSONObject getAllDocumentsJSON = TestUtils.responseStringToJSON(getAllDocuments.getBody());
    assertThat(getAllDocumentsJSON.getString("status")).isEqualTo("SUCCESS");
    return getAllDocumentsJSON;
  }

  @Test
  public void uploadValidPDFTestExistsAndDelete() {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    uploadTestPDF();
    JSONObject allDocuments = searchTestPDF();
    String idString = allDocuments.getJSONArray("documents").getJSONObject(0).getString("id");
    // delete(idString);
    // @todo this test isn't finished
  }

  @Test
  public void uploadInvalidPDFTypeTest() {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    File examplePDF =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
            .field("pdfType", "")
            .header("Content-Disposition", "attachment")
            .field("file", examplePDF)
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("INVALID_PDF_TYPE");
    TestUtils.logout();
  }

  @Test
  public void uploadNullPDFTest() {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    File examplePDF = null;
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
            .header("Content-Disposition", "attachment")
            .field("pdfType", "COMPLETED_APPLICATION")
            .asString();

    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("INVALID_PDF");
    TestUtils.logout();
  }

  // Todo: Test for Docx

  // ------------------ DOWNLOAD PDF TESTS ------------------------ //

  @Test
  public void downloadTestFormTest() throws IOException, GeneralSecurityException {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    File testPdf = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
    String fileId = uploadFileAndGetFileId(testPdf, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("fileId", fileId);
    body.put("pdfType", "BLANK_FORM");
    HttpResponse<File> downloadFileResponse =
        Unirest.post(TestUtils.getServerUrl() + "/download")
            .body(body.toString())
            .asFile(resourcesFolderPath + File.separator + "downloaded_form.pdf");
    assertThat(downloadFileResponse.getStatus()).isEqualTo(200);
  }

  @Test
  public void downloadPDFTypeNullTest() throws IOException, GeneralSecurityException {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    File testPdf = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
    String fileId = uploadFileAndGetFileId(testPdf, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("fileId", fileId);
    body.put("pdfType", (String) null);
    HttpResponse downloadFileResponse =
        Unirest.post(TestUtils.getServerUrl() + "/download").body(body.toString()).asString();
    assertThat(downloadFileResponse.getStatus()).isEqualTo(500);
  }

  // ------------------ GET PDF TESTS ------------------------ //

  @Test
  public void getDocumentsTargetUser() throws IOException, GeneralSecurityException {
    createUser()
        .withUserType(UserType.Worker)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    createUser()
        .withUserType(UserType.Worker)
        .withUsername("workerttfBSM")
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    clearAllDocuments();
    File applicationPDF = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");
    TestUtils.logout();
    TestUtils.login(username, password);

    JSONObject body = new JSONObject();
    body.put("pdfType", "BLANK_FORM");
    body.put("annotated", false);
    body.put("targetUser", "workerttfBSM");
    HttpResponse<String> getForm =
        Unirest.post(TestUtils.getServerUrl() + "/get-documents").body(body.toString()).asString();
    JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());
    String downId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
    assertThat(fileId).isEqualTo(downId);
    TestUtils.logout();
  }
}