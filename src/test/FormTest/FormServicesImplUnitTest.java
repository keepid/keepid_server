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
import TestUtils.TestUtils;
import User.UserType;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormServicesImplUnitTest {
  public FormDao formDao;

  @Before
  public void initialize() {
    TestUtils.startServer();
    this.formDao = FormDaoFactory.create(DeploymentLevel.TEST);
  }

  @After
  public void reset() {
    formDao.clear();
  }

  @Test
  public void upload() {
    String testUsername = "username1";
    Form form = EntityFactory.createForm().withUsername(testUsername).build();
    JSONObject formJson = form.toJSON();
    UserType testUserType = UserType.userTypeFromString("worker");
    UploadFormService uploadFormService =
        new UploadFormService(formDao, testUsername, testUserType, formJson);
    assertEquals(FormMessage.SUCCESS, uploadFormService.executeAndGetResponse());
    assertTrue(formDao.get(testUsername).size() == 1);
    assertEquals(formDao.get(testUsername).get(0).getUsername(), form.getUsername());
    assertEquals(
        formDao.get(testUsername).get(0).getUploaderUsername(), form.getUploaderUsername());
    assertEquals(formDao.get(testUsername).get(0).getLastModifiedAt(), form.getLastModifiedAt());
    assertEquals(formDao.get(testUsername).get(0).getUploadedAt(), form.getUploadedAt());
    assertEquals(formDao.get(testUsername).get(0).getBody(), form.getBody());
    assertEquals(formDao.get(testUsername).get(0).getMetadata(), form.getMetadata());
    assertEquals(formDao.get(testUsername).get(0).getFormType(), form.getFormType());
  }

  @Test
  public void get() {
    String testUsername = "username1";
    UserType testUserType = UserType.userTypeFromString("worker");
    Form form = EntityFactory.createForm().withUsername(testUsername).buildAndPersist(formDao);
    assertTrue(formDao.get(testUsername).size() == 1);
    ObjectId id = form.getId();
    GetFormService getFormService =
        new GetFormService(formDao, id, testUsername, testUserType, false);
    assertEquals(FormMessage.SUCCESS, getFormService.executeAndGetResponse());
    JSONObject jsonInformation = getFormService.getJsonInformation();
    //    assertEquals(jsonInformation.get("id"), id.); //TODO: will need to rewrite the form
    // service here
    //    assertEquals(jsonInformation.get("fileId"), form.getFileId().toString());
    assertEquals(jsonInformation.get("username"), testUsername);
    assertEquals(jsonInformation.get("uploaderUsername"), form.getUploaderUsername());
    assertEquals(jsonInformation.get("formType"), form.getFormType().toString().toUpperCase());
    assertTrue(formDao.get(testUsername).size() == 1);
  }

  @Test
  public void delete() {
    String testUsername = "username1";
    UserType testUserType = UserType.userTypeFromString("worker");
    Form form = EntityFactory.createForm().withUsername(testUsername).buildAndPersist(formDao);
    assertTrue(formDao.get(testUsername).size() == 1);
    ObjectId id = form.getId();
    DeleteFormService deleteFormService =
        new DeleteFormService(formDao, id, testUsername, testUserType, false);
    assertEquals(FormMessage.SUCCESS, deleteFormService.executeAndGetResponse());
    assertTrue(formDao.get(testUsername).size() == 0);
  }
}
