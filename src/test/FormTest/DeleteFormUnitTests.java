package FormTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Form.Form;
import Form.FormMessage;
import Form.Services.DeleteFormService;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.Services.GetMembersService;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class DeleteFormUnitTests {

  private FormDao formDao;

  /*
  List<User> users = new ArrayList<>();
   */

  private GetMembersService getMembersService;
  private ObjectId formId;

  @Before
  public void setup() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
    formDao = FormDaoFactory.create(DeploymentLevel.TEST);
    Form form = EntityFactory.createForm().withUsername("testUsername").buildAndPersist(formDao);
    formId = form.getId();
  }

  @After
  public void reset() {
    formDao.clear();
  }

  @Test
  public void FormNotExistant() {
    DeleteFormService deleteFormService =
        new DeleteFormService(formDao, new ObjectId(), "someUsername", UserType.Admin, false);
    Message result = deleteFormService.executeAndGetResponse();
    assertEquals(FormMessage.FORM_NOT_FOUND, result);
  }

  // for now, I have hte test privelege level be null, it seems all privelges on the
  // actuall services are sufficient, not sure if this is intentional.
  @Test
  public void InsufficientPrivilege() {
    DeleteFormService deleteFormService =
        new DeleteFormService(formDao, new ObjectId(), "someUsername", null, false);
    Message result = deleteFormService.executeAndGetResponse();
    assertEquals(FormMessage.INSUFFICIENT_PRIVILEGE, result);
  }

  @Test
  public void FormExistsAndSufficientPrivilege() {
    DeleteFormService deleteFormService =
        new DeleteFormService(formDao, formId, "someUsername", UserType.Admin, false);
    Message result = deleteFormService.executeAndGetResponse();
    assertEquals(FormMessage.SUCCESS, result);
    assertEquals(formDao.size(), 0);
  }
}
