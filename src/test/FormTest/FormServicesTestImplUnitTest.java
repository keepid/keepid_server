package FormTest;

import Config.DeploymentLevel;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Form.Form;
import Form.FormMessage;
import Form.Services.DeleteFormService;
import Form.Services.GetFormService;
import Form.Services.UploadFormService;
import TestUtils.EntityFactory;
import User.UserType;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormServicesTestImplUnitTest {
  public FormDao formDao;

  @Before
  public void initialize() {
    this.formDao = FormDaoFactory.create(DeploymentLevel.IN_MEMORY);
  }

  @After
  public void reset() {
    this.formDao.clear();
  }

  @Test
  public void upload() {
    String testUsername = "username1";
    UserType testUserType = UserType.userTypeFromString("worker");
    Form form = EntityFactory.createForm().withUsername(testUsername).build();
    JSONObject formJson = form.toJSON();
    UploadFormService uploadFormService =
        new UploadFormService(formDao, testUsername, testUserType, formJson);
    assertEquals(FormMessage.SUCCESS, uploadFormService.executeAndGetResponse());
    System.out.println(formDao.get(testUsername).size());
    assertTrue(formDao.get(testUsername).size() == 1);
  }

  @Test
  public void delete() {
    String testUsername = "username1";
    UserType testUserType = UserType.userTypeFromString("worker");
    Form form = EntityFactory.createForm().withUsername(testUsername).build();
    formDao.save(form);
    assertTrue(formDao.get(testUsername).size() == 1);
    ObjectId id = form.getId();
    DeleteFormService deleteFormService =
        new DeleteFormService(formDao, id, testUsername, testUserType, false);
    assertEquals(FormMessage.SUCCESS, deleteFormService.executeAndGetResponse());
    System.out.println(formDao.get(testUsername).size());
    assertTrue(formDao.get(testUsername).size() == 0);
  }

  @Test
  public void get() {
    String testUsername = "username1";
    UserType testUserType = UserType.userTypeFromString("worker");
    Form form = EntityFactory.createForm().withUsername(testUsername).build();
    formDao.save(form);
    assertTrue(formDao.get(testUsername).size() == 1);
    ObjectId id = form.getId();
    GetFormService getFormService =
        new GetFormService(formDao, id, testUsername, testUserType, false);
    assertEquals(FormMessage.SUCCESS, getFormService.executeAndGetResponse());
  }
}
