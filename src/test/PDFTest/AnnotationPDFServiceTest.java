package PDFTest;

import static PDFTest.PDFTestUtils.*;
import static TestUtils.EntityFactory.createUser;
import static TestUtils.TestUtils.getFieldValues;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import PDF.PDFType;
import PDF.PdfController;
import TestUtils.TestUtils;
import User.User;
import User.UserType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;

@Ignore("Deprecated: v1 endpoints (/upload, /get-application-questions, /fill-application) were "
    + "removed. Replaced by PDFV2Test suite.")
public class AnnotationPDFServiceTest {
  private UserDao userDao;

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
  }

  @Before
  public void initialize() {
    userDao = UserDaoFactory.create(DeploymentLevel.TEST);
  }

  @After
  public void reset() {
    if(this.userDao != null) {
      this.userDao.clear();
    }
    try {
      TestUtils.logout();
    } catch (Exception e) {
      // Ignore – server may already be stopped or no session active
    }
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  // ------------------ GET QUESTIONS TESTS ------------------------ //
  @Test
  public void getApplicationQuestionsBirthCertificateTest()
      throws IOException, GeneralSecurityException {
    String username = "username";
    String password = "password";
    User user =
        createUser()
            .withUserType(UserType.Admin)
            .withUsername(username)
            .withPasswordToHash(password)
            .buildAndPersist(userDao);
    TestUtils.login(username, password);
    // when running entire file, other documents interfere with retrieving the form.
    // clearAllDocuments();

    File applicationPDF =
        new File(
            resourcesFolderPath
                + File.separator
                + "AnnotatedPDFs"
                + File.separator
                + "Application_for_a_Birth_Certificate_Annotated.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    body.put("clientUsername", "");
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());
    JSONArray fields = applicationsQuestionsResponseJSON.getJSONArray("fields");
    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");

    // Todo: We Need To Read the Correct Annotation from a CSV File
    JSONArray correctFields = PDFTestUtils.csvToJSON("Test_Pennsylvania_Birth_Certificate.csv");
    // this equality is broken right now
    //    PDFTestUtils.checkFieldsEquality(correctFields, fields);
    TestUtils.logout();
  }

  @Test
  public void getApplicationQuestionsSocialSecurityCardTest()
      throws IOException, GeneralSecurityException {
    String username = "username";
    String password = "password";
    User user =
        createUser()
            .withUserType(UserType.Admin)
            .withUsername(username)
            .withPasswordToHash(password)
            .buildAndPersist(userDao);
    TestUtils.login(username, password);

    File applicationPDF = new File(resourcesFolderPath + File.separator + "SSAPP_DELETE.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    body.put("clientUsername", "");
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());

    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("ANNOTATION_ERROR");
    assertThat(applicationsQuestionsResponseJSON.getString("message"))
        .isEqualTo("Field Directive not Understood for Field '1:First Name:firstName'");
    // delete(fileId, "BLANK_FORM");
    TestUtils.logout();
  }

  @Test
  public void getApplicationQuestionBlankPDFTest() throws IOException, GeneralSecurityException {
    String username = "username";
    String password = "password";
    User user =
        createUser()
            .withUserType(UserType.Admin)
            .withUsername(username)
            .withPasswordToHash(password)
            .buildAndPersist(userDao);
    TestUtils.login(username, password);

    File applicationDocx =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");
    String fileId = uploadFileAndGetFileId(applicationDocx, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    body.put("clientUsername", "");
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());

    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("INVALID_PDF");
    //    assertThat(applicationsQuestionsResponseJSON.getJSONArray("fields").toString())
    //        .isEqualTo(new JSONArray().toString());
    // delete(fileId, "BLANK_FORM");
    TestUtils.logout();
  }

  @Test
  public void getApplicationQuestionMetadataTest() throws IOException, GeneralSecurityException {
    // Test to get application with metadata
    User user =
        createUser()
            .withUserType(UserType.Admin)
            .withUsername(username)
            .withPasswordToHash(password)
            .buildAndPersist(userDao);
    TestUtils.login(username, password);

    File applicationPDF =
        new File(resourcesFolderPath + File.separator + "Application_for_a_Birth_Certificate.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    body.put("clientUsername", "");
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());
    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");
    assertEquals("Untitled", applicationsQuestionsResponseJSON.get("title"));
  }

  @Test
  public void getApplicationQuestionsCaseWorkerTest() throws IOException, GeneralSecurityException {
    String caseWorkerUsername = "username1";
    String caseWorkerPassword = "password1";
    String clientUsername = "username2";
    String clientPassword = "password2";
    String organization = "org1";
    // Case worker
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(caseWorkerUsername)
        .withPasswordToHash(caseWorkerPassword)
        .withOrgName(organization)
        .buildAndPersist(userDao);
    TestUtils.login(caseWorkerUsername, caseWorkerPassword);
    // Client
    createUser()
        .withUserType(UserType.Client)
        .withUsername(clientUsername)
        .withPasswordToHash(clientPassword)
        .withOrgName(organization)
        .buildAndPersist(userDao);

    File applicationPDF =
        new File(resourcesFolderPath + File.separator + "Application_for_a_Birth_Certificate.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    body.put("clientUsername", clientUsername);
    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject responseJSON = TestUtils.responseStringToJSON(response.getBody());
    assertThat(responseJSON.getString("status")).isEqualTo("SUCCESS");
    assertEquals("Untitled", responseJSON.get("title"));
  }

  // ------------------ FILL COMPLETED_APPLICATION TESTS ------------------------ //

  @Test
  public void fillApplicationQuestionsBirthCertificateTest()
      throws IOException, GeneralSecurityException {
    String username = "username2";
    String password = "password";
    User user =
        createUser()
            .withUserType(UserType.Admin)
            .withUsername(username)
            .withPasswordToHash(password)
            .buildAndPersist(userDao);
    TestUtils.login(username, password);

    File applicationPDF = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    body.put("clientUsername", "");
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());
    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");

    JSONObject formAnswers = getFormAnswersTestPDFForm(applicationsQuestionsResponseJSON);
    // fill out form
    body = new JSONObject();
    body.put("applicationId", fileId);
    body.put("formAnswers", formAnswers);
    body.put("clientUsername", "");
    HttpResponse<File> filledForm =
        Unirest.post(TestUtils.getServerUrl() + "/fill-application")
            .body(body.toString())
            .asFile(resourcesFolderPath + File.separator + "ss-5_filled_out.pdf");
    assertThat(filledForm.getStatus()).isEqualTo(200);

    // check if all fields are filled
    JSONObject fieldValues = null;
    try {
      File filled_out_pdf = new File(resourcesFolderPath + File.separator + "ss-5_filled_out.pdf");
      PDDocument pdf = Loader.loadPDF(filled_out_pdf);
      fieldValues = getFieldValues(new FileInputStream(filled_out_pdf));
    } catch (IOException e) {
      assertThat(false).isTrue();
    }
    assertThat(fieldValues).isNotNull();
    // checkFormAnswersSS5Form(fieldValues);
    // delete(fileId, "BLANK_FORM");
    TestUtils.logout();
  }

  @Test
  public void fillApplicationQuestionsSS5Test() throws IOException, GeneralSecurityException {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    //    clearAllDocumentsForUser(username, password);

    File applicationPDF = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    body.put("clientUsername", "");
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());
    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");

    JSONObject formAnswers = getFormAnswersTestPDFForm(applicationsQuestionsResponseJSON);
    // fill out form
    body = new JSONObject();
    body.put("applicationId", fileId);
    body.put("formAnswers", formAnswers);
    body.put("clientUsername", "");
    HttpResponse<File> filledForm =
        Unirest.post(TestUtils.getServerUrl() + "/fill-application")
            .body(body.toString())
            .asFile(resourcesFolderPath + File.separator + "ss-5_filled_out.pdf");
    assertThat(filledForm.getStatus()).isEqualTo(200);

    // check if all fields are filled
    JSONObject fieldValues = null;
    try {
      File filled_out_pdf = new File(resourcesFolderPath + File.separator + "ss-5_filled_out.pdf");
      PDDocument pdf = Loader.loadPDF(filled_out_pdf);
      fieldValues = getFieldValues(new FileInputStream(filled_out_pdf));
    } catch (IOException e) {
      assertThat(false).isTrue();
    }
    assertThat(fieldValues).isNotNull();
    // checkFormAnswersSS5Form(fieldValues);
    // delete(fileId, "BLANK_FORM");
    TestUtils.logout();
  }

  @Test
  public void fillApplicationQuestionsCaseWorkerTest()
      throws IOException, GeneralSecurityException {
    String caseWorkerUsername = "username1";
    String caseWorkerPassword = "password1";
    String clientUsername = "username2";
    String clientPassword = "password2";
    String organization = "org1";
    // Case worker
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(caseWorkerUsername)
        .withPasswordToHash(caseWorkerPassword)
        .withOrgName(organization)
        .buildAndPersist(userDao);
    TestUtils.login(caseWorkerUsername, caseWorkerPassword);
    // Client
    createUser()
        .withUserType(UserType.Client)
        .withUsername(clientUsername)
        .withPasswordToHash(clientPassword)
        .withOrgName(organization)
        .buildAndPersist(userDao);

    File applicationPDF = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    body.put("clientUsername", clientUsername);
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());
    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");

    JSONObject formAnswers = getFormAnswersTestPDFForm(applicationsQuestionsResponseJSON);
    // fill out form
    body = new JSONObject();
    body.put("applicationId", fileId);
    body.put("formAnswers", formAnswers);
    body.put("clientUsername", clientUsername);
    HttpResponse<File> filledForm =
        Unirest.post(TestUtils.getServerUrl() + "/fill-application")
            .body(body.toString())
            .asFile(resourcesFolderPath + File.separator + "ss-5_filled_out.pdf");
    assertThat(filledForm.getStatus()).isEqualTo(200);

    // check if all fields are filled
    JSONObject fieldValues = null;
    try {
      File filled_out_pdf = new File(resourcesFolderPath + File.separator + "ss-5_filled_out.pdf");
      PDDocument pdf = Loader.loadPDF(filled_out_pdf);
      fieldValues = getFieldValues(new FileInputStream(filled_out_pdf));
    } catch (IOException e) {
      assertThat(false).isTrue();
    }
    assertThat(fieldValues).isNotNull();
    // checkFormAnswersSS5Form(fieldValues);
    // delete(fileId, "BLANK_FORM");
    TestUtils.logout();
  }

  // ------------------ UPLOAD SIGNED PDF TESTS ------------------------ //\

  @Test
  public void uploadSignedSSPDF() throws IOException, GeneralSecurityException {
    String caseWorkerUsername = "username1";
    String caseWorkerPassword = "password1";
    String clientUsername = "username2";
    String clientPassword = "password2";
    String organization = "org1";
    // Case worker
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(caseWorkerUsername)
        .withPasswordToHash(caseWorkerPassword)
        .withOrgName(organization)
        .buildAndPersist(userDao);
    TestUtils.login(caseWorkerUsername, caseWorkerPassword);
    // Client
    createUser()
        .withUserType(UserType.Client)
        .withUsername(clientUsername)
        .withPasswordToHash(clientPassword)
        .withOrgName(organization)
        .buildAndPersist(userDao);
    String filledApplicationPDFFilePath =
        resourcesFolderPath + File.separator + "ss-5_filled_out.pdf";
    File filledApplicationPDF = new File(filledApplicationPDFFilePath);
    String mimeTypePDF = Files.probeContentType(Path.of(filledApplicationPDFFilePath));
    String signatureImagePath = resourcesFolderPath + File.separator + "sample-signature.png";
    File signatureImage = new File(signatureImagePath);
    String mimeTypeSignature = Files.probeContentType(Path.of(signatureImagePath));
    HttpResponse<String> uploadSignedPDFResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload-signed-pdf")
            .header("Content-Disposition", "attachment")
            .field("file", filledApplicationPDF, mimeTypePDF)
            .field("pdfType", Collections.singleton(PDFType.COMPLETED_APPLICATION))
            .field("signature", signatureImage, mimeTypeSignature)
            .field("clientUsername", clientUsername)
            .asString();
    JSONObject responseJSON = TestUtils.responseStringToJSON(uploadSignedPDFResponse.getBody());
    System.out.println(responseJSON);
    assertThat(responseJSON.getString("status")).isEqualTo("SUCCESS");
    TestUtils.logout();
  }

  // ------------------ GET METADATA TESTS ------------------------ //

  @Test // Test with title embedded in document
  public void getPDFTitleTest1() throws IOException {
    String fileName = "Application_for_a_Birth_Certificate.pdf";
    File applicationPDF = new File(resourcesFolderPath + File.separator + fileName);
    InputStream pdfDocument = FileUtils.openInputStream(applicationPDF);
    assertEquals(
        "Application_for_a_Birth_Certificate.pdf",
        PdfController.getPDFTitle(fileName, pdfDocument, PDFType.BLANK_FORM));
  }

  @Test // Test without any title in document
  public void getPDFTitleTest2() throws IOException {
    String fileName = "library-card-application.pdf";
    File applicationPDF = new File(resourcesFolderPath + File.separator + fileName);
    InputStream pdfDocument = FileUtils.openInputStream(applicationPDF);
    assertEquals(fileName, PdfController.getPDFTitle(fileName, pdfDocument, PDFType.BLANK_FORM));
  }
}
