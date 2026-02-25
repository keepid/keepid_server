package PDFTest.PDFV2Test;

import static PDFTest.PDFV2Test.PDFTestUtilsV2.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import PDF.Services.V2Services.GetQuestionsPDFServiceV2;
import Security.EncryptionController;
import TestUtils.TestUtils;
import User.Address;
import User.Name;
import User.User;
import User.UserType;
import Validation.ValidationException;
import com.mongodb.client.MongoDatabase;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;
import org.junit.jupiter.api.AfterAll;

public class GetQuestionsPDFServiceV2UnitTests {
  private FileDao fileDao;
  private FormDao formDao;
  private UserDao userDao;
  private MongoDatabase db;
  private EncryptionController encryptionController;
  private UserParams developerUserParams;
  private UserParams clientUserParams;
  private FileParams blankFileParams;
  private JSONObject formAnswers;
  private InputStream sampleAnnotatedFileStream;
  private InputStream sampleBlankFileStream1;
  private InputStream signatureStream;

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
    File sampleBlankFile1 = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    File sampleAnnotatedFile =
        new File(resourcesFolderPath + File.separator + "ss-5_filled_out.pdf");
    File signatureFile = new File(resourcesFolderPath + File.separator + "sample-signature.png");
    try {
      sampleAnnotatedFileStream = FileUtils.openInputStream(sampleAnnotatedFile);
      sampleBlankFileStream1 = FileUtils.openInputStream(sampleBlankFile1);
      signatureStream = FileUtils.openInputStream(signatureFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    try {
      this.userDao.save(
          new User(
              new Name("testcFirstName", "testcLastName"),
              "12-12-2012",
              "testcemail@keep.id",
              "2652623333",
              "org2",
              new Address("1 Keep Ave", "Keep", "PA", "11111"),
              false,
              "client1",
              "clientPass123",
              UserType.Developer));
    } catch (ValidationException e) {
      throw new RuntimeException(e);
    }
    this.developerUserParams =
        new UserParams()
            .setUsername("dev1")
            .setOrganizationName("org2")
            .setPrivilegeLevel(UserType.Developer);
    this.clientUserParams =
        new UserParams()
            .setUsername("client1")
            .setOrganizationName("org2")
            .setPrivilegeLevel(UserType.Client);
    this.blankFileParams =
        new FileParams()
            .setFileName("ss-5.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleBlankFileStream1)
            .setFileOrgName("org2");
  }

  @After
  public void reset() {
    fileDao.clear();
    formDao.clear();
    userDao.clear();
    try {
      sampleAnnotatedFileStream.close();
      sampleBlankFileStream1.close();
      signatureStream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @AfterAll
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void getQuestionsPDFMissingForm() {
    ObjectId fakeObjectId = new ObjectId();
    FileParams getQuestionsFileParams = new FileParams().setFileId(fakeObjectId.toString());
    GetQuestionsPDFServiceV2 getService =
        new GetQuestionsPDFServiceV2(formDao, userDao, clientUserParams, getQuestionsFileParams);
    Message response = getService.executeAndGetResponse();
    assertEquals(PdfMessage.MISSING_FORM, response);
  }

  @Test
  public void getQuestionsPDFSuccess() {
    ObjectId uploadedFileId =
        uploadBlankSSFormAndGetFileId(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    FileParams getQuestionsFileParams = new FileParams().setFileId(uploadedFileId.toString());
    GetQuestionsPDFServiceV2 getService =
        new GetQuestionsPDFServiceV2(formDao, userDao, clientUserParams, getQuestionsFileParams);
    Message response = getService.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
    JSONArray fields = (JSONArray) getService.getApplicationInformation().get("fields");
    List<String> fieldNames = getFieldNamesFromFields(fields);
    // First field, readOnlyField
    assertTrue(fieldNames.contains("topmostSubform[0].Page1[0].First[0]"));
    // textField
    assertTrue(fieldNames.contains("topmostSubform[0].Page5[0].Middlename[0]"));
    // checkBox
    assertTrue(fieldNames.contains("topmostSubform[0].Page5[0].hawaiian[0]"));
    assertEquals(70, fields.length());
  }
}
