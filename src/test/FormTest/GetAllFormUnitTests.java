package FormTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Form.Form;
import Form.FormMessage;
import Form.Services.GetAllFormsService;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.Services.GetMembersService;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class GetAllFormUnitTests {

  private FormDao formDao;

  /*
  List<User> users = new ArrayList<>();
   */

  private GetMembersService getMembersService;
  private ObjectId formId1;
  private ObjectId formId2;
  private ObjectId formId3;
  private Form formObject1;
  private Form formObject2;
  private Form formObject3;

  @Before
  public void setup() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
    formDao = FormDaoFactory.create(DeploymentLevel.TEST);
    Form form1 = EntityFactory.createForm().withUsername("testUsername").buildAndPersist(formDao);
    formObject1 = form1;
    formId1 = form1.getId();
    Form form2 = EntityFactory.createForm().withUsername("testUsername").buildAndPersist(formDao);
    formObject2 = form2;
    formId2 = form2.getId();
    Form form3 = EntityFactory.createForm().withUsername("testUsername").buildAndPersist(formDao);
    formObject3 = form3;
    formId3 = form3.getId();
  }

  @After
  public void reset() {
    formDao.clear();
  }

  @Test
  public void UsernameNotExistant() {
    GetAllFormsService getAllFormsService =
        new GetAllFormsService(formDao, "someUsername", UserType.Admin, false);
    Message result = getAllFormsService.executeAndGetResponse();
    assertEquals(FormMessage.SUCCESS, result);
    JSONArray formArray = getAllFormsService.getJsonInformation();
    assertEquals(formArray.length(), 0);
  }

  // for now, I have hte test privelege level be null, it seems all privelges on the
  // actuall services are sufficient, not sure if this is intentional.
  @Test
  public void InsufficientPrivilege() {
    GetAllFormsService getAllFormsService =
        new GetAllFormsService(formDao, "someUsername", null, false);
    Message result = getAllFormsService.executeAndGetResponse();
    assertEquals(FormMessage.INSUFFICIENT_PRIVILEGE, result);
  }

  @Test
  public void FormExistsAndSufficientPrivilege() {
    GetAllFormsService getAllFormsService =
        new GetAllFormsService(formDao, "testUsername", UserType.Admin, false);
    Message result = getAllFormsService.executeAndGetResponse();
    assertEquals(FormMessage.SUCCESS, result);
    JSONArray formArray = getAllFormsService.getJsonInformation();
    assertEquals(formArray.length(), 3);

    JSONObject jsonObject1 = formArray.getJSONObject(0);
    Form form1 = Form.fromJson(jsonObject1);
    assertEquals(formObject1.getId().toString(), form1.getId().toString());
    assertEquals(formObject1.getUsername(), form1.getUsername());

    JSONObject jsonObject2 = formArray.getJSONObject(1);
    Form form2 = Form.fromJson(jsonObject2);
    assertEquals(formObject2.getId().toString(), form2.getId().toString());
    assertEquals(formObject2.getUsername(), form2.getUsername());

    JSONObject jsonObject3 = formArray.getJSONObject(2);
    Form form3 = Form.fromJson(jsonObject3);
    assertEquals(formObject3.getId().toString(), form3.getId().toString());
    assertEquals(formObject3.getUsername(), form3.getUsername());
  }
}
