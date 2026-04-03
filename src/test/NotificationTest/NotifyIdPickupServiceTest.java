package NotificationTest;

import static org.junit.Assert.*;

import Activity.Activity;
import Config.DeploymentLevel;
import Config.Message;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import Database.Notification.NotificationDao;
import Database.Notification.NotificationDaoFactory;
import Notification.Notification;
import Notification.Services.NotifyIdPickupService;
import Notification.WindmillNotificationClient;
import User.UserMessage;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NotifyIdPickupServiceTest {
    private ActivityDao activityDao;
    private NotificationDao notificationDao;
    private WindmillNotificationClient notificationClient;

    @Before
    public void setUp() {
        activityDao = ActivityDaoFactory.create(DeploymentLevel.IN_MEMORY);
        notificationDao = NotificationDaoFactory.create(DeploymentLevel.IN_MEMORY);
        notificationClient =
                new WindmillNotificationClient(
                        "http://localhost:9999", "fake-token",
                        "+10000000000", "fake-sid", "fake-auth",
                        "fake_email", "fake_host", "fake_port", "fake_password");
    }

    @After
    public void tearDown() {
        activityDao.clear();
        notificationDao.clear();
    }

    @Test
    public void successfulNotificationSavesActivity() {
        NotifyIdPickupService service =
                new NotifyIdPickupService(
                        activityDao,
                        notificationDao,
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
    public void successfulNotificationPersistsFullMessage() {
        NotifyIdPickupService service =
                new NotifyIdPickupService(
                        activityDao,
                        notificationDao,
                        notificationClient,
                        "worker1",
                        "clientA",
                        "birth-cert",
                        "+12125551234",
                        "Hi Jane, your Birth Certificate is ready.");
        Message result = service.executeAndGetResponse();

        assertEquals(UserMessage.SUCCESS, result);
        assertEquals(1, notificationDao.size());

        List<Notification> notifications = notificationDao.getByClientUsername("clientA");
        assertEquals(1, notifications.size());

        Notification n = notifications.get(0);
        assertEquals("worker1", n.getWorkerUsername());
        assertEquals("clientA", n.getClientUsername());
        assertEquals("+12125551234", n.getClientPhoneNumber());
        assertEquals("Hi Jane, your Birth Certificate is ready.", n.getMessage());
        assertNotNull(n.getSentAt());
        assertNotNull(n.getId());
    }

    @Test
    public void notificationsArePerClient() {
        new NotifyIdPickupService(
                activityDao, notificationDao, notificationClient,
                "worker1", "clientA", "id1", "+12125551234", "Message for A")
                .executeAndGetResponse();
        new NotifyIdPickupService(
                activityDao, notificationDao, notificationClient,
                "worker1", "clientB", "id2", "+12125559999", "Message for B")
                .executeAndGetResponse();
        new NotifyIdPickupService(
                activityDao, notificationDao, notificationClient,
                "worker1", "clientA", "id3", "+12125551234", "Second message for A")
                .executeAndGetResponse();

        assertEquals(3, notificationDao.size());

        List<Notification> forA = notificationDao.getByClientUsername("clientA");
        assertEquals(2, forA.size());
        assertTrue(forA.stream().allMatch(n -> n.getClientUsername().equals("clientA")));

        List<Notification> forB = notificationDao.getByClientUsername("clientB");
        assertEquals(1, forB.size());
        assertEquals("clientB", forB.get(0).getClientUsername());
        assertEquals("Message for B", forB.get(0).getMessage());
    }

    @Test
    public void samePhoneNumberDifferentClientsGetSeparateHistories() {
        String sharedPhone = "+12125550000";

        new NotifyIdPickupService(
                activityDao, notificationDao, notificationClient,
                "worker1", "clientX", "ssn-card", sharedPhone, "SSN card ready for X")
                .executeAndGetResponse();
        new NotifyIdPickupService(
                activityDao, notificationDao, notificationClient,
                "worker1", "clientY", "photo-id", sharedPhone, "Photo ID ready for Y")
                .executeAndGetResponse();

        List<Notification> forX = notificationDao.getByClientUsername("clientX");
        List<Notification> forY = notificationDao.getByClientUsername("clientY");

        assertEquals(1, forX.size());
        assertEquals("SSN card ready for X", forX.get(0).getMessage());

        assertEquals(1, forY.size());
        assertEquals("Photo ID ready for Y", forY.get(0).getMessage());
    }

    @Test
    public void multipleWorkersCanNotifySameClient() {
        new NotifyIdPickupService(
                activityDao, notificationDao, notificationClient,
                "worker1", "clientA", "id1", "+12125551234", "Worker 1 message")
                .executeAndGetResponse();
        new NotifyIdPickupService(
                activityDao, notificationDao, notificationClient,
                "worker2", "clientA", "id2", "+12125551234", "Worker 2 message")
                .executeAndGetResponse();
        new NotifyIdPickupService(
                activityDao, notificationDao, notificationClient,
                "admin1", "clientA", "id3", "+12125551234", "Admin message")
                .executeAndGetResponse();

        List<Notification> forA = notificationDao.getByClientUsername("clientA");
        assertEquals(3, forA.size());

        assertTrue(forA.stream().anyMatch(n -> n.getWorkerUsername().equals("worker1")));
        assertTrue(forA.stream().anyMatch(n -> n.getWorkerUsername().equals("worker2")));
        assertTrue(forA.stream().anyMatch(n -> n.getWorkerUsername().equals("admin1")));
    }

    @Test
    public void failedNotificationDoesNotPersist() {
        NotifyIdPickupService service =
                new NotifyIdPickupService(
                        activityDao, notificationDao, notificationClient,
                        "worker1", "clientA", "id1", "bad-phone", "A message");
        service.executeAndGetResponse();

        assertEquals(0, notificationDao.size());
        assertEquals(0, notificationDao.getByClientUsername("clientA").size());
    }

    @Test
    public void invalidPhoneNumberReturnsError() {
        NotifyIdPickupService service =
                new NotifyIdPickupService(
                        activityDao,
                        notificationDao,
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
                        notificationDao,
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
                        notificationDao,
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
                        notificationDao,
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

    @Test
    public void queryForNonexistentClientReturnsEmpty() {
        new NotifyIdPickupService(
                activityDao, notificationDao, notificationClient,
                "worker1", "clientA", "id1", "+12125551234", "A message")
                .executeAndGetResponse();

        List<Notification> forNobody = notificationDao.getByClientUsername("nonexistent-client");
        assertEquals(0, forNobody.size());
    }
}
