package PDFTest.PDFV2Test;

import static PDFTest.PDFV2Test.PDFTestUtilsV2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import File.IdCategoryType;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import PDF.Services.V2Services.FilterPDFServiceV2;
import PDF.Services.V2Services.UploadPDFServiceV2;
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
import org.json.JSONArray;
import org.junit.*;
import org.junit.jupiter.api.AfterAll;

public class FilterPDFServiceUnitTests {

  private FileDao fileDao;
  private FormDao formDao;
  private ActivityDao activityDao;
  private UserDao userDao;
  private MongoDatabase db;
  private InputStream sampleFileStream1;
  private InputStream sampleFileStream2;
  private InputStream sampleImageStream;
  private InputStream sampleAnnotatedFileStream;
  private InputStream sampleBlankFileStream1;
  private InputStream sampleBlankFileStream2;
  private InputStream signatureStream;
  private UserParams clientOneUserParams;
  private UserParams developerUserParams;
  private UserParams workerUserParams;
  private FileParams uploadImageFileParams;
  private FileParams uploadFileOneFileParams;
  private FileParams uploadFileTwoFileParams;
  private FileParams blankOneFileParams;
  private FileParams blankTwoFileParams;
  private EncryptionController encryptionController;

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
      System.out.println(sampleImageStream);
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
              "testcFirstName",
              "testcLastName",
              "12-12-2012",
              "testcemail@keep.id",
              "2652623333",
              "org2",
              "1 Keep Ave",
              "Keep",
              "PA",
              "11111",
              false,
              "client1",
              "clientPass123",
              UserType.Developer));
    } catch (ValidationException e) {
      throw new RuntimeException(e);
    }
    try {
      this.userDao.save(
          new User(
              "testtFirstName",
              "testtLastName",
              "12-12-2012",
              "testtemail@keep.id",
              "2652623333",
              "org2",
              "1 Keep Ave",
              "Keep",
              "PA",
              "11111",
              false,
              "worker1",
              "workerPass123",
              UserType.Developer));
    } catch (ValidationException e) {
      throw new RuntimeException(e);
    }
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
    this.clientOneUserParams =
        new UserParams()
            .setUsername("client1")
            .setOrganizationName("org2")
            .setPrivilegeLevel(UserType.Client);
    this.workerUserParams =
        new UserParams()
            .setUsername("worker1")
            .setOrganizationName("org2")
            .setPrivilegeLevel(UserType.Worker);
    this.developerUserParams =
        new UserParams()
            .setUsername("dev1")
            .setOrganizationName("org2")
            .setPrivilegeLevel(UserType.Developer);
    this.uploadFileOneFileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("test_out_signature.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleFileStream1)
            .setIdCategoryType(IdCategoryType.OTHER);
    this.uploadFileTwoFileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("testpdf.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleFileStream2)
            .setIdCategoryType(IdCategoryType.OTHER);
    this.uploadImageFileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("first-love.png")
            .setFileContentType("image")
            .setFileStream(sampleImageStream)
            .setIdCategoryType(IdCategoryType.OTHER);
    this.blankOneFileParams =
        new FileParams()
            .setFileName("ss-5.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleBlankFileStream1)
            .setFileOrgName("org2");
    this.blankTwoFileParams =
        new FileParams()
            .setFileName("Application_for_a_Birth_Certificate.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleBlankFileStream2)
            .setFileOrgName("org2");
  }

  @After
  public void reset() {
    this.fileDao.clear();
    this.formDao.clear();
    if(this.userDao != null) {
      this.userDao.clear();
    }
    try {
      sampleImageStream.close();
      sampleFileStream1.close();
      sampleFileStream2.close();
      sampleAnnotatedFileStream.close();
      sampleBlankFileStream1.close();
      sampleBlankFileStream2.close();
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
  public void filterPDFServiceNullPDFType() {
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(
            fileDao, clientOneUserParams, uploadFileOneFileParams, encryptionController);
    uploadService.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    FileParams filterClientDocsFileParams = new FileParams().setPdfType(null).setAnnotated(false);
    FilterPDFServiceV2 filterService =
        new FilterPDFServiceV2(fileDao, clientOneUserParams, filterClientDocsFileParams);
    Message response = filterService.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PDF_TYPE, response);
  }

  @Test
  public void filterPDFServiceFilterClientDocsSuccess() {
    uploadSixTestStreams(
        fileDao,
        formDao,
        activityDao,
        userDao,
        signatureStream,
        clientOneUserParams,
        developerUserParams,
        blankOneFileParams,
        blankTwoFileParams,
        uploadFileOneFileParams,
        uploadFileTwoFileParams,
        uploadImageFileParams,
        encryptionController);
    assertEquals(6, fileDao.size());
    assertEquals(3, formDao.size());
    FileParams filterFileParams =
        new FileParams().setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT).setAnnotated(false);
    FilterPDFServiceV2 filterService =
        new FilterPDFServiceV2(fileDao, clientOneUserParams, filterFileParams);
    Message response = filterService.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
    JSONArray filteredFiles = filterService.getFiles();
    assertEquals(3, filteredFiles.length());
  }

  @Test
  public void filterPDFServiceFilterBlankApplicationsSuccess() {
    uploadSixTestStreams(
        fileDao,
        formDao,
        activityDao,
        userDao,
        signatureStream,
        clientOneUserParams,
        developerUserParams,
        blankOneFileParams,
        blankTwoFileParams,
        uploadFileOneFileParams,
        uploadFileTwoFileParams,
        uploadImageFileParams,
        encryptionController);
    assertEquals(6, fileDao.size());
    assertEquals(3, formDao.size());
    System.out.println(fileDao.getAll());
    FileParams filterFileParams =
        new FileParams().setPdfType(PDFTypeV2.BLANK_APPLICATION).setAnnotated(true);
    FilterPDFServiceV2 filterService =
        new FilterPDFServiceV2(fileDao, clientOneUserParams, filterFileParams);
    Message response = filterService.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
    JSONArray filteredFiles = filterService.getFiles();
    assertEquals(2, filteredFiles.length());
  }

  @Test
  public void filterPDFServiceFilterFilledApplicationSuccess() {
    uploadSixTestStreams(
        fileDao,
        formDao,
        activityDao,
        userDao,
        signatureStream,
        clientOneUserParams,
        developerUserParams,
        blankOneFileParams,
        blankTwoFileParams,
        uploadFileOneFileParams,
        uploadFileTwoFileParams,
        uploadImageFileParams,
        encryptionController);
    assertEquals(6, fileDao.size());
    assertEquals(3, formDao.size());
    FileParams filterFileParams =
        new FileParams().setPdfType(PDFTypeV2.ANNOTATED_APPLICATION).setAnnotated(true);
    FilterPDFServiceV2 filterService =
        new FilterPDFServiceV2(fileDao, workerUserParams, filterFileParams);
    Message response = filterService.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
    JSONArray filteredFiles = filterService.getFiles();
    assertEquals(1, filteredFiles.length());
  }

  @Test
  public void filterPDFServiceInsufficientPrivilege() {
    uploadSixTestStreams(
        fileDao,
        formDao,
        activityDao,
        userDao,
        signatureStream,
        clientOneUserParams,
        developerUserParams,
        blankOneFileParams,
        blankTwoFileParams,
        uploadFileOneFileParams,
        uploadFileTwoFileParams,
        uploadImageFileParams,
        encryptionController);
    assertEquals(6, fileDao.size());
    assertEquals(3, formDao.size());
    FileParams filterClientFileParams =
        new FileParams().setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT).setAnnotated(true);
    FilterPDFServiceV2 filterClientDocumentService =
        new FilterPDFServiceV2(fileDao, workerUserParams, filterClientFileParams);
    Message response = filterClientDocumentService.executeAndGetResponse();
    assertEquals(PdfMessage.INSUFFICIENT_PRIVILEGE, response);
    FileParams filterAnnotatedFileParams =
        new FileParams().setPdfType(PDFTypeV2.ANNOTATED_APPLICATION).setAnnotated(true);
    FilterPDFServiceV2 filterAnnotatedService =
        new FilterPDFServiceV2(fileDao, clientOneUserParams, filterAnnotatedFileParams);
    response = filterAnnotatedService.executeAndGetResponse();
    assertEquals(PdfMessage.INSUFFICIENT_PRIVILEGE, response);
  }
}
