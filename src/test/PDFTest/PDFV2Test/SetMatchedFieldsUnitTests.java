package PDFTest.PDFV2Test;

import static PDFTest.PDFV2Test.PDFTestUtilsV2.*;
import static org.junit.Assert.*;

import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import Form.FieldType;
import Form.FormQuestion;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import PDF.Services.V2Services.GetQuestionsPDFServiceV2;
import Security.EncryptionController;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.OptionalInformation;
import User.User;
import User.UserInformation.Address;
import User.UserInformation.BasicInfo;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.junit.*;
import org.junit.jupiter.api.AfterAll;

/**
 * TDD tests for setMatchedFields() behavior with flattened field map, alias mapping, graceful
 * degradation on unmatched fields, and last-colon parsing.
 *
 * <p>These tests describe the DESIRED behavior after the refactor. Some will fail against the
 * current implementation and should pass after Step 2 (refactor setMatchedFields) and Step 3
 * (re-enable in getQuestions).
 */
public class SetMatchedFieldsUnitTests {
  private FileDao fileDao;
  private FormDao formDao;
  private UserDao userDao;
  private MongoDatabase db;
  private EncryptionController encryptionController;
  private UserParams developerUserParams;
  private UserParams clientUserParams;
  private FileParams blankFileParams;
  private ObjectId uploadedFileId;
  private GetQuestionsPDFServiceV2 service;

  @BeforeClass
  public static void start() {
    TestUtils.startServer();
  }

  @Before
  public void initialize() throws InterruptedException {
    Thread.sleep(1000);
    this.fileDao = FileDaoFactory.create(DeploymentLevel.TEST);
    this.formDao = FormDaoFactory.create(DeploymentLevel.TEST);
    this.userDao = UserDaoFactory.create(DeploymentLevel.TEST);
    this.db = MongoConfig.getDatabase(DeploymentLevel.TEST);

    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Create developer user for uploading forms
    this.developerUserParams =
        new UserParams()
            .setUsername("dev1")
            .setOrganizationName("org2")
            .setPrivilegeLevel(UserType.Developer);

    // Create client user with known profile data for matching tests
    User clientUser =
        EntityFactory.createUser()
            .withUsername("matchclient")
            .withFirstName("John")
            .withLastName("Doe")
            .withEmail("john@example.com")
            .withPhoneNumber("5551234567")
            .withAddress("123 Main St")
            .withCity("Philadelphia")
            .withState("PA")
            .withZipcode("19104")
            .withUserType(UserType.Client)
            .withOrgName("org2")
            .build();

    // Add optional information with nested address for nested field tests
    OptionalInformation optionalInfo = new OptionalInformation();
    BasicInfo basicInfo = new BasicInfo();
    Address mailingAddress = new Address();
    mailingAddress.setStreetAddress("456 Oak Ave");
    mailingAddress.setCity("Pittsburgh");
    mailingAddress.setState("PA");
    mailingAddress.setZip("15213");
    basicInfo.setMailingAddress(mailingAddress);
    basicInfo.setEmailAddress("john.optional@example.com");
    basicInfo.setPhoneNumber("5559876543");
    optionalInfo.setBasicInfo(basicInfo);
    clientUser.setOptionalInformation(optionalInfo);
    userDao.save(clientUser);

    this.clientUserParams =
        new UserParams()
            .setUsername("matchclient")
            .setOrganizationName("org2")
            .setPrivilegeLevel(UserType.Client);

    // Upload a blank SS form so the service can initialize
    File sampleBlankFile = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    InputStream sampleBlankFileStream;
    try {
      sampleBlankFileStream = FileUtils.openInputStream(sampleBlankFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.blankFileParams =
        new FileParams()
            .setFileName("ss-5.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleBlankFileStream)
            .setFileOrgName("org2");

    this.uploadedFileId =
        uploadBlankSSFormAndGetFileId(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);

    // Create and initialize the service (populates user info internally)
    FileParams getQuestionsFileParams = new FileParams().setFileId(uploadedFileId.toString());
    this.service =
        new GetQuestionsPDFServiceV2(formDao, userDao, clientUserParams, getQuestionsFileParams);
    Message initResponse = service.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, initResponse);
  }

  @After
  public void reset() {
    fileDao.clear();
    formDao.clear();
    userDao.clear();
  }

  @AfterAll
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  // ---- Helper to build a FormQuestion with a given annotation string ----

  private FormQuestion makeQuestion(String questionName) {
    return new FormQuestion(
        new ObjectId(),
        FieldType.TEXT_FIELD,
        questionName,
        "", // questionText (will be set by setMatchedFields)
        "", // answerText
        new ArrayList<>(),
        "", // defaultValue
        true, // required
        1, // numLines
        false, // matched
        new ObjectId(),
        "NONE");
  }

  // =====================================================================
  // Direct field matching via flattened map
  // =====================================================================

  @Test
  public void directMatchFirstName() {
    FormQuestion fq = makeQuestion("First Name:firstName");
    Message result = service.setMatchedFields(fq);
    assertNull("Should not return error for matched field", result);
    assertTrue("firstName should be matched", fq.isMatched());
    assertEquals("John", fq.getDefaultValue());
    assertEquals("First Name", fq.getQuestionText());
  }

  @Test
  public void directMatchEmail() {
    FormQuestion fq = makeQuestion("Email:email");
    Message result = service.setMatchedFields(fq);
    assertNull("Should not return error for matched field", result);
    assertTrue("email should be matched", fq.isMatched());
    assertEquals("john@example.com", fq.getDefaultValue());
  }

  @Test
  public void directMatchCity() {
    FormQuestion fq = makeQuestion("City:city");
    Message result = service.setMatchedFields(fq);
    assertNull("Should not return error for matched field", result);
    assertTrue("city should be matched", fq.isMatched());
    assertEquals("Philadelphia", fq.getDefaultValue());
  }

  // =====================================================================
  // Alias mapping
  // =====================================================================

  @Test
  public void aliasEmailAddress() {
    // "emailAddress" should alias to "email"
    FormQuestion fq = makeQuestion("Email Address:emailAddress");
    Message result = service.setMatchedFields(fq);
    assertNull("Should not return error for alias match", result);
    assertTrue("emailAddress alias should be matched", fq.isMatched());
    assertEquals("john@example.com", fq.getDefaultValue());
    assertEquals("Email Address", fq.getQuestionText());
  }

  @Test
  public void aliasPhoneNumber() {
    // "phoneNumber" should alias to "phone"
    FormQuestion fq = makeQuestion("Phone Number:phoneNumber");
    Message result = service.setMatchedFields(fq);
    assertNull("Should not return error for alias match", result);
    assertTrue("phoneNumber alias should be matched", fq.isMatched());
    assertEquals("5551234567", fq.getDefaultValue());
    assertEquals("Phone Number", fq.getQuestionText());
  }

  // =====================================================================
  // Nested flattened key matching
  // =====================================================================

  @Test
  public void nestedMailingAddressCity() {
    FormQuestion fq =
        makeQuestion("Mailing City:optionalInformation.basicInfo.mailingAddress.city");
    Message result = service.setMatchedFields(fq);
    assertNull("Should not return error for nested match", result);
    assertTrue("nested mailing city should be matched", fq.isMatched());
    assertEquals("Pittsburgh", fq.getDefaultValue());
    assertEquals("Mailing City", fq.getQuestionText());
  }

  @Test
  public void nestedMailingAddressState() {
    FormQuestion fq =
        makeQuestion("Mailing State:optionalInformation.basicInfo.mailingAddress.state");
    Message result = service.setMatchedFields(fq);
    assertNull("Should not return error for nested match", result);
    assertTrue("nested mailing state should be matched", fq.isMatched());
    assertEquals("PA", fq.getDefaultValue());
  }

  // =====================================================================
  // Unmatched field -- graceful degradation (no error)
  // =====================================================================

  @Test
  public void unmatchedFieldNoError() {
    FormQuestion fq = makeQuestion("Some Field:unknownFieldThatDoesNotExist");
    Message result = service.setMatchedFields(fq);
    // NEW behavior: unmatched should NOT return error, just leave unmatched
    assertNull("Should not return error for unmatched field", result);
    assertFalse("unmatched field should have matched=false", fq.isMatched());
    assertEquals("unmatched field should have empty default", "", fq.getDefaultValue());
    assertEquals("Some Field", fq.getQuestionText());
  }

  // =====================================================================
  // Special directives (should keep working)
  // =====================================================================

  @Test
  public void specialDirectiveAnyDate() {
    FormQuestion fq = makeQuestion("Select Date:anyDate");
    Message result = service.setMatchedFields(fq);
    assertNull(result);
    assertEquals(FieldType.DATE_FIELD, fq.getType());
    assertFalse("anyDate should not set matched", fq.isMatched());
  }

  @Test
  public void specialDirectiveCurrentDate() {
    FormQuestion fq = makeQuestion("Today:currentDate");
    Message result = service.setMatchedFields(fq);
    assertNull(result);
    assertEquals(FieldType.DATE_FIELD, fq.getType());
    assertTrue("currentDate should set matched=true", fq.isMatched());
  }

  @Test
  public void specialDirectiveSignature() {
    FormQuestion fq = makeQuestion("Signature:signature");
    Message result = service.setMatchedFields(fq);
    assertNull(result);
    assertEquals(FieldType.SIGNATURE, fq.getType());
  }

  @Test
  public void specialDirectivePositiveConditional() {
    ObjectId linkedId = new ObjectId();
    FormQuestion fq = makeQuestion("Conditional Q:+" + linkedId.toHexString());
    Message result = service.setMatchedFields(fq);
    assertNull(result);
    assertEquals("POSITIVE", fq.getConditionalType());
    assertEquals(linkedId, fq.getConditionalOnField());
  }

  @Test
  public void specialDirectiveNegativeConditional() {
    ObjectId linkedId = new ObjectId();
    FormQuestion fq = makeQuestion("Conditional Q:-" + linkedId.toHexString());
    Message result = service.setMatchedFields(fq);
    assertNull(result);
    assertEquals("NEGATIVE", fq.getConditionalType());
    assertEquals(linkedId, fq.getConditionalOnField());
  }

  // =====================================================================
  // Colons in question text -- split on LAST colon
  // =====================================================================

  @Test
  public void colonsInQuestionTextParsedByLastColon() {
    // "Question:with:colons:firstName" should parse:
    //   questionText = "Question:with:colons"
    //   directive = "firstName"
    FormQuestion fq = makeQuestion("Question:with:colons:firstName");
    Message result = service.setMatchedFields(fq);
    assertNull("Should not return error for colons in question text", result);
    assertTrue("Should match firstName via last-colon parsing", fq.isMatched());
    assertEquals("John", fq.getDefaultValue());
    assertEquals("Question:with:colons", fq.getQuestionText());
  }

  // =====================================================================
  // Question text only (no colon) -- no matching attempted
  // =====================================================================

  @Test
  public void questionTextOnlyNoColon() {
    FormQuestion fq = makeQuestion("Just a plain question");
    Message result = service.setMatchedFields(fq);
    assertNull(result);
    assertFalse("No colon means no matching", fq.isMatched());
    assertEquals("Just a plain question", fq.getQuestionText());
  }
}
