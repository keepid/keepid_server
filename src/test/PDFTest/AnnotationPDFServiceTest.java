package PDFTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import PDF.PDFType;
import PDF.PdfController;
import TestUtils.TestUtils;
import User.UserType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import static PDFTest.PDFTestUtils.*;
import static TestUtils.EntityFactory.createUser;
import static TestUtils.TestUtils.getFieldValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class AnnotationPDFServiceTest {
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

  // ------------------ GET QUESTIONS TESTS ------------------------ //
  @Test
  public void getApplicationQuestionsBirthCertificateTest()
      throws IOException, GeneralSecurityException {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    // when running entire file, other documents interfere with retrieving the form.
    clearAllDocuments();

    File applicationPDF =
        new File(resourcesFolderPath + File.separator + "Pennsylvania_Birth_Certificate_Form.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
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
    PDFTestUtils.checkFieldsEquality(correctFields, fields);

    TestUtils.logout();
  }

  @Test
  public void getApplicationQuestionsSocialSecurityCardTest()
      throws IOException, GeneralSecurityException {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    // when running entire file, other documents interfere with retrieving the form.
    clearAllDocuments();

    File applicationPDF = new File(resourcesFolderPath + File.separator + "SSAPP_DELETE.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());

    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");

    // delete(fileId, "BLANK_FORM");
    TestUtils.logout();
  }

  @Test
  public void getApplicationQuestionBlankPDFTest() throws IOException, GeneralSecurityException {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    // when running entire file, other documents interfere with retrieving the form.
    clearAllDocuments();

    File applicationDocx =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");
    String fileId = uploadFileAndGetFileId(applicationDocx, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
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
    TestUtils.login(username, username);
    clearAllDocuments();

    File applicationPDF =
        new File(resourcesFolderPath + File.separator + "Pennsylvania_Birth_Certificate_Form.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());
    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");
    assertEquals(
        "Pennsylvania - Application for a Birth Certificate",
        applicationsQuestionsResponseJSON.get("title"));
    assertEquals(
        "An application for a birth certificate in the state of Pennsylvania. Requires a driver's license photocopy to apply",
        applicationsQuestionsResponseJSON.get("description"));
  }

  // ------------------ FILL COMPLETED_APPLICATION TESTS ------------------------ //

  @Test
  public void fillApplicationQuestionsBirthCertificateTest()
      throws IOException, GeneralSecurityException {
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    clearAllDocuments();

    File applicationPDF = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
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
    clearAllDocuments();

    File applicationPDF = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "BLANK_FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
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

  // ------------------ GET METADATA TESTS ------------------------ //

  @Test // Test with title embedded in document
  public void getPDFTitleTest1() throws IOException {
    String fileName = "Pennsylvania_Birth_Certificate_Form.pdf";
    File applicationPDF = new File(resourcesFolderPath + File.separator + fileName);
    InputStream pdfDocument = FileUtils.openInputStream(applicationPDF);
    assertEquals(
        "Pennsylvania - Application for a Birth Certificate",
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
