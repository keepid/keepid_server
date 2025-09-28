package FileTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.User;
import User.UserType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static FileTest.FileControllerIntegrationTestHelperMethods.uploadTestPDF;

public class FileControllerIntegrationTests {
  private final UserDao userDao = UserDaoFactory.create(DeploymentLevel.TEST);

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
  }

  @After
  public void clear() {
    if (userDao != null) {
      userDao.clear();
    }
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void uploadValidPDFTest() {
    String username = "username1";
    String password = "password1";
    User user =
        EntityFactory.createUser()
            .withUsername(username)
            .withPasswordToHash(password)
            .withUserType(UserType.Client)
            .buildAndPersist(userDao);
    TestUtils.login(username, password);
    uploadTestPDF();
    TestUtils.logout();
  }
}
