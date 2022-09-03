package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import User.Services.GetAllMembersByRoleService;
import User.User;
import User.UserMessage;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class GetAllMembersByRoleServiceUnitTests {
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);

  @After
  public void reset() {
    userDao.clear();
  }

  @Test
  public void success() {
    String targetOrg = "MyOrganization";
    User workerInOrg1 =
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
    User clientInOrg2 =
        EntityFactory.createUser()
            .withOrgName(targetOrg)
            .withUsername("user3")
            .withUserType(UserType.Client)
            .buildAndPersist(userDao);
    User userNotInOrg =
        EntityFactory.createUser()
            .withOrgName("otherOrganization")
            .withUsername("user4")
            .buildAndPersist(userDao);
    GetAllMembersByRoleService getMembersService =
        new GetAllMembersByRoleService(userDao, targetOrg, UserType.Worker);
    Message result = getMembersService.executeAndGetResponse();
    assertEquals(result, UserMessage.SUCCESS);
    assertEquals(
        List.of(workerInOrg1.serialize().toString(), workerInOrg2.serialize().toString()),
        getMembersService.getUsersWithSpecificRole().stream()
            .map(JSONObject::toString)
            .sorted()
            .collect(Collectors.toList()));
  }

  @Test
  public void noUsersInOrganization() {
    String targetOrg = "MyOrganization";
    User workerNotInOrganization =
        EntityFactory.createUser()
            .withOrgName("someOtherOrg")
            .withUsername("user1")
            .withUserType(UserType.Worker)
            .buildAndPersist(userDao);
    User workerNotInOrganization2 =
        EntityFactory.createUser()
            .withOrgName("someOtherOrg")
            .withUsername("user2")
            .withUserType(UserType.Worker)
            .buildAndPersist(userDao);
    User client =
        EntityFactory.createUser()
            .withOrgName(targetOrg)
            .withUsername("user3")
            .withUserType(UserType.Client)
            .buildAndPersist(userDao);

    GetAllMembersByRoleService getMembersService =
        new GetAllMembersByRoleService(userDao, targetOrg, UserType.Worker);
    Message result = getMembersService.executeAndGetResponse();
    assertEquals(result, UserMessage.SUCCESS);
    assertEquals(emptySet(), getMembersService.getUsersWithSpecificRole());
  }

  @Test
  public void notLoggedIn() {
    GetAllMembersByRoleService getMembersService =
        new GetAllMembersByRoleService(userDao, null, UserType.Worker);
    Message result = getMembersService.executeAndGetResponse();
    assertEquals(result, UserMessage.SESSION_TOKEN_FAILURE);
    assertEquals(emptySet(), getMembersService.getUsersWithSpecificRole());
  }
}
