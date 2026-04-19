package PDFTest.PDFV2Test;

import static PDFTest.PDFV2Test.PDFTestUtilsV2.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Database.Organization.OrgDao;
import Database.Organization.OrgDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import PDF.Services.V2Services.GetQuestionsPDFServiceV2;
import Security.EncryptionController;
import TestUtils.EntityFactory;
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
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;
import org.junit.jupiter.api.AfterAll;

public class GetQuestionsPDFServiceV2UnitTests {
  private FileDao fileDao;
  private FormDao formDao;
  private OrgDao orgDao;
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
  public void initialize() throws InterruptedException {
    Thread.sleep(1000);
    this.fileDao = FileDaoFactory.create(DeploymentLevel.TEST);
    this.formDao = FormDaoFactory.create(DeploymentLevel.TEST);
    this.orgDao = OrgDaoFactory.create(DeploymentLevel.TEST);
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
      this.userDao.save(
          new User(
              new Name("workerFirstName", "workerLastName"),
              "12-12-2012",
              "worker@keep.id",
              "2153334444",
              "org2",
              new Address("2 Keep Ave", "Keep", "PA", "11111"),
              false,
              "worker1",
              "workerPass123",
              UserType.Worker));
      this.userDao.save(
          new User(
              new Name("adminFirstName", "adminLastName"),
              "12-12-2012",
              "admin@keep.id",
              "2154445555",
              "org2",
              new Address("3 Keep Ave", "Keep", "PA", "11111"),
              false,
              "admin1",
              "adminPass123",
              UserType.Admin));
    } catch (ValidationException e) {
      throw new RuntimeException(e);
    }
    EntityFactory.createOrganization()
        .withOrgName("org2")
        .withAddress("311 Broad Street")
        .withCity("Philadelphia")
        .withState("PA")
        .withZipcode("19107")
        .withPhoneNumber("1234567890")
        .withEmail("org@example.com")
        .withWebsite("https://www.example.org")
        .withEIN("123456789")
        .buildAndPersist(orgDao);
    orgDao
        .get("org2")
        .ifPresent(
            organization -> {
              organization.setDesignatedDirectorUsername("admin1");
              orgDao.update(organization);
            });
    this.developerUserParams =
        new UserParams()
            .setUsername("dev1")
            .setOrganizationName("org2")
            .setPrivilegeLevel(UserType.Developer);
    this.clientUserParams =
        new UserParams()
            .setUsername("client1")
            .setActorUsername("worker1")
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
    orgDao.clear();
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
  public void getQuestionsPDFMissingForm() {
    ObjectId fakeObjectId = new ObjectId();
    FileParams getQuestionsFileParams = new FileParams().setFileId(fakeObjectId.toString());
    GetQuestionsPDFServiceV2 getService =
        new GetQuestionsPDFServiceV2(
            formDao, orgDao, userDao, clientUserParams, getQuestionsFileParams);
    Message response = getService.executeAndGetResponse();
    assertEquals(PdfMessage.MISSING_FORM, response);
  }

  @Test
  public void getQuestionsPDFSuccess() {
    ObjectId uploadedFileId =
        uploadBlankSSFormAndGetFileId(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    FileParams getQuestionsFileParams = new FileParams().setFileId(uploadedFileId.toString());
    GetQuestionsPDFServiceV2 getService =
        new GetQuestionsPDFServiceV2(
            formDao, orgDao, userDao, clientUserParams, getQuestionsFileParams);
    Message response = getService.executeAndGetResponse();
    assertEquals(PdfMessage.SUCCESS, response);
    JSONArray fields = (JSONArray) getService.getApplicationInformation().get("fields");
    List<String> fieldNames = getFieldNamesFromFields(fields);
    // First field, readOnlyField
    assertTrue(fieldNames.contains("topmostSubform[0].Page1[0].First[0]"));
    // textField
    assertTrue(fieldNames.contains("topmostSubform[0].Page5[0].Middlename[0]"));
    // checkBox
    assertTrue(fieldNames.contains("topmostSubform[0].Page5[0].hawaiian[0]"));
    assertEquals(70, fields.length());
  }

  @Test
  public void getQuestionsReturnsNestedResolvedProfiles() {
    ObjectId uploadedFileId =
        uploadBlankSSFormAndGetFileId(
            fileDao, formDao, userDao, developerUserParams, blankFileParams, encryptionController);
    FileParams getQuestionsFileParams = new FileParams().setFileId(uploadedFileId.toString());
    GetQuestionsPDFServiceV2 getService =
        new GetQuestionsPDFServiceV2(
            formDao, orgDao, userDao, clientUserParams, getQuestionsFileParams);
    Message response = getService.executeAndGetResponse();

    assertEquals(PdfMessage.SUCCESS, response);

    JSONObject resolvedProfiles =
        getService.getApplicationInformation().getJSONObject("resolvedProfiles");
    JSONObject clientProfile = resolvedProfiles.getJSONObject("client");
    JSONObject workerProfile = resolvedProfiles.getJSONObject("worker");
    JSONObject orgProfile = resolvedProfiles.getJSONObject("org");
    JSONObject directorProfile = resolvedProfiles.getJSONObject("director");

    assertEquals("testcFirstName", clientProfile.getJSONObject("currentName").getString("first"));
    assertFalse(clientProfile.has("currentName.first"));
    assertEquals("workerFirstName", workerProfile.getJSONObject("currentName").getString("first"));
    assertEquals("org2", orgProfile.getString("organizationName"));
    assertEquals("Philadelphia", orgProfile.getJSONObject("address").getString("city"));
    assertEquals("1234567890", orgProfile.getString("phoneNumber"));
    assertEquals("adminFirstName", directorProfile.getJSONObject("currentName").getString("first"));
  }
}
