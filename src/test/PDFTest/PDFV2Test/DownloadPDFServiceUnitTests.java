package PDFTest.PDFV2Test;

import static PDFTest.PDFTestUtils.resourcesFolderPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import File.FileType;
import File.IdCategoryType;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import PDF.Services.V2Services.DownloadPDFServiceV2;
import PDF.Services.V2Services.UploadPDFServiceV2;
import Security.EncryptionController;
import TestUtils.TestUtils;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.*;

@Slf4j
public class DownloadPDFServiceUnitTests {
  private FileDao fileDao;
  private FormDao formDao;
  private MongoDatabase db;
  private EncryptionController encryptionController;
  private UserParams clientOneUserParams;
  private FileParams uploadFileOneFileParams;
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
    this.formDao = FormDaoFactory.create(DeploymentLevel.TEST);
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
      log.error("Generating test encryption controller failed");
    }
    this.clientOneUserParams =
        new UserParams()
            .setUsername("client1")
            .setOrganizationName("org1")
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

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void downloadPDFServiceNullPDFType() {
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(
            fileDao, clientOneUserParams, uploadFileOneFileParams, encryptionController);
    uploadService.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    ObjectId fileObjectId = fileDao.get("client1", FileType.IDENTIFICATION_PDF).get().getId();
    String fileId = fileObjectId.toString();
    FileParams downloadFileOneParams =
        new FileParams().setFileId(fileId.toString()).setPdfType(null);
    DownloadPDFServiceV2 downloadService =
        new DownloadPDFServiceV2(
            fileDao, formDao, clientOneUserParams, downloadFileOneParams, encryptionController);
    Message response = downloadService.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void downloadPDFServiceMissingFile() {
    FileParams downloadFileOneParams =
        new FileParams()
            .setFileId(new ObjectId().toString())
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT);
    DownloadPDFServiceV2 downloadService =
        new DownloadPDFServiceV2(
            fileDao, formDao, clientOneUserParams, downloadFileOneParams, encryptionController);
    Message response = downloadService.executeAndGetResponse();
    assertEquals(PdfMessage.NO_SUCH_FILE, response);
  }

  @Test
  public void downloadPDFServiceJustFileSuccess() {
    UploadPDFServiceV2 uploadService =
        new UploadPDFServiceV2(
            fileDao, clientOneUserParams, uploadFileOneFileParams, encryptionController);
    uploadService.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    ObjectId fileObjectId = fileDao.get("client1", FileType.IDENTIFICATION_PDF).get().getId();
    InputStream encryptedInputStream = fileDao.getStream(fileObjectId).get();
    InputStream expectedInputStream = null;
    try {
      expectedInputStream = encryptionController.decryptFile(encryptedInputStream, "client1");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    String fileId = fileObjectId.toString();
    FileParams downloadFileParams =
        new FileParams()
            .setFileId(fileId.toString())
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT);
    DownloadPDFServiceV2 downloadService =
        new DownloadPDFServiceV2(
            fileDao, formDao, clientOneUserParams, downloadFileParams, encryptionController);
    Message response = downloadService.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    assertEquals(PdfMessage.SUCCESS, response);
    InputStream downloadedInputStream = downloadService.getDownloadedInputStream();
    try {
      assertTrue(IOUtils.contentEquals(expectedInputStream, downloadedInputStream));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
