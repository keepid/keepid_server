package NotificationTest;

import static org.junit.Assert.*;

import Activity.Activity;
import Config.DeploymentLevel;
import Config.Message;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import Notification.Services.NotifyIdPickupService;
import Notification.WindmillNotificationClient;
import User.UserMessage;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NotifyIdPickupServiceTest {
    private ActivityDao activityDao;
    private WindmillNotificationClient notificationClient;

    @Before
    public void setUp() {
        activityDao = ActivityDaoFactory.create(DeploymentLevel.IN_MEMORY);
        notificationClient =
                new WindmillNotificationClient(
                        "http://localhost:9999", "fake-token", "+10000000000", "fake-sid", "fake-auth");
    }

    @After
    public void tearDown() {
        activityDao.clear();
    }

    @Test
    public void successfulNotificationSavesActivity() {
        NotifyIdPickupService service =
                new NotifyIdPickupService(
                        activityDao,
                        notificationClient,
                        "worker1",
                        "client1",
                        "drivers-license",
                        "+12125551234",
                        "Your ID is ready for pickup!");
        Message result = service.executeAndGetResponse();

        assertEquals(UserMessage.SUCCESS, result);
        assertEquals(1, activityDao.size());

        List<Activity> activities = activityDao.getAllFromUser("worker1");
        assertEquals(1, activities.size());
        Activity activity = activities.get(0);
        assertEquals("worker1", activity.getInvokerUsername());
        assertEquals("client1", activity.getTargetUsername());
        assertEquals("drivers-license", activity.getObjectName());
        assertEquals(
                "NotifyIdPickupActivity", activity.getType().get(activity.getType().size() - 1));
    }

    @Test
    public void invalidPhoneNumberReturnsError() {
        NotifyIdPickupService service =
                new NotifyIdPickupService(
                        activityDao,
                        notificationClient,
                        "worker1",
                        "client1",
                        "drivers-license",
                        "not-a-phone",
                        "Your ID is ready!");
        Message result = service.executeAndGetResponse();

        assertNotEquals(UserMessage.SUCCESS, result);
        assertEquals(0, activityDao.size());
    }

    @Test
    public void blankMessageReturnsError() {
        NotifyIdPickupService service =
                new NotifyIdPickupService(
                        activityDao,
                        notificationClient,
                        "worker1",
                        "client1",
                        "drivers-license",
                        "+12125551234",
                        "");
        Message result = service.executeAndGetResponse();

        assertNotEquals(UserMessage.SUCCESS, result);
        assertEquals(0, activityDao.size());
    }

    @Test
    public void nullWorkerUsernameReturnsError() {
        NotifyIdPickupService service =
                new NotifyIdPickupService(
                        activityDao,
                        notificationClient,
                        null,
                        "client1",
                        "drivers-license",
                        "+12125551234",
                        "Your ID is ready!");
        Message result = service.executeAndGetResponse();

        assertNotEquals(UserMessage.SUCCESS, result);
        assertEquals(0, activityDao.size());
    }

    @Test
    public void activityRecordedWithCorrectTypeChain() {
        NotifyIdPickupService service =
                new NotifyIdPickupService(
                        activityDao,
                        notificationClient,
                        "worker1",
                        "client1",
                        "state-id",
                        "+12125551234",
                        "Your state ID is ready");
        service.executeAndGetResponse();

        Activity activity = activityDao.getAll().get(0);
        List<String> type = activity.getType();
        assertEquals(3, type.size());
        assertEquals("Activity", type.get(0));
        assertEquals("UserActivity", type.get(1));
        assertEquals("NotifyIdPickupActivity", type.get(2));
    }
}
