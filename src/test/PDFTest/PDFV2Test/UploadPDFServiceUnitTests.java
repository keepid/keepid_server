package PDFTest.PDFV2Test;

import static PDFTest.PDFTestUtils.resourcesFolderPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.User.UserDao;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.*;

@Slf4j
public class UploadPDFServiceUnitTests {
  private FileDao fileDao;
  private FormDao formDao;
  private UserDao userDao;
  private UserParams exampleUserParams;
  private FileParams exampleFileParams;
  private MongoDatabase db;
  private EncryptionController encryptionController;
  private FileInputStream sampleFileStream;

  @BeforeClass
  public static void startDatabaseConnection() {
    TestUtils.startServer();
  }

  @Before
  public void initialize() {
    fileDao = FileDaoFactory.create(DeploymentLevel.TEST);
    db = MongoConfig.getDatabase(DeploymentLevel.TEST);
    File sampleFile = new File(resourcesFolderPath + File.separator + "1_converted.pdf");
    try {
      sampleFileStream = FileUtils.openInputStream(sampleFile);
    } catch (IOException e) {
      log.error("Opening sampleFileStream failed");
    }
    exampleUserParams = new UserParams("username1", "org1", UserType.Client);
    exampleFileParams =
        new FileParams(
            null,
            PDFTypeV2.BLANK_APPLICATION,
            "file1",
            "application/pdf",
            sampleFileStream,
            IdCategoryType.NONE,
            false);
    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {
      log.error("Generating test encryption controller failed");
    }
  }

  @After
  public void reset() {
    fileDao.clear();
    exampleFileParams = null;
    exampleUserParams = null;
    try {
      sampleFileStream.close();
    } catch (IOException e) {
      log.error("Closing sampleFileStream failed");
    }
    MongoConfig.dropDatabase(DeploymentLevel.TEST);
  }

  @AfterClass
  public static void closeDatabaseConnection() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void uploadPDFServiceNullPDFType() {
    exampleFileParams.setPdfType(null);
    UploadPDFServiceV2 service =
        new UploadPDFServiceV2(fileDao, exampleUserParams, exampleFileParams, encryptionController);
    Message response = service.executeAndGetResponse();
    assertEquals(PdfMessage.INVALID_PDF_TYPE, response);
  }
}
