package PDFTest.PDFV2Test;

import static PDFTest.PDFV2Test.PDFTestUtilsV2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import File.IdCategoryType;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import PDF.Services.V2Services.FilterPDFServiceV2;
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

public class FilterPDFServiceUnitTests {

  private FileDao fileDao;
  private MongoDatabase db;
  private InputStream sampleFileStream1;
  private InputStream sampleFileStream2;
  private InputStream sampleImageStream;
  private InputStream sampleAnnotatedFileStream;
  private InputStream sampleBlankFileStream1;
  private InputStream sampleBlankFileStream2;
  private UserParams clientOneUserParams;
  private FileParams uploadFileOneFileParams;
  private EncryptionController encryptionController;

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
      System.out.println(sampleImageStream);
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
    this.fileDao.clear();
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

  private void uploadAllStreams() {
    //    UploadPDFServiceV2 uploadService;
    //    UserParams workerOneUserParams =
    //    new UserParams()
    //      .setUsername("worker1")
    //      .setOrganizationName("org1")
    //      .setPrivilegeLevel(UserType.Developer);
    //    for (InputStream fileStream : [sampleFileStream1, sampleBlankFileStream2,
    // sampleImageStream]) {
    //      uploadService = new UploadPDFServiceV2(
    //        fileDao, clientOneUserParams, uploadFileOneFileParams, encryptionController);
    //      uploadService.executeAndGetResponse();
    //    }
    // WAITING ON UPLOAD THINGS
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
    uploadAllStreams();
    assertEquals(6, fileDao.size());
  }
}
