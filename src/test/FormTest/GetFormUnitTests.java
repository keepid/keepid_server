package FormTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Form.Form;
import Form.FormMessage;
import Form.Services.GetFormService;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.Services.GetMembersService;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class GetFormUnitTests {

  private FormDao formDao;

  /*
  List<User> users = new ArrayList<>();
   */

  private GetMembersService getMembersService;
  private ObjectId formId;
  private Form formObject;

  @Before
  public void setup() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
    formDao = FormDaoFactory.create(DeploymentLevel.TEST);
    Form form = EntityFactory.createForm().withUsername("testUsername").buildAndPersist(formDao);
    formObject = form;
    formId = form.getId();
  }

  @After
  public void reset() {
    formDao.clear();
  }

  @Test
  public void FormNotExistant() {
    GetFormService getFormService =
        new GetFormService(formDao, new ObjectId(), "someUsername", UserType.Admin, false);
    Message result = getFormService.executeAndGetResponse();
    assertEquals(FormMessage.FORM_NOT_FOUND, result);
  }

  // for now, I have hte test privelege level be null, it seems all privelges on the
  // actuall services are sufficient, not sure if this is intentional.
  @Test
  public void InsufficientPrivilege() {
    GetFormService getFormService =
        new GetFormService(formDao, formId, "someUsername", null, false);
    Message result = getFormService.executeAndGetResponse();
    assertEquals(FormMessage.INSUFFICIENT_PRIVILEGE, result);
  }

  @Test
  public void FormExistsAndSufficientPrivilege() {
    GetFormService getFormService =
        new GetFormService(formDao, formId, "someUsername", UserType.Admin, false);
    Message result = getFormService.executeAndGetResponse();
    assertEquals(FormMessage.SUCCESS, result);
    JSONObject jsonObject = getFormService.getJsonInformation();
    Form form = Form.fromJson(jsonObject);
    assertEquals(formObject.getId().toString(), form.getId().toString());
    assertEquals(formObject.getUsername(), form.getUsername());
  }
}
