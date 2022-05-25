package FormTest;

import Config.DeploymentLevel;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Form.Form;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;

import static FormTest.FormControllerIntegrationTestHelperMethods.*;

public class FormControllerIntegrationTests {
  public static String username = "adminBSM";
  private static FormDao formDao;
  private static Form formObject;

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
    formDao = FormDaoFactory.create(DeploymentLevel.TEST);
    Form form = EntityFactory.createForm().withUsername("testUsername").buildAndPersist(formDao);
    formObject = form;
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void uploadFormTest() {
    TestUtils.login(username, username);
    uploadForm(formObject);
    TestUtils.logout();
  }

  @Test
  public void updateFormTest() {
    TestUtils.login(username, username);
    uploadForm(formObject);
    formObject.setUploadedAt(new Date());
    updateForm(formObject);
    TestUtils.logout();
  }

  @Test
  public void deleteFormTest() {
    TestUtils.login(username, username);
    uploadForm(formObject);
    delete(formObject.getId());
    TestUtils.logout();
  }

  @Test
  public void getFormTest() {
    TestUtils.login(username, username);
    uploadForm(formObject);
    getForm(formObject.getId());
    TestUtils.logout();
  }
}
