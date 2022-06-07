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
  // private static Form formObject;

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
    formDao = FormDaoFactory.create(DeploymentLevel.TEST);
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void uploadFormTest() {
    TestUtils.login(username, username);
    Form form = EntityFactory.createForm().withUsername(username).build();
    uploadForm(form);
    TestUtils.logout();
  }

  @Test
  public void updateFormTest() {
    TestUtils.login(username, username);
    Form form = EntityFactory.createForm().withUsername(username).build();
    uploadForm(form);
    form.setUploadedAt(new Date());
    updateForm(form);
    TestUtils.logout();
  }

  @Test
  public void deleteFormTest() {
    TestUtils.login(username, username);
    Form form = EntityFactory.createForm().withUsername(username).buildAndPersist(formDao);
    // uploadForm(form);
    delete(form.getId());
    TestUtils.logout();
  }

  @Test
  public void getFormTest() {
    TestUtils.login(username, username);
    Form form = EntityFactory.createForm().withUsername(username).buildAndPersist(formDao);
    // uploadForm(form);
    getForm(form.getId());
    TestUtils.logout();
  }

  @Test
  public void getAllFormsTest() {
    TestUtils.login(username, username);
    Form form = EntityFactory.createForm().withUsername(username).buildAndPersist(formDao);
    Form form2 = EntityFactory.createForm().withUsername(username).buildAndPersist(formDao);
    Form form3 = EntityFactory.createForm().withUsername(username).buildAndPersist(formDao);
    // uploadForm(form);
    getAllForms();
    TestUtils.logout();
  }
}
