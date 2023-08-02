package PDFTest.PDFV2Test;

import static PDFTest.PDFTestUtils.resourcesFolderPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import PDF.Services.V2Services.DeletePDFServiceV2;
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
import org.bson.types.ObjectId;
import org.junit.*;

@Slf4j
public class DeletePDFServiceUnitTests {
  private FileDao fileDao;
  private FormDao formDao;
  private MongoDatabase db;
  private EncryptionController encryptionController;
  private InputStream sampleFileStream;
  private InputStream sampleAnnotatedFileStream;
  private InputStream sampleImageStream;

  @BeforeClass
  public static void start() {
    TestUtils.startServer();
  }

  @Before
  public void initialize() {
    this.fileDao = FileDaoFactory.create(DeploymentLevel.TEST);
    this.formDao = FormDaoFactory.create(DeploymentLevel.TEST);
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
    formDao.clear();
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
  public void deletePDFServiceNullPDFType() {
    UserParams userParams =
        new UserParams()
            .setUsername("user1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Client);
    FileParams uploadFileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("first-love.png")
            .setFileContentType("image")
            .setFileStream(sampleImageStream)
            .setIdCategoryType(IdCategoryType.OTHER);
    UploadPDFServiceV2 service =
        new UploadPDFServiceV2(fileDao, userParams, uploadFileParams, encryptionController);
    service.executeAndGetResponse();
    String fileId = fileDao.get("user1", FileType.IDENTIFICATION_PDF).get().getId().toString();
    FileParams deleteFileParams = new FileParams().setFileId(fileId).setPdfType(null);
    DeletePDFServiceV2 service1 =
        new DeletePDFServiceV2(fileDao, formDao, userParams, deleteFileParams);
    Message response = service1.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void deletePDFServiceNoSuchFile() {
    UserParams userParams =
        new UserParams()
            .setUsername("user1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Client);
    FileParams deleteFileParams =
        new FileParams()
            .setFileId(new ObjectId().toString())
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT);
    DeletePDFServiceV2 service1 =
        new DeletePDFServiceV2(fileDao, formDao, userParams, deleteFileParams);
    Message response = service1.executeAndGetResponse();
    assertEquals(PdfMessage.NO_SUCH_FILE, response);
  }

  @Test
  public void deletePDFServiceInsufficientPrivilege() {
    UserParams userParams =
        new UserParams()
            .setUsername("user1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Director);
    FileParams uploadFileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("first-love.png")
            .setFileContentType("image")
            .setFileStream(sampleImageStream)
            .setIdCategoryType(IdCategoryType.OTHER);
    UploadPDFServiceV2 service =
        new UploadPDFServiceV2(fileDao, userParams, uploadFileParams, encryptionController);
    service.executeAndGetResponse();
    String fileId = fileDao.get("user1", FileType.IDENTIFICATION_PDF).get().getId().toString();
    FileParams deleteFileParams =
        new FileParams().setFileId(fileId).setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT);
    DeletePDFServiceV2 service1 =
        new DeletePDFServiceV2(fileDao, formDao, userParams, deleteFileParams);
    Message response = service1.executeAndGetResponse();
    assertEquals(PdfMessage.INSUFFICIENT_PRIVILEGE, response);
  }

  @Test
  public void deletePDFServiceClientDocumentSuccess() {
    UserParams userParams =
        new UserParams()
            .setUsername("user1")
            .setOrganizationName("org1")
            .setPrivilegeLevel(UserType.Client);
    FileParams uploadFileParams =
        new FileParams()
            .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT)
            .setFileName("first-love.png")
            .setFileContentType("image")
            .setFileStream(sampleImageStream)
            .setIdCategoryType(IdCategoryType.OTHER);
    UploadPDFServiceV2 service =
        new UploadPDFServiceV2(fileDao, userParams, uploadFileParams, encryptionController);
    service.executeAndGetResponse();
    assertEquals(1, fileDao.size());
    String fileId = fileDao.get("user1", FileType.IDENTIFICATION_PDF).get().getId().toString();
    FileParams deleteFileParams =
        new FileParams().setFileId(fileId).setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT);
    DeletePDFServiceV2 service1 =
        new DeletePDFServiceV2(fileDao, formDao, userParams, deleteFileParams);
    Message response = service1.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
    assertEquals(0, fileDao.size());
  }

  // NEED TO TEST DELETE ON ANNOTATED AND BLANK APPS
}
