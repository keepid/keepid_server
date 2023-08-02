package PDFTest.PDFV2Test;

import static PDFTest.PDFTestUtils.resourcesFolderPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import File.FileMessage;
import File.IdCategoryType;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import PDF.Services.V2Services.UploadPDFServiceV2;
import Security.EncryptionController;
import TestUtils.TestUtils;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.*;

@Slf4j
public class UploadPDFServiceUnitTests {
  private FileDao fileDao;
  private MongoDatabase db;
  private EncryptionController encryptionController;
  private FileInputStream sampleFileStream;
  private FileInputStream sampleImageStream;
  private FileInputStream sampleAnnotatedFileStream;

  @BeforeClass
  public static void start() {
    TestUtils.startServer();
  }

  @Before
  public void initialize() {
    this.fileDao = FileDaoFactory.create(DeploymentLevel.TEST);
    this.db = MongoConfig.getDatabase(DeploymentLevel.TEST);
    File sampleFile = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    File sampleAnnotatedFile =
        new File(resourcesFolderPath + File.separator + "ss-5_filled_out.pdf");
    File sampleImage = new File(resourcesFolderPath + File.separator + "first-love.png");
    try {
      this.sampleFileStream = FileUtils.openInputStream(sampleFile);
      this.sampleAnnotatedFileStream = FileUtils.openInputStream(sampleAnnotatedFile);
      this.sampleImageStream = FileUtils.openInputStream(sampleImage);
    } catch (IOException e) {
      log.error("Opening file stream failed");
    }
    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {
      log.error("Generating test encryption controller failed");
    }
  }

  @After
  public void reset() {
    fileDao.clear();
    try {
      sampleFileStream.close();
      sampleImageStream.close();
      sampleAnnotatedFileStream.close();
    } catch (IOException e) {
      log.error("Closing file stream failed");
    }
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void uploadPDFServiceNullPDFType() {
    UserParams userParams =
        new UserParams()
            .setUsername("user1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Client);
    FileParams fileParams =
        new FileParams()
            .setPdfType(null)
            .setFileName("ss-5.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleFileStream)
            .setIdCategoryType(IdCategoryType.NONE);
    UploadPDFServiceV2 service =
        new UploadPDFServiceV2(fileDao, userParams, fileParams, encryptionController);
    Message response = service.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PDF_TYPE, response);
  }

  @Test
  public void uploadPDFServiceNullFileStream() {
    UserParams userParams =
        new UserParams()
            .setUsername("user1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Worker);
    FileParams fileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.BLANK_APPLICATION)
            .setFileName("ss-5.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(null)
            .setIdCategoryType(IdCategoryType.NONE);
    UploadPDFServiceV2 service =
        new UploadPDFServiceV2(fileDao, userParams, fileParams, encryptionController);
    Message response = service.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PDF, response);
  }

  @Test
  public void uploadPDFServiceInvalidFileContentType() {
    UserParams userParams =
        new UserParams()
            .setUsername("user1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Worker);
    FileParams fileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.BLANK_APPLICATION)
            .setFileName("ss-5.pdf")
            .setFileContentType("invalid")
            .setFileStream(sampleFileStream)
            .setIdCategoryType(IdCategoryType.NONE);
    UploadPDFServiceV2 service =
        new UploadPDFServiceV2(fileDao, userParams, fileParams, encryptionController);
    Message response = service.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PDF, response);
  }

  @Test
  public void uploadPDFServiceImageSuccess() {
    UserParams userParams =
        new UserParams()
            .setUsername("user1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Client);
    FileParams fileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("first-love.png")
            .setFileContentType("image")
            .setFileStream(sampleImageStream)
            .setIdCategoryType(IdCategoryType.OTHER);
    assertEquals(0, fileDao.size());
    UploadPDFServiceV2 service =
        new UploadPDFServiceV2(fileDao, userParams, fileParams, encryptionController);
    Message response = service.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
    assertEquals(1, fileDao.size());
  }

  @Test
  public void uploadPDFServiceBlankPDFFailure() {
    UserParams userParams =
        new UserParams()
            .setUsername("user1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Worker);
    FileParams fileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.BLANK_APPLICATION)
            .setFileName("ss-5.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleFileStream)
            .setIdCategoryType(IdCategoryType.NONE);
    UploadPDFServiceV2 service =
        new UploadPDFServiceV2(fileDao, userParams, fileParams, encryptionController);
    Message response = service.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PDF_TYPE, response);
  }

  @Test
  public void uploadPDFServiceAnnotatedPDFFailure() {
    UserParams userParams =
        new UserParams()
            .setUsername("user1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Client);
    FileParams fileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.ANNOTATED_APPLICATION)
            .setFileName("ss-5_filled_out.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleAnnotatedFileStream)
            .setIdCategoryType(IdCategoryType.NONE);
    UploadPDFServiceV2 service =
        new UploadPDFServiceV2(fileDao, userParams, fileParams, encryptionController);
    Message response = service.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PDF_TYPE, response);
  }

  @Test
  public void uploadPDFServiceDuplicateUpload() {
    UserParams userParams =
        new UserParams()
            .setUsername("user1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Client);
    FileParams fileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("first-love.png")
            .setFileContentType("image")
            .setFileStream(sampleImageStream)
            .setIdCategoryType(IdCategoryType.OTHER);
    File duplicateSampleImageFile =
        new File(resourcesFolderPath + File.separator + "first-love.png");
    InputStream duplicateSampleImageStream = null;
    try {
      duplicateSampleImageStream = FileUtils.openInputStream(duplicateSampleImageFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    FileParams fileParamsDuplicate =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("first-love.png")
            .setFileContentType("image")
            .setFileStream(duplicateSampleImageStream)
            .setIdCategoryType(IdCategoryType.OTHER);
    UploadPDFServiceV2 service =
        new UploadPDFServiceV2(fileDao, userParams, fileParams, encryptionController);
    Message response = service.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
    UploadPDFServiceV2 duplicateService =
        new UploadPDFServiceV2(fileDao, userParams, fileParamsDuplicate, encryptionController);
    Message duplicateResponse = duplicateService.executeAndGetResponse();
    assertEquals(FileMessage.FILE_EXISTS, duplicateResponse);
  }
}
