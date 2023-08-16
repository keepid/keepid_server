package PDFTest.PDFV2Test;

import static PDFTest.PDFV2Test.PDFTestUtilsV2.*;
import static org.junit.Assert.assertEquals;

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
import PDF.Services.V2Services.UploadSignedPDFServiceV2;
import Security.EncryptionController;
import TestUtils.TestUtils;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import java.io.*;
import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.junit.*;
import org.junit.jupiter.api.AfterAll;

public class UploadSignedPDFUnitTests {
  private FileDao fileDao;
  private FormDao formDao;
  private UserDao userDao;
  private MongoDatabase db;
  private EncryptionController encryptionController;
  private UserParams developerUserParams;
  private UserParams clientUserParams;
  private FileParams blankFileParams;
  private InputStream sampleAnnotatedFileStream;
  private InputStream sampleBlankFileStream1;
  private InputStream signatureStream;

  @BeforeClass
  public static void start() {
    TestUtils.startServer();
  }

  @Before
  public void initialize() {
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
    this.developerUserParams =
        new UserParams()
            .setUsername("dev1")
            .setOrganizationName("org0")
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
  public void uploadSignedPDFNullFormAnswers() {
    ObjectId uploadedFileId =
        uploadBlankSSFormAndGetFileId(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    FileParams fillFileParams =
        new FileParams()
            .setFileId(uploadedFileId.toString())
            .setFormAnswers(null)
            .setSignatureStream(signatureStream);
    UploadSignedPDFServiceV2 uploadService =
        new UploadSignedPDFServiceV2(
            fileDao, formDao, clientUserParams, fillFileParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void uploadSignedPDFServiceNullSignatureStream() {
    ObjectId uploadedFileId =
        uploadBlankSSFormAndGetFileId(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    JSONObject formQuestions =
        getQuestionsSSForm(formDao, userDao, clientUserParams, uploadedFileId);
    JSONObject formAnswers = getSampleFormAnswersFromSSFormQuestions(formQuestions);
    FileParams fillFileParams =
        new FileParams()
            .setFileId(uploadedFileId.toString())
            .setFormAnswers(formAnswers)
            .setSignatureStream(null);
    UploadSignedPDFServiceV2 uploadService =
        new UploadSignedPDFServiceV2(
            fileDao, formDao, clientUserParams, fillFileParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(PdfMessage.SERVER_ERROR, response);
  }

  @Test
  public void uploadSignedPDFServiceInsufficientPrivilege() {
    ObjectId uploadedFileId =
        uploadBlankSSFormAndGetFileId(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    JSONObject formQuestions =
        getQuestionsSSForm(formDao, userDao, clientUserParams, uploadedFileId);
    JSONObject formAnswers = getSampleFormAnswersFromSSFormQuestions(formQuestions);
    FileParams fillFileParams =
        new FileParams()
            .setFileId(uploadedFileId.toString())
            .setFormAnswers(formAnswers)
            .setSignatureStream(signatureStream);
    UploadSignedPDFServiceV2 uploadService =
        new UploadSignedPDFServiceV2(
            fileDao, formDao, developerUserParams, fillFileParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(PdfMessage.INSUFFICIENT_PRIVILEGE, response);
  }

  @Test
  public void uploadSignedPDFServiceSSSuccess() {
    ObjectId uploadedFileId =
        uploadBlankSSFormAndGetFileId(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    assertEquals(1, fileDao.size());
    assertEquals(1, formDao.size());
    JSONObject formQuestions =
        getQuestionsSSForm(formDao, userDao, clientUserParams, uploadedFileId);
    JSONObject formAnswers = getSampleFormAnswersFromSSFormQuestions(formQuestions);
    FileParams fillFileParams =
        new FileParams()
            .setFileId(uploadedFileId.toString())
            .setFormAnswers(formAnswers)
            .setSignatureStream(signatureStream);
    UploadSignedPDFServiceV2 uploadService =
        new UploadSignedPDFServiceV2(
            fileDao, formDao, clientUserParams, fillFileParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
    assertEquals(2, fileDao.size());
    assertEquals(2, formDao.size());
  }
}
