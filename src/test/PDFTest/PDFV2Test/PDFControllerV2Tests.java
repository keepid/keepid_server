package PDFTest.PDFV2Test;

import Config.DeploymentLevel;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.TestUtils;
import org.junit.*;
import org.junit.jupiter.api.AfterAll;

public class PDFControllerV2Tests {
  private FileDao fileDao;
  private FormDao formDao;
  private UserDao userDao;

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
  }

  @Before
  public void initialize() {
    fileDao = FileDaoFactory.create(DeploymentLevel.TEST);
    formDao = FormDaoFactory.create(DeploymentLevel.TEST);
    userDao = UserDaoFactory.create(DeploymentLevel.TEST);
  }

  @After
  public void reset() {
    fileDao.clear();
    formDao.clear();
    userDao.clear();
    TestUtils.logout();
  }

  @AfterAll
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  // ------------------ UPLOAD PDF TESTS ------------------ //
}
