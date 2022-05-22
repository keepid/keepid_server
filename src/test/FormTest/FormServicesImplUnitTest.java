package FormTest;

import Config.DeploymentLevel;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Form.Form;
import Form.FormMessage;
import Form.Services.DeleteFormService;
import Form.Services.GetFormService;
import Form.Services.UpdateFormService;
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
    TestUtils.setUpTestDB();
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
    int spacesToIndentEachLevel = 2;
    String jsonString = formJson.toString(spacesToIndentEachLevel);
    System.out.println(jsonString);

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
    public void update() {
        String testUsername = "username1";
        Form form = EntityFactory.createForm().withUsername(testUsername).build();
        JSONObject formJson = form.toJSON();
        int spacesToIndentEachLevel = 2;
        String jsonString = formJson.toString(spacesToIndentEachLevel);
        System.out.println(jsonString);
        UserType testUserType = UserType.userTypeFromString("worker");
        UploadFormService uploadFormService =
                new UploadFormService(formDao, testUsername, testUserType, formJson);
        assertEquals(FormMessage.SUCCESS, uploadFormService.executeAndGetResponse());

        System.out.println("test");
        System.out.println(formDao.get(testUsername));

        Form updatedForm = EntityFactory.createForm().withUsername(testUsername).updatedBuild(form.getId(), form.getFileId());
        JSONObject updatedFormJson = updatedForm.toJSON();
        String updatedJsonString = updatedFormJson.toString(spacesToIndentEachLevel);
        System.out.println(updatedJsonString);
        System.out.println("hello");
        System.out.println(formDao.get(testUsername).get(0).toJSON());

        UpdateFormService updateFormService =
                new UpdateFormService(formDao, testUsername, testUserType, updatedFormJson);
        assertEquals(FormMessage.SUCCESS, updateFormService.executeAndGetResponse());

        System.out.println("updated test");
        System.out.println(formDao.get(testUsername).get(0).toJSON());
        assertEquals(formDao.get(testUsername).get(0).getUsername(), updatedForm.getUsername());
        //assertEquals(
                //formDao.get(testUsername).get(0).getUploaderUsername(), updatedForm.getUploaderUsername());
        assertEquals(formDao.get(testUsername).get(0).getLastModifiedAt(), updatedForm.getLastModifiedAt());
        assertEquals(formDao.get(testUsername).get(0).getUploadedAt(), updatedForm.getUploadedAt());
        assertEquals(formDao.get(testUsername).get(0).getBody(), updatedForm.getBody());
        assertEquals(formDao.get(testUsername).get(0).getMetadata(), updatedForm.getMetadata());
        assertEquals(formDao.get(testUsername).get(0).getFormType(), updatedForm.getFormType());
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

    int spacesToIndentEachLevel = 2;
    String jsonString = jsonInformation.toString(spacesToIndentEachLevel);
    System.out.println(jsonString);

    assertEquals(jsonInformation.get("id"), id.toString());
    assertEquals(jsonInformation.get("fileId"), form.getFileId().toString());
    assertEquals(jsonInformation.get("uploadedAt"), form.getUploadedAt().toString());
    assertEquals(jsonInformation.get("username"), testUsername);
    assertEquals(jsonInformation.get("uploaderUsername"), form.getUploaderUsername());
    assertEquals(jsonInformation.get("formType"), form.getFormType().toString());

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
