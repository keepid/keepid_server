package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
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

  @After
  public void reset() {
    if (userDao != null) {
      userDao.clear();
    }
  }

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "admin1", "worker1", "TestOrg", UserType.Admin);
    Message response = service.executeAndGetResponse();

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "director1", "worker1", "TestOrg", UserType.Director);
    Message response = service.executeAndGetResponse();

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "admin1", "client1", "TestOrg", UserType.Admin);
    Message response = service.executeAndGetResponse();

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "worker1", "worker2", "TestOrg", UserType.Worker);
    Message response = service.executeAndGetResponse();

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "client1", "worker1", "TestOrg", UserType.Client);
    Message response = service.executeAndGetResponse();

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "admin1", "admin1", "TestOrg", UserType.Admin);
    Message response = service.executeAndGetResponse();

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "admin1", "ghost_user", "TestOrg", UserType.Admin);
    Message response = service.executeAndGetResponse();

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "admin1", "worker1", "OrgA", UserType.Admin);
    Message response = service.executeAndGetResponse();

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "admin1", "", "TestOrg", UserType.Admin);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void blankTargetUsernameReturnsInvalidParameter() {
    EntityFactory.createUser()
        .withUsername("admin1")
        .withUserType(UserType.Admin)
        .withOrgName("TestOrg")
        .buildAndPersist(userDao);

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "admin1", "   ", "TestOrg", UserType.Admin);
    Message response = service.executeAndGetResponse();

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "admin1", "workerA", "TestOrg", UserType.Admin);
    Message response = service.executeAndGetResponse();

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "admin1", "admin2", "TestOrg", UserType.Admin);
    Message response = service.executeAndGetResponse();

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

    RemoveOrganizationMemberService service = new RemoveOrganizationMemberService(
        null, userDao, "admin1", "director1", "TestOrg", UserType.Admin);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INSUFFICIENT_PRIVILEGE, response);
    assertTrue(userDao.get("director1").isPresent());
  }
}
