package PDFTest.PDFV2Test;

import static PDFTest.PDFV2Test.PDFTestUtilsV2.*;
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
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.jupiter.api.AfterAll;

public class UploadPDFServiceUnitTests {
  private FileDao fileDao;
  private MongoDatabase db;
  private EncryptionController encryptionController;
  private UserParams clientOneUserParams;
  private InputStream sampleFileStream1;
  private InputStream sampleFileStream2;
  private InputStream sampleImageStream;
  private InputStream sampleAnnotatedFileStream;
  private InputStream sampleBlankFileStream1;
  private InputStream sampleBlankFileStream2;

  @BeforeClass
  public static void start() {
    TestUtils.startServer();
  }

  @Before
  public void initialize() {
    this.fileDao = FileDaoFactory.create(DeploymentLevel.TEST);
    this.db = MongoConfig.getDatabase(DeploymentLevel.TEST);
    File sampleBlankFile1 = new File(resourcesFolderPath + File.separator + "ss-5.pdf");
    File sampleBlankFile2 =
        new File(resourcesFolderPath + File.separator + "Application_for_a_Birth_Certificate.pdf");
    File sampleAnnotatedFile =
        new File(resourcesFolderPath + File.separator + "ss-5_filled_out.pdf");
    File sampleImageFile = new File(resourcesFolderPath + File.separator + "first-love.png");
    File sampleFile1 = new File(resourcesFolderPath + File.separator + "test_out_signature.pdf");
    File sampleFile2 = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
    try {
      sampleImageStream = FileUtils.openInputStream(sampleImageFile);
      sampleFileStream1 = FileUtils.openInputStream(sampleFile1);
      sampleFileStream2 = FileUtils.openInputStream(sampleFile2);
      sampleAnnotatedFileStream = FileUtils.openInputStream(sampleAnnotatedFile);
      sampleBlankFileStream1 = FileUtils.openInputStream(sampleBlankFile1);
      sampleBlankFileStream2 = FileUtils.openInputStream(sampleBlankFile2);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    this.clientOneUserParams =
        new UserParams()
            .setUsername("client1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Client);
  }

  @After
  public void reset() {
    fileDao.clear();
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
  public void uploadPDFServiceNullPDFType() {
    FileParams fileOneParams =
        new FileParams()
            .setPdfType(null)
            .setFileName("test_out_signature")
            .setFileContentType("application/pdf")
            .setFileStream(sampleFileStream1)
            .setIdCategoryType(IdCategoryType.NONE);
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(fileDao, clientOneUserParams, fileOneParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(0, fileDao.size());
    assertEquals(PdfMessage.INVALID_PDF_TYPE, response);
  }

  @Test
  public void uploadPDFServiceNullFileStream() {
    FileParams fileOneParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("test_out_signature.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(null)
            .setIdCategoryType(IdCategoryType.NONE);
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(fileDao, clientOneUserParams, fileOneParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(0, fileDao.size());
    assertEquals(PdfMessage.INVALID_PDF, response);
  }

  @Test
  public void uploadPDFServiceInvalidFileContentType() {
    FileParams fileOneParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("test_out_signature.pdf")
            .setFileContentType("invalid")
            .setFileStream(sampleFileStream1)
            .setIdCategoryType(IdCategoryType.NONE);
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(fileDao, clientOneUserParams, fileOneParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(0, fileDao.size());
    assertEquals(PdfMessage.INVALID_PDF, response);
  }

  @Test
  public void uploadPDFServiceImageSuccess() {
    FileParams imageFileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("first-love.png")
            .setFileContentType("image")
            .setFileStream(sampleImageStream)
            .setIdCategoryType(IdCategoryType.OTHER);
    assertEquals(0, fileDao.size());
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(fileDao, clientOneUserParams, imageFileParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
    assertEquals(1, fileDao.size());
  }

  @Test
  public void uploadPDFServiceBlankPDFFailure() {
    UserParams worker1Params =
        new UserParams()
            .setUsername("worker1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Worker);
    FileParams blankFileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.BLANK_APPLICATION)
            .setFileName("ss-5.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleBlankFileStream1)
            .setIdCategoryType(IdCategoryType.NONE);
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(fileDao, worker1Params, blankFileParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(0, fileDao.size());
    assertEquals(PdfMessage.INVALID_PDF_TYPE, response);
  }

  @Test
  public void uploadPDFServiceAnnotatedPDFFailure() {
    FileParams annotatedFileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.ANNOTATED_APPLICATION)
            .setFileName("ss-5_filled_out.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleAnnotatedFileStream)
            .setIdCategoryType(IdCategoryType.NONE);
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(
            fileDao, clientOneUserParams, annotatedFileParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(0, fileDao.size());
    assertEquals(PdfMessage.INVALID_PDF_TYPE, response);
  }

  @Test
  public void uploadPDFServiceDuplicateUpload() {
    FileParams fileOneParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("test_out_signature.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(sampleFileStream1)
            .setIdCategoryType(IdCategoryType.OTHER);
    File duplicateSampleFile =
        new File(resourcesFolderPath + File.separator + "test_out_signature.pdf");
    InputStream duplicateSampleFileStream = null;
    try {
      duplicateSampleFileStream = FileUtils.openInputStream(duplicateSampleFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    FileParams fileOneParamsDuplicate =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("test_out_signature.pdf")
            .setFileContentType("application/pdf")
            .setFileStream(duplicateSampleFileStream)
            .setIdCategoryType(IdCategoryType.OTHER);
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(fileDao, clientOneUserParams, fileOneParams, encryptionController);
    Message response = uploadService.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    assertEquals(PdfMessage.SUCCESS, response);
    UploadPDFServiceV2 uploadDuplicateService =
        new UploadPDFServiceV2(
            fileDao, clientOneUserParams, fileOneParamsDuplicate, encryptionController);
    Message duplicateResponse = uploadDuplicateService.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    assertEquals(FileMessage.FILE_EXISTS, duplicateResponse);
  }
}
