package PDFTest.PDFV2Test;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.User.UserDao;
import Security.EncryptionController;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;

@Slf4j
public class UploadPDFServiceUnitTests {
  private FileDao fileDao;
  private FormDao formDao;
  private UserDao userDao;
  private MongoDatabase db;
  private EncryptionController encryptionController;

  @BeforeClass
  public static void startDatabaseConnection() {
    MongoConfig.getMongoClient();
  }

  @Before
  public void initialize() {
    fileDao = FileDaoFactory.create(DeploymentLevel.TEST);
    db = MongoConfig.getDatabase(DeploymentLevel.TEST);
    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {
      log.error("Generating test encryption controller failed");
    }
  }

  @After
  public void reset() {
    fileDao.clear();
    MongoConfig.dropDatabase(DeploymentLevel.TEST);
  }

  @AfterClass
  public static void closeDatabaseConnection() {
    MongoConfig.closeClientConnection();
  }
}
