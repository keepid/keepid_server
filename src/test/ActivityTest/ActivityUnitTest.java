package ActivityTest;

import Activity.Activity;
import Activity.ActivityController;
import Database.Activity.ActivityDao;
import io.javalin.http.Context;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class ActivityUnitTest {

  private ActivityDao activityDao = mock(ActivityDao.class);
  private Context ctx = mock(Context.class);
  private ActivityController testSubject;

  @Before
  public void setup() {
    testSubject = new ActivityController(activityDao);
  }

  @Test
  public void addActivity() {
    Activity activity = new Activity();
    testSubject.addActivity(activity);
    verify(activityDao).save(activity);
  }

  @Test
  public void findMyActivities() {
    JSONObject req = new JSONObject();
    req.put("username", "testUsername");
    when(ctx.body()).thenReturn(req.toString());
    testSubject.findMyActivities(ctx);
    verify(activityDao).getAllFromUser("testUsername");
  }
}
