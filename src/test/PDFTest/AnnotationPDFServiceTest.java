package PDFTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import PDF.PDFType;
import PDF.PdfController;
import TestUtils.TestUtils;
import User.User;
import User.UserType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.LinkedList;

import static PDFTest.PDFTestUtils.*;
import static TestUtils.EntityFactory.createUser;
import static TestUtils.TestUtils.getFieldValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
    String fileId = uploadFileAndGetFileId(applicationPDF, "FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());

    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");

    // Todo: We Need To Read the Correct Annotation from a CSV File

    // comb through JSON for each field, to see if it is there.
    LinkedList<String[][]> fieldsToCheck = new LinkedList<String[][]>();
    // each array has format {{fieldType}, {fieldName} {fieldValueOptions}}
    String[][] street = {{"TextField"}, {"Street"}, {}};
    String[][] intended_use = {
      {"RadioButton"},
      {"Intended_use"},
      {
        "Choice1-travel/passport",
        "Choice2-school",
        "Choice3-drivers_license",
        "Choice4-social security/benefits",
        "Choice5-dual citizenship",
        "Choice6-employment",
        "Choice7-other-specify"
      }
    };
    String[][] city = {{"TextField"}, {"City"}, {}};
    String[][] email_address = {{"TextField"}, {"Email_Address"}, {}};
    String[][] relationship = {
      {"ComboBox"},
      {"Relationship_Dropdown"},
      {
        " ",
        "Self",
        "Mother",
        "Father",
        "Brother",
        "Daughter",
        "Grandchild",
        "Grandparent",
        "Sister",
        "Spouse",
        "Son",
        "Other - "
      }
    };
    fieldsToCheck.add(street);
    fieldsToCheck.add(intended_use);
    fieldsToCheck.add(city);
    fieldsToCheck.add(email_address);
    fieldsToCheck.add(relationship);
    // checkForFields(applicationsQuestionsResponseJSON, fieldsToCheck);
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

    File applicationPDF = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());

    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");

    // delete(fileId, "FORM");
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
    String fileId = uploadFileAndGetFileId(applicationDocx, "FORM");

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
    // delete(fileId, "FORM");
    TestUtils.logout();
  }

  @Test
  public void getApplicationQuestionMetadataTest() throws IOException, GeneralSecurityException {
    // Test to get application with metadata
    TestUtils.login(username, username);
    clearAllDocuments();

    File applicationPDF =
        new File(resourcesFolderPath + File.separator + "Pennsylvania_Birth_Certificate_Form.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "FORM");

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

  @Test
  public void getApplicationQuestionsMatchedFieldsTest1()
      throws IOException, GeneralSecurityException {
    // Test simple matched fields in database
    User user =
        createUser()
            .withUserType(UserType.Admin)
            .withUsername(username)
            .withPasswordToHash(password)
            .buildAndPersist(userDao);
    TestUtils.login(username, password);
    // when running entire file, other documents interfere with retrieving the form.
    clearAllDocuments();

    File applicationPDF =
        new File(
            resourcesFolderPath + File.separator + "Ann_Too_Pennsylvania_Birth_Certificate.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());

    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");

    JSONArray fields = applicationsQuestionsResponseJSON.getJSONArray("fields");
    int i = 0;
    JSONObject matchedField = null;
    Iterator<Object> fieldIterator = fields.iterator();
    while (fieldIterator.hasNext()) {
      JSONObject JSONField = (JSONObject) fieldIterator.next();
      // An annotated field
      if (JSONField.getString("fieldName").equals("First Name:firstName")) {
        matchedField = JSONField;
      }
    }
    if (matchedField == null) {
      fail("No matched field found");
    } else {
      assertEquals(user.getFirstName(), matchedField.getString("fieldDefaultValue"));
      assertEquals(true, matchedField.getBoolean("fieldIsMatched"));
      assertEquals("TextField", matchedField.getString("fieldType"));
      assertEquals("Please Enter Your: First Name", matchedField.getString("fieldQuestion"));
    }
  }

  @Test
  public void getApplicationQuestionsMatchedFieldsTest2()
      throws IOException, GeneralSecurityException {
    // Test simple matched fields in database
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    // when running entire file, other documents interfere with retrieving the form.
    clearAllDocuments();

    File applicationPDF =
        new File(
            resourcesFolderPath + File.separator + "Ann_Too_Pennsylvania_Birth_Certificate.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());

    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");

    JSONArray fields = applicationsQuestionsResponseJSON.getJSONArray("fields");
    int i = 0;
    JSONObject matchedField = null;
    Iterator<Object> fieldIterator = fields.iterator();
    while (fieldIterator.hasNext()) {
      JSONObject JSONField = (JSONObject) fieldIterator.next();
      // An annotated field
      if (JSONField.getString("fieldName").equals("Date:currentDate")) {
        matchedField = JSONField;
      }
    }
    if (matchedField == null) {
      fail("No matched field found");
    } else {
      assertEquals("", matchedField.getString("fieldDefaultValue"));
      assertEquals(true, matchedField.getBoolean("fieldIsMatched"));
      assertEquals("DateField", matchedField.getString("fieldType"));
      assertEquals("Please Enter Your: Date", matchedField.getString("fieldQuestion"));
    }
  }

  @Test
  public void getApplicationQuestionsMatchedFieldsTest3()
      throws IOException, GeneralSecurityException {
    // Test simple matched fields in database
    createUser()
        .withUserType(UserType.Admin)
        .withUsername(username)
        .withPasswordToHash(password)
        .buildAndPersist(userDao);
    TestUtils.login(username, password);
    // when running entire file, other documents interfere with retrieving the form.
    clearAllDocuments();

    File applicationPDF =
        new File(
            resourcesFolderPath + File.separator + "Ann_Too_Pennsylvania_Birth_Certificate.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "FORM");

    JSONObject body = new JSONObject();
    body.put("applicationId", fileId);
    HttpResponse<String> applicationsQuestionsResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-application-questions")
            .body(body.toString())
            .asString();
    JSONObject applicationsQuestionsResponseJSON =
        TestUtils.responseStringToJSON(applicationsQuestionsResponse.getBody());

    assertThat(applicationsQuestionsResponseJSON.getString("status")).isEqualTo("SUCCESS");

    JSONArray fields = applicationsQuestionsResponseJSON.getJSONArray("fields");
    int i = 0;
    JSONObject matchedField = null;
    Iterator<Object> fieldIterator = fields.iterator();
    while (fieldIterator.hasNext()) {
      JSONObject JSONField = (JSONObject) fieldIterator.next();
      // Not annotated field
      if (JSONField.getString("fieldName").equals("Parent 1's First Name")) {
        matchedField = JSONField;
      }
    }
    if (matchedField == null) {
      fail("No matched field found");
    } else {
      assertEquals("", matchedField.getString("fieldDefaultValue"));
      assertEquals(false, matchedField.getBoolean("fieldIsMatched"));
      assertEquals("TextField", matchedField.getString("fieldType"));
      assertEquals(
          "Please Enter Your: Parent 1's First Name", matchedField.getString("fieldQuestion"));
    }
  }

  // ------------------ FILL APPLICATION TESTS ------------------------ //

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
    String fileId = uploadFileAndGetFileId(applicationPDF, "FORM");

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
      PDDocument pdf = PDDocument.load(filled_out_pdf);
      fieldValues = getFieldValues(new FileInputStream(filled_out_pdf));
    } catch (IOException e) {
      assertThat(false).isTrue();
    }
    assertThat(fieldValues).isNotNull();
    // checkFormAnswersSS5Form(fieldValues);
    // delete(fileId, "FORM");
    TestUtils.logout();
  }

  // ------------------ GET TITLE TESTS ------------------------ //

  @Test // Test with title embedded in document
  public void getPDFTitleTest1() throws IOException {
    String fileName = "Pennsylvania_Birth_Certificate_Form.pdf";
    File applicationPDF = new File(resourcesFolderPath + File.separator + fileName);
    InputStream pdfDocument = FileUtils.openInputStream(applicationPDF);
    assertEquals(
        "Pennsylvania - Application for a Birth Certificate",
        PdfController.getPDFTitle(fileName, pdfDocument, PDFType.FORM));
  }

  @Test // Test without any title in document
  public void getPDFTitleTest2() throws IOException {
    String fileName = "library-card-application.pdf";
    File applicationPDF = new File(resourcesFolderPath + File.separator + fileName);
    InputStream pdfDocument = FileUtils.openInputStream(applicationPDF);
    assertEquals(fileName, PdfController.getPDFTitle(fileName, pdfDocument, PDFType.FORM));
  }
}
