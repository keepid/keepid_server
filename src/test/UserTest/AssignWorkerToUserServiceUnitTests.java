package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import User.Services.AssignWorkerToUserService;
import User.User;
import User.UserMessage;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class AssignWorkerToUserServiceUnitTests {
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);

  @After
  public void reset() {
    if (userDao != null) {
      userDao.clear();
    }
  }

  @Test
  public void success() {
    String targetOrg = "MyOrganization";
    User currentlyLoggedInWorker =
        EntityFactory.createUser()
            .withOrgName(targetOrg)
            .withUsername("user1")
            .withUserType(UserType.Worker)
            .buildAndPersist(userDao);
    User workerInOrg2 =
        EntityFactory.createUser()
            .withOrgName(targetOrg)
            .withUsername("user2")
            .withUserType(UserType.Worker)
            .buildAndPersist(userDao);
    User workerInOrg3 =
        EntityFactory.createUser()
            .withOrgName(targetOrg)
            .withUsername("user3")
            .withUserType(UserType.Worker)
            .buildAndPersist(userDao);
    User workerNotInOrg4 =
        EntityFactory.createUser()
            .withOrgName("other org")
            .withUsername("user4")
            .withUserType(UserType.Worker)
            .buildAndPersist(userDao);
    User targetClient5 =
        EntityFactory.createUser()
            .withOrgName(targetOrg)
            .withUsername("user5")
            .withUserType(UserType.Client)
            .buildAndPersist(userDao);
    AssignWorkerToUserService assignWorkerToUserService =
        new AssignWorkerToUserService(
            userDao,
            currentlyLoggedInWorker.getUsername(),
            targetClient5.getUsername(),
            List.of(
                workerInOrg2.getUsername(),
                workerInOrg3.getUsername(),
                workerNotInOrg4.getUsername(),
                "other username here"));
    Message result = assignWorkerToUserService.executeAndGetResponse();
    assertEquals(result, UserMessage.SUCCESS);
    User readFromDBTargetUser = userDao.get(targetClient5.getUsername()).orElseThrow();
    assertEquals(
        List.of(workerInOrg2.getUsername(), workerInOrg3.getUsername()),
        readFromDBTargetUser.getAssignedWorkerUsernames());
  }

  @Test
  public void notLoggedIn() {
    AssignWorkerToUserService getMembersService =
        new AssignWorkerToUserService(userDao, null, null, emptyList());
    Message result = getMembersService.executeAndGetResponse();
    assertEquals(result, UserMessage.SESSION_TOKEN_FAILURE);
  }
}
