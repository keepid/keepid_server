package DatabaseTest.Activity;

import static org.junit.Assert.*;

import Activity.*;
import Activity.CreateUserActivity.CreateAdminActivity;
import Activity.UserActivity.AuthenticationActivity.AuthenticationActivity;
import Activity.UserActivity.UserInformationActivity.ChangeUserAttributesActivity;
import Activity.UserActivity.FileActivity.DeleteFileActivity;
import Config.DeploymentLevel;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import File.FileType;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import java.time.LocalDateTime;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.*;

public class ActivityDaoImplUnitTests {
  private ActivityDao activityDao;
  private LocalDateTime now = LocalDateTime.of(2022, 9, 4, 1, 1, 1);

  @BeforeClass
  public static void startServer() {
    TestUtils.startServer();
  }

  @Before
  public void initialize() {
    this.activityDao = ActivityDaoFactory.create(DeploymentLevel.TEST);
  }

  @After
  public void reset() {
    activityDao.clear();
  }

  @AfterClass
  public static void teardown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void save() {
    Activity activity =
        EntityFactory.createActivity()
            .withUsername("my username")
            .withOccurredAt(now)
            .withType(List.of("Activity", "CreateUserActivity"))
            .buildAndPersist(activityDao);
    Activity readActivity = activityDao.get(activity.getId()).orElseThrow();
    assertTrue(areActivitiesEqual(activity, readActivity));

    AuthenticationActivity authenticationActivity = new AuthenticationActivity("username");
    authenticationActivity.setOccurredAt(
        now); // you have to set occurredAt else it will pull current
    activityDao.save(authenticationActivity);
    Activity readAuthenticateActivity =
        activityDao.get(authenticationActivity.getId()).orElseThrow();
    assertTrue(areActivitiesEqual(authenticationActivity, readAuthenticateActivity));

    ChangeUserAttributesActivity changeUserAttributesActivity =
        new ChangeUserAttributesActivity("username", "target", "attr 1", "newAttr 1");
    changeUserAttributesActivity.setOccurredAt(now);
    activityDao.save(changeUserAttributesActivity);
    Activity readChangeUserAttributesActivity =
        activityDao.get(changeUserAttributesActivity.getId()).orElseThrow();
    assertTrue(areActivitiesEqual(changeUserAttributesActivity, readChangeUserAttributesActivity));

    CreateAdminActivity createAdminActivity = new CreateAdminActivity("username", "target");
    createAdminActivity.setOccurredAt(now);
    activityDao.save(createAdminActivity);
    Activity readCreateAdminActivity = activityDao.get(createAdminActivity.getId()).orElseThrow();
    assertTrue(areActivitiesEqual(createAdminActivity, readCreateAdminActivity));

    DeleteFileActivity deleteFileActivity =
        new DeleteFileActivity(
            "usernameOfInvoker", "documentOwner", FileType.FORM, new ObjectId(), "fileName");
    deleteFileActivity.setOccurredAt(now);
    activityDao.save(deleteFileActivity);
    Activity readDeleteFileActivity = activityDao.get(deleteFileActivity.getId()).orElseThrow();
    assertTrue(areActivitiesEqual(deleteFileActivity, readDeleteFileActivity));
  }

  @Test
  public void getAll() {
    Activity activity =
        EntityFactory.createActivity()
            .withUsername("my username")
            .withOccurredAt(now)
            .withType(List.of("Activity", "CreateUserActivity"))
            .buildAndPersist(activityDao);
    Activity readActivity = activityDao.get(activity.getId()).orElseThrow();
    assertTrue(areActivitiesEqual(activity, readActivity));

    AuthenticationActivity authenticationActivity = new AuthenticationActivity("username");
    authenticationActivity.setOccurredAt(
        now); // you have to set occurredAt else it will pull current
    activityDao.save(authenticationActivity);

    ChangeUserAttributesActivity changeUserAttributesActivity =
        new ChangeUserAttributesActivity("username", "target", "attr 1", "newAttr 1");
    changeUserAttributesActivity.setOccurredAt(now);
    activityDao.save(changeUserAttributesActivity);

    CreateAdminActivity createAdminActivity = new CreateAdminActivity("username", "target");
    createAdminActivity.setOccurredAt(now);
    activityDao.save(createAdminActivity);

    DeleteFileActivity deleteFileActivity =
        new DeleteFileActivity(
            "usernameOfInvoker", "documentOwner", FileType.FORM, new ObjectId(), "fileName");
    deleteFileActivity.setOccurredAt(now);
    activityDao.save(deleteFileActivity);

    List<Activity> readAllActivities = activityDao.getAll();
    assertEquals(5, readAllActivities.size());
  }

  @Test
  public void size() {
    Activity activity =
        EntityFactory.createActivity()
            .withUsername("my username")
            .withOccurredAt(now)
            .withType(List.of("Activity", "CreateUserActivity"))
            .buildAndPersist(activityDao);
    Activity readActivity = activityDao.get(activity.getId()).orElseThrow();
    assertTrue(areActivitiesEqual(activity, readActivity));

    AuthenticationActivity authenticationActivity = new AuthenticationActivity("username");
    authenticationActivity.setOccurredAt(
        now); // you have to set occurredAt else it will pull current
    activityDao.save(authenticationActivity);

    ChangeUserAttributesActivity changeUserAttributesActivity =
        new ChangeUserAttributesActivity("username", "target", "attr 1", "newAttr 1");
    changeUserAttributesActivity.setOccurredAt(now);
    activityDao.save(changeUserAttributesActivity);

    CreateAdminActivity createAdminActivity = new CreateAdminActivity("username", "target");
    createAdminActivity.setOccurredAt(now);
    activityDao.save(createAdminActivity);

    DeleteFileActivity deleteFileActivity =
        new DeleteFileActivity(
            "usernameOfInvoker", "documentOwner", FileType.FORM, new ObjectId(), "fileName");
    deleteFileActivity.setOccurredAt(now);
    activityDao.save(deleteFileActivity);

    assertEquals(5, activityDao.size());
  }

  @Test
  public void update() {
    Activity activity =
        EntityFactory.createActivity()
            .withUsername("my username")
            .withOccurredAt(now)
            .withType(List.of("Activity", "CreateUserActivity"))
            .buildAndPersist(activityDao);
    Activity readActivity = activityDao.get(activity.getId()).orElseThrow();
    assertTrue(areActivitiesEqual(activity, readActivity));

    activity.setUsername("new username");
    activityDao.update(activity);
    Activity readActivity2 = activityDao.get(activity.getId()).orElseThrow();
    assertEquals("new username", readActivity2.getUsername());
  }

  @Test
  public void delete() {
    Activity activity =
        EntityFactory.createActivity()
            .withUsername("my username")
            .withOccurredAt(now)
            .withType(List.of("Activity", "CreateUserActivity"))
            .buildAndPersist(activityDao);
    Activity readActivity = activityDao.get(activity.getId()).orElseThrow();
    assertTrue(areActivitiesEqual(activity, readActivity));

    activityDao.delete(activity);
    assertFalse(activityDao.get(activity.getId()).isPresent());
    assertEquals(0, activityDao.size());
  }

  @Test
  public void clear() {
    Activity activity =
        EntityFactory.createActivity()
            .withUsername("my username")
            .withOccurredAt(now)
            .withType(List.of("Activity", "CreateUserActivity"))
            .buildAndPersist(activityDao);
    Activity readActivity = activityDao.get(activity.getId()).orElseThrow();
    assertTrue(areActivitiesEqual(activity, readActivity));

    activityDao.clear();
    assertFalse(activityDao.get(activity.getId()).isPresent());
    assertEquals(0, activityDao.size());
  }

  @Ignore
  @Test
  public void getAllFromUser() {
    String username1 = "username1";
    String username2 = "username2";
    Activity activityUsername1 =
        EntityFactory.createActivity()
            .withUsername(username1)
            .withOccurredAt(now.minusDays(1))
            .withType(List.of("Activity", "CreateUserActivity"))
            .buildAndPersist(activityDao);

    AuthenticationActivity authenticationActivityUsername1 = new AuthenticationActivity(username1);
    authenticationActivityUsername1.setOccurredAt(
        now.minusDays(2)); // you have to set occurredAt else it will pull current
    activityDao.save(authenticationActivityUsername1);

    ChangeUserAttributesActivity changeUserAttributesActivityUsername1 =
        new ChangeUserAttributesActivity(username1, "target", "attr 1", "newAttr 1");
    changeUserAttributesActivityUsername1.setOccurredAt(now.minusDays(3));
    activityDao.save(changeUserAttributesActivityUsername1);

    CreateAdminActivity createAdminActivityUsername2 = new CreateAdminActivity(username2, "target");
    createAdminActivityUsername2.setOccurredAt(now);
    activityDao.save(createAdminActivityUsername2);

    DeleteFileActivity deleteFileActivityUsername3 =
        new DeleteFileActivity(
            "usernameOfInvoker", "documentOwner", FileType.FORM, new ObjectId(), "fileName");
    deleteFileActivityUsername3.setOccurredAt(now);
    activityDao.save(deleteFileActivityUsername3);

    assertTrue(
        areActivitiesEqual(
            List.of(
                activityUsername1,
                authenticationActivityUsername1,
                changeUserAttributesActivityUsername1),
            activityDao.getAllFromUser(username1)));
  }

  static boolean areActivitiesEqual(Activity activity1, Activity activity2) {
    return activity1.compareTo(activity2) == 0;
  }

  static boolean areActivitiesEqual(List<Activity> activities1, List<Activity> activities2) {
    if (activities1.size() != activities2.size()) {
      return false;
    }
    boolean isEqual = true;
    for (int i = 0; i < activities1.size() - 1; i++) {
      if (activities1.get(i).compareTo(activities2.get(i)) != 0) {
        isEqual = false;
      }
    }
    return isEqual;
  }
}
