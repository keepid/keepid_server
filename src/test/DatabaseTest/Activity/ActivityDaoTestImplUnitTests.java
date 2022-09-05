package DatabaseTest.Activity;

import Activity.*;
import Config.DeploymentLevel;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import File.FileType;
import TestUtils.EntityFactory;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

import static DatabaseTest.Activity.ActivityDaoImplUnitTests.areActivitiesEqual;
import static org.junit.Assert.*;

public class ActivityDaoTestImplUnitTests {
  private ActivityDao activityDao;
  private LocalDateTime now = LocalDateTime.of(2022, 9, 4, 1, 1, 1);

  @Before
  public void initialize() {
    this.activityDao = ActivityDaoFactory.create(DeploymentLevel.IN_MEMORY);
  }

  @After
  public void reset() {
    activityDao.clear();
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

    AuthenticateActivity authenticateActivity = new AuthenticateActivity("username");
    authenticateActivity.setOccurredAt(now); // you have to set occurredAt else it will pull current
    activityDao.save(authenticateActivity);
    Activity readAuthenticateActivity = activityDao.get(authenticateActivity.getId()).orElseThrow();
    assertTrue(areActivitiesEqual(authenticateActivity, readAuthenticateActivity));

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
            "usernameOfInvoker", "documentOwner", FileType.FORM_PDF, new ObjectId());
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

    AuthenticateActivity authenticateActivity = new AuthenticateActivity("username");
    authenticateActivity.setId(new ObjectId());
    authenticateActivity.setOccurredAt(now); // you have to set occurredAt else it will pull current
    activityDao.save(authenticateActivity);

    assertEquals(2, activityDao.getAll().size());
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

    AuthenticateActivity authenticateActivity = new AuthenticateActivity("username");
    authenticateActivity.setId(new ObjectId());
    authenticateActivity.setOccurredAt(now); // you have to set occurredAt else it will pull current
    activityDao.save(authenticateActivity);

    assertEquals(2, activityDao.size());
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

    AuthenticateActivity authenticateActivityUsername1 = new AuthenticateActivity(username1);
    authenticateActivityUsername1.setOccurredAt(
        now.minusDays(2)); // you have to set occurredAt else it will pull current
    authenticateActivityUsername1.setId(new ObjectId());
    activityDao.save(authenticateActivityUsername1);

    assertTrue(
        areActivitiesEqual(
            List.of(activityUsername1, authenticateActivityUsername1),
            activityDao.getAllFromUser(username1)));
  }
}
