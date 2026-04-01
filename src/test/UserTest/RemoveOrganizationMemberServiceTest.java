package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Database.Notification.NotificationDao;
import Database.Notification.NotificationDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import Notification.Notification;
import TestUtils.EntityFactory;
import User.Services.RemoveOrganizationMemberService;
import User.User;
import User.UserMessage;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

@Slf4j
public class RemoveOrganizationMemberServiceTest {
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);
  FileDao fileDao = FileDaoFactory.create(DeploymentLevel.IN_MEMORY);
  FormDao formDao = FormDaoFactory.create(DeploymentLevel.IN_MEMORY);
  ActivityDao activityDao = ActivityDaoFactory.create(DeploymentLevel.IN_MEMORY);
  NotificationDao notificationDao = NotificationDaoFactory.create(DeploymentLevel.IN_MEMORY);

  @After
  public void reset() {
    userDao.clear();
    fileDao.clear();
    formDao.clear();
    activityDao.clear();
    notificationDao.clear();
  }

  private RemoveOrganizationMemberService createService(
      String requestingUsername, String targetUsername,
      String orgName, UserType requestingUserType) {
    return new RemoveOrganizationMemberService(
        null, userDao, fileDao, formDao, activityDao, notificationDao,
        requestingUsername, targetUsername, orgName, requestingUserType);
  }

  // ========== Authorization tests ==========

  @Test
  public void adminCanRemoveWorkerFromSameOrg() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("worker1")
        .withUserType(UserType.Worker)
        .withOrgName("TestOrg")
        .withEmail("worker@keep.id")
        .buildAndPersist(userDao);

    Message response = createService("admin1", "worker1", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertTrue(userDao.get("worker1").isEmpty());
  }

  @Test
  public void directorCanRemoveWorkerFromSameOrg() {
    EntityFactory.createUser()
        .withUsername("director1")
        .withUserType(UserType.Director)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("worker1")
        .withUserType(UserType.Worker)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Message response = createService("director1", "worker1", "TestOrg", UserType.Director)
        .executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertTrue(userDao.get("worker1").isEmpty());
  }

  @Test
  public void adminCanRemoveClientFromSameOrg() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("client1")
        .withUserType(UserType.Client)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Message response = createService("admin1", "client1", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertTrue(userDao.get("client1").isEmpty());
  }

  @Test
  public void workerCannotRemoveMembers() {
    EntityFactory.createUser()
        .withUsername("worker1")
        .withUserType(UserType.Worker)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("worker2")
        .withUserType(UserType.Worker)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Message response = createService("worker1", "worker2", "TestOrg", UserType.Worker)
        .executeAndGetResponse();

    assertEquals(UserMessage.INSUFFICIENT_PRIVILEGE, response);
    assertTrue(userDao.get("worker2").isPresent());
  }

  @Test
  public void clientCannotRemoveMembers() {
    EntityFactory.createUser()
        .withUsername("client1")
        .withUserType(UserType.Client)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("worker1")
        .withUserType(UserType.Worker)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Message response = createService("client1", "worker1", "TestOrg", UserType.Client)
        .executeAndGetResponse();

    assertEquals(UserMessage.INSUFFICIENT_PRIVILEGE, response);
    assertTrue(userDao.get("worker1").isPresent());
  }

  @Test
  public void cannotRemoveSelf() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Message response = createService("admin1", "admin1", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
    assertTrue(userDao.get("admin1").isPresent());
  }

  @Test
  public void cannotRemoveNonexistentUser() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Message response = createService("admin1", "ghost_user", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.USER_NOT_FOUND, response);
  }

  @Test
  public void cannotRemoveUserFromDifferentOrg() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("OrgA")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("worker1")
        .withUserType(UserType.Worker)
        .withOrgName("OrgB")
        .buildAndPersist(userDao);

    Message response = createService("admin1", "worker1", "OrgA", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.CROSS_ORG_ACTION_DENIED, response);
    assertTrue(userDao.get("worker1").isPresent());
  }

  @Test
  public void emptyTargetUsernameReturnsInvalidParameter() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Message response = createService("admin1", "", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void blankTargetUsernameReturnsInvalidParameter() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Message response = createService("admin1", "   ", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void removedUserIsNoLongerInDao() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("workerA")
        .withUserType(UserType.Worker)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("workerB")
        .withUserType(UserType.Worker)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Message response = createService("admin1", "workerA", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertTrue(userDao.get("workerA").isEmpty());
    assertTrue(userDao.get("workerB").isPresent());
    assertTrue(userDao.get("admin1").isPresent());
  }

  @Test
  public void cannotRemoveAdmin() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("admin2")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Message response = createService("admin1", "admin2", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.INSUFFICIENT_PRIVILEGE, response);
    assertTrue(userDao.get("admin2").isPresent());
  }

  @Test
  public void cannotRemoveDirector() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("director1")
        .withUserType(UserType.Director)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Message response = createService("admin1", "director1", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.INSUFFICIENT_PRIVILEGE, response);
    assertTrue(userDao.get("director1").isPresent());
  }

  // ========== Orphan cleanup tests ==========

  @Test
  public void removingClientDeletesTheirFiles() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("client1")
        .withUserType(UserType.Client)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createFile()
        .withUsername("client1")
        .withFilename("id_card.pdf")
        .buildAndPersist(fileDao);
    EntityFactory.createFile()
        .withUsername("client1")
        .withFilename("birth_cert.pdf")
        .buildAndPersist(fileDao);

    assertEquals(2, fileDao.getAll("client1").size());

    Message response = createService("admin1", "client1", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertNull(fileDao.getAll("client1"));
  }

  @Test
  public void removingClientDeletesTheirForms() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("client1")
        .withUserType(UserType.Client)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createForm()
        .withUsername("client1")
        .buildAndPersist(formDao);
    EntityFactory.createForm()
        .withUsername("client1")
        .buildAndPersist(formDao);

    assertEquals(2, formDao.get("client1").size());

    Message response = createService("admin1", "client1", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertTrue(formDao.get("client1") == null || formDao.get("client1").isEmpty());
  }

  @Test
  public void removingClientDeletesTheirActivities() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("client1")
        .withUserType(UserType.Client)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createActivity()
        .withUsername("client1")
        .buildAndPersist(activityDao);
    EntityFactory.createActivity()
        .withUsername("client1")
        .buildAndPersist(activityDao);

    assertEquals(2, activityDao.getAllFromUser("client1").size());

    Message response = createService("admin1", "client1", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertTrue(activityDao.getAllFromUser("client1").isEmpty());
  }

  @Test
  public void removingClientDeletesTheirNotifications() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("client1")
        .withUserType(UserType.Client)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    Notification n1 = new Notification("worker1", "client1", "1234567890", "Pickup ready");
    Notification n2 = new Notification("worker2", "client1", "1234567890", "Second notice");
    notificationDao.save(n1);
    notificationDao.save(n2);

    assertEquals(2, notificationDao.getByClientUsername("client1").size());

    Message response = createService("admin1", "client1", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertTrue(notificationDao.getByClientUsername("client1").isEmpty());
  }

  @Test
  public void removingClientDeletesAllRelatedDataAtOnce() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("client1")
        .withUserType(UserType.Client)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createFile().withUsername("client1").withFilename("doc1.pdf").buildAndPersist(fileDao);
    EntityFactory.createForm().withUsername("client1").buildAndPersist(formDao);
    EntityFactory.createActivity().withUsername("client1").buildAndPersist(activityDao);
    notificationDao.save(new Notification("worker1", "client1", "1234567890", "Pickup ready"));

    Message response = createService("admin1", "client1", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertTrue(userDao.get("client1").isEmpty());
    assertNull(fileDao.getAll("client1"));
    assertTrue(formDao.get("client1") == null || formDao.get("client1").isEmpty());
    assertTrue(activityDao.getAllFromUser("client1").isEmpty());
    assertTrue(notificationDao.getByClientUsername("client1").isEmpty());
  }

  @Test
  public void removingClientDoesNotAffectOtherUsersData() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("client1")
        .withUserType(UserType.Client)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("client2")
        .withUserType(UserType.Client)
        .withOrgName("TestOrg")
        .withEmail("client2@keep.id")
        .buildAndPersist(userDao);

    EntityFactory.createFile().withUsername("client1").withFilename("c1_doc.pdf").buildAndPersist(fileDao);
    EntityFactory.createFile().withUsername("client2").withFilename("c2_doc.pdf").buildAndPersist(fileDao);

    EntityFactory.createForm().withUsername("client1").buildAndPersist(formDao);
    EntityFactory.createForm().withUsername("client2").buildAndPersist(formDao);

    EntityFactory.createActivity().withUsername("client1").buildAndPersist(activityDao);
    EntityFactory.createActivity().withUsername("client2").buildAndPersist(activityDao);

    notificationDao.save(new Notification("worker1", "client1", "1234567890", "Pickup"));
    notificationDao.save(new Notification("worker1", "client2", "0987654321", "Pickup"));

    Message response = createService("admin1", "client1", "TestOrg", UserType.Admin)
        .executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    assertTrue(userDao.get("client1").isEmpty());
    assertNull(fileDao.getAll("client1"));
    assertTrue(activityDao.getAllFromUser("client1").isEmpty());
    assertTrue(notificationDao.getByClientUsername("client1").isEmpty());

    assertTrue(userDao.get("client2").isPresent());
    assertEquals(1, fileDao.getAll("client2").size());
    assertEquals(1, formDao.get("client2").size());
    assertEquals(1, activityDao.getAllFromUser("client2").size());
    assertEquals(1, notificationDao.getByClientUsername("client2").size());
  }

  @Test
  public void failedRemovalDoesNotDeleteData() {
    EntityFactory.createUser()
        .withUsername("worker1")
        .withUserType(UserType.Worker)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createUser()
        .withUsername("client1")
        .withUserType(UserType.Client)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    EntityFactory.createFile().withUsername("client1").withFilename("doc.pdf").buildAndPersist(fileDao);
    EntityFactory.createForm().withUsername("client1").buildAndPersist(formDao);
    EntityFactory.createActivity().withUsername("client1").buildAndPersist(activityDao);
    notificationDao.save(new Notification("worker1", "client1", "1234567890", "Pickup"));

    Message response = createService("worker1", "client1", "TestOrg", UserType.Worker)
        .executeAndGetResponse();

    assertEquals(UserMessage.INSUFFICIENT_PRIVILEGE, response);

    assertTrue(userDao.get("client1").isPresent());
    assertEquals(1, fileDao.getAll("client1").size());
    assertEquals(1, formDao.get("client1").size());
    assertEquals(1, activityDao.getAllFromUser("client1").size());
    assertEquals(1, notificationDao.getByClientUsername("client1").size());
  }
}
