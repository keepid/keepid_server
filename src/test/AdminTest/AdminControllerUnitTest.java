package AdminTest;

import Admin.AdminController;
import Config.MongoTestConfig;
import Database.Activity.ActivityDao;
import Database.Organization.OrgDao;
import Database.User.UserDao;
import io.javalin.http.Context;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class AdminControllerUnitTest {

  private UserDao userDao = mock(UserDao.class);
  private ActivityDao activityDao = mock(ActivityDao.class);
  private OrgDao orgDao = mock(OrgDao.class);
  private MongoTestConfig mongoTestConfig = mock(MongoTestConfig.class);
  private Context ctx = mock(Context.class);
  private AdminController testSubject;

  @Before
  public void setup() {
    testSubject = new AdminController(userDao, activityDao, orgDao, mongoTestConfig);
  }

  @Test
  public void deleteOrg() {
    when(ctx.body()).thenReturn("{ \"orgName\": \"testOrg\" }");
    testSubject.deleteOrg(ctx);
    verify(orgDao).delete("testOrg");
    verify(userDao).deleteAllFromOrg("testOrg");
    verify(activityDao).deleteAllFromOrg("testOrg");
  }
}
