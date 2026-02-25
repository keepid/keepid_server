package PDFTest.PDFV2Test;

import static PDFTest.PDFV2Test.PDFTestUtilsV2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import File.FileType;
import File.IdCategoryType;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import PDF.Services.V2Services.DeletePDFServiceV2;
import PDF.Services.V2Services.UploadPDFServiceV2;
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
import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.junit.*;
import org.junit.jupiter.api.AfterAll;

public class DeletePDFServiceUnitTests {
  private FileDao fileDao;
  private FormDao formDao;
  private ActivityDao activityDao;
  private UserDao userDao;
  private MongoDatabase db;
  private EncryptionController encryptionController;
  private UserParams clientOneUserParams;
  private UserParams developerUserParams;
  private FileParams blankFileParams;
  private FileParams uploadFileOneFileParams;
  private InputStream sampleFileStream1;
  private InputStream sampleFileStream2;
  private InputStream sampleImageStream;
  private InputStream sampleAnnotatedFileStream;
  private InputStream sampleBlankFileStream1;
  private InputStream sampleBlankFileStream2;
  private InputStream signatureStream;

  @BeforeClass
  public static void start() {
    TestUtils.startServer();
  }

  @Before
  public void initialize() {
    this.fileDao = FileDaoFactory.create(DeploymentLevel.TEST);
    this.formDao = FormDaoFactory.create(DeploymentLevel.TEST);
    this.activityDao = ActivityDaoFactory.create(DeploymentLevel.TEST);
    this.userDao = UserDaoFactory.create(DeploymentLevel.TEST);
    this.db = MongoConfig.getDatabase(DeploymentLevel.TEST);
    File sampleBlankFile1 = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    File sampleBlankFile2 =
        new File(resourcesFolderPath + File.separator + "Application_for_a_Birth_Certificate.pdf");
    File sampleAnnotatedFile =
        new File(resourcesFolderPath + File.separator + "ss-5_filled_out.pdf");
    File sampleImageFile = new File(resourcesFolderPath + File.separator + "first-love.png");
    File sampleFile1 = new File(resourcesFolderPath + File.separator + "test_out_signature.pdf");
    File sampleFile2 = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
    File signatureFile = new File(resourcesFolderPath + File.separator + "sample-signature.png");
    try {
      sampleImageStream = FileUtils.openInputStream(sampleImageFile);
      sampleFileStream1 = FileUtils.openInputStream(sampleFile1);
      sampleFileStream2 = FileUtils.openInputStream(sampleFile2);
      sampleAnnotatedFileStream = FileUtils.openInputStream(sampleAnnotatedFile);
      sampleBlankFileStream1 = FileUtils.openInputStream(sampleBlankFile1);
      sampleBlankFileStream2 = FileUtils.openInputStream(sampleBlankFile2);
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
    this.blankFileParams =
        new FileParams()
            .setFileName("ss-5.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleBlankFileStream1)
            .setFileOrgName("org2");
    this.clientOneUserParams =
        new UserParams()
            .setUsername("client1")
            .setOrganizationName("org2")
            .setPrivilegeLevel(UserType.Client);
    this.uploadFileOneFileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("test_out_signature.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleFileStream1)
            .setIdCategoryType(IdCategoryType.OTHER);
  }

  @After
  public void reset() {
    fileDao.clear();
    formDao.clear();
    userDao.clear();
    try {
      sampleImageStream.close();
      sampleFileStream1.close();
      sampleFileStream2.close();
      sampleAnnotatedFileStream.close();
      sampleBlankFileStream1.close();
      sampleBlankFileStream2.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @AfterAll
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void deletePDFServiceNullPDFType() {
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(
            fileDao, clientOneUserParams, uploadFileOneFileParams, encryptionController);
    uploadService.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    String fileId = fileDao.get("client1", FileType.IDENTIFICATION_PDF).get().getId().toString();
    FileParams deleteFileOneParams = new FileParams().setFileId(fileId).setPdfType(null);
    DeletePDFServiceV2 deleteService =
        new DeletePDFServiceV2(fileDao, formDao, clientOneUserParams, deleteFileOneParams);
    Message response = deleteService.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    assertEquals(PdfMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void deletePDFServiceNoSuchFile() {
    FileParams deleteFileOneParams =
        new FileParams()
            .setFileId(new ObjectId().toString())
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT);
    DeletePDFServiceV2 deleteService =
        new DeletePDFServiceV2(fileDao, formDao, clientOneUserParams, deleteFileOneParams);
    Message response = deleteService.executeAndGetResponse();
    assertEquals(PdfMessage.NO_SUCH_FILE, response);
  }

  @Test
  public void deletePDFServiceInsufficientPrivilege() {
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(
            fileDao, clientOneUserParams, uploadFileOneFileParams, encryptionController);
    uploadService.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    UserParams workerOneUserParams =
        new UserParams()
            .setUsername("worker1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Worker);
    String fileId = fileDao.get("client1", FileType.IDENTIFICATION_PDF).get().getId().toString();
    FileParams deleteFileOneParams =
        new FileParams().setFileId(fileId).setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT);
    DeletePDFServiceV2 deleteService =
        new DeletePDFServiceV2(fileDao, formDao, workerOneUserParams, deleteFileOneParams);
    Message response = deleteService.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    assertEquals(PdfMessage.INSUFFICIENT_PRIVILEGE, response);
  }

  @Test
  public void deletePDFServiceClientDocumentSuccess() {
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(
            fileDao, clientOneUserParams, uploadFileOneFileParams, encryptionController);
    uploadService.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    String fileId = fileDao.get("client1", FileType.IDENTIFICATION_PDF).get().getId().toString();
    FileParams deleteFileOneParams =
        new FileParams().setFileId(fileId).setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT);
    DeletePDFServiceV2 deleteService =
        new DeletePDFServiceV2(fileDao, formDao, clientOneUserParams, deleteFileOneParams);
    Message response = deleteService.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
    assertEquals(0, fileDao.size());
  }

  @Test
  public void deletePDFServiceApplicationsSuccess() {
    ObjectId uploadedBlankFileId =
        uploadBlankSSFormAndGetFileId(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    JSONObject formQuestions =
        getQuestionsSSForm(formDao, userDao, clientOneUserParams, uploadedBlankFileId);
    JSONObject formAnswers = getSampleFormAnswersFromSSFormQuestions(formQuestions);
    ObjectId filledFileObjectId =
        uploadAnnotatedSSFormAndGetFileId(
            fileDao,
            formDao,
            activityDao,
            clientOneUserParams,
            encryptionController,
            signatureStream,
            formAnswers,
            uploadedBlankFileId);
    assertEquals(2, fileDao.size());
    assertEquals(2, fileDao.size());
    FileParams deleteAnnotatedFileParams =
        new FileParams()
            .setFileId(filledFileObjectId.toString())
            .setPdfType(PDFTypeV2.ANNOTATED_APPLICATION);
    UserParams workerUserParams =
        new UserParams()
            .setUsername("worker1")
            .setOrganizationName("org2")
            .setPrivilegeLevel(UserType.Worker);
    DeletePDFServiceV2 deleteAnnotatedService =
        new DeletePDFServiceV2(fileDao, formDao, workerUserParams, deleteAnnotatedFileParams);
    Message deleteAnnotatedResponse = deleteAnnotatedService.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, deleteAnnotatedResponse);
    assertEquals(1, fileDao.size());
    assertEquals(1, formDao.size());

    FileParams deleteBlankFileParams =
        new FileParams()
            .setFileId(uploadedBlankFileId.toString())
            .setPdfType(PDFTypeV2.BLANK_APPLICATION);
    DeletePDFServiceV2 deleteBlankService =
        new DeletePDFServiceV2(fileDao, formDao, developerUserParams, deleteBlankFileParams);
    Message deleteBlankResponse = deleteBlankService.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, deleteAnnotatedResponse);
    assertEquals(0, fileDao.size());
    assertEquals(0, formDao.size());
  }
}
