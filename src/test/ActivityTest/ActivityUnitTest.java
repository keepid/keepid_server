package ActivityTest;

import Activity.Activity;
import Activity.ActivityController;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoTestImpl;
import User.User;
import io.javalin.http.Context;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class ActivityUnitTest {

  private final ActivityDao activityDao = new ActivityDaoTestImpl();
  private final Context ctx = mock(Context.class);
  private ActivityController testSubject;

  @Before
  public void setup() {
    testSubject = new ActivityController(activityDao);
  }

  @Test
  public void addActivity() {
    Activity activity =
        Activity.builder().id(new ObjectId()).type(Collections.singletonList("TYPE")).build();
    testSubject.addActivity(activity);
  }

  @Test
  public void findMyActivities() {
    Activity activity =
        Activity.builder()
            .id(new ObjectId())
            .type(Collections.singletonList("TYPE"))
            .owner(User.builder().username("testUsername").build())
            .build();
    activityDao.save(activity);

    JSONObject req = new JSONObject();
    req.put("username", "testUsername");
    when(ctx.body()).thenReturn(req.toString());
    testSubject.findMyActivities(ctx);
    verify(ctx).result(any(String.class));
  }
}
