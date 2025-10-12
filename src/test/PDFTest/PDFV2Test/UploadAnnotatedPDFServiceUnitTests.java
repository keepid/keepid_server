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
import PDF.Services.V2Services.UploadAnnotatedPDFServiceV2;
import Security.EncryptionController;
import TestUtils.TestUtils;
import User.User;
import User.UserType;
import Validation.ValidationException;
import com.mongodb.client.MongoDatabase;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.jupiter.api.AfterAll;

public class UploadAnnotatedPDFServiceUnitTests {
  private FileDao fileDao;
  private FormDao formDao;
  private UserDao userDao;
  private EncryptionController encryptionController;
  private UserParams developerUserParams;
  private FileParams blankFileParams;
  private InputStream sampleAnnotatedFileStream;
  private InputStream sampleBlankFileStream1;

  @BeforeClass
  public static void start() throws InterruptedException {
    Thread.sleep(3000);
    TestUtils.startServer();
    Thread.sleep(3000);
  }

  @Before
  public void initialize() {
    this.fileDao = FileDaoFactory.create(DeploymentLevel.TEST);
    this.formDao = FormDaoFactory.create(DeploymentLevel.TEST);
    this.userDao = UserDaoFactory.create(DeploymentLevel.TEST);
    MongoDatabase db = MongoConfig.getDatabase(DeploymentLevel.TEST);
    File sampleBlankFile1 = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    File sampleAnnotatedFile =
        new File(resourcesFolderPath + File.separator + "ss-5_filled_out.pdf");
    try {
      sampleAnnotatedFileStream = FileUtils.openInputStream(sampleAnnotatedFile);
      sampleBlankFileStream1 = FileUtils.openInputStream(sampleBlankFile1);
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
    this.blankFileParams =
        new FileParams()
            .setFileName("ss-5.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleBlankFileStream1)
            .setFileOrgName("org1");
  }

  @After
  public void reset() {
    fileDao.clear();
    formDao.clear();
    userDao.clear();
    try {
      sampleAnnotatedFileStream.close();
      sampleBlankFileStream1.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @AfterAll
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void uploadAnnotatedPDFServiceInsufficientPrivilege() {
    UserParams clientOneUserParams =
        new UserParams()
            .setUsername("client1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Client);
    UploadAnnotatedPDFServiceV2 uploadService =
        new UploadAnnotatedPDFServiceV2(
            fileDao, formDao, userDao, clientOneUserParams, blankFileParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(PdfMessage.INSUFFICIENT_PRIVILEGE, response);
  }

  @Test
  public void uploadAnnotatedPDFServiceNullFileStream() {
    blankFileParams.setFileStream(null);
    UploadAnnotatedPDFServiceV2 uploadService =
        new UploadAnnotatedPDFServiceV2(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PDF, response);
  }

  @Test
  public void uploadAnnotatedPDFServiceInvalidFileContentType() {
    blankFileParams.setFileContentType("image");
    UploadAnnotatedPDFServiceV2 uploadService =
        new UploadAnnotatedPDFServiceV2(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PDF, response);
  }

  @Test
  public void uploadAnnotatedPDFServiceSuccess() {
    try {
      userDao.save(
          new User(
              "testFirstName",
              "testLastName",
              "12-12-2012",
              "testemail@keep.id",
              "2652623333",
              "org0",
              "1 Keep Ave",
              "Keep",
              "PA",
              "11111",
              false,
              "dev1",
              "devPass123",
              UserType.Developer));
    } catch (ValidationException e) {
      throw new RuntimeException(e);
    }
    UploadAnnotatedPDFServiceV2 uploadService =
        new UploadAnnotatedPDFServiceV2(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
  }
}
