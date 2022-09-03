package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import User.Services.GetUserInfoService;
import User.User;
import User.UserMessage;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class GetUserInfoServiceUnitTests {
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);

  @After
  public void reset() {
    userDao.clear();
  }

  @Test
  public void success() {
    User worker1 = EntityFactory.createUser()
        .withUsername("user1")
        .withUserType(UserType.Worker)
        .buildAndPersist(userDao);
    User worker2 = EntityFactory.createUser()
        .withUsername("user2")
        .withUserType(UserType.Worker)
        .buildAndPersist(userDao);

    GetUserInfoService getUserInfoService = new GetUserInfoService(userDao, "user1");
    Message result = getUserInfoService.executeAndGetResponse();
    assertEquals(result, UserMessage.SUCCESS);
    assertTrue(worker1.serialize().similar(getUserInfoService.getUserFields()));

    getUserInfoService = new GetUserInfoService(userDao, "user2");
    result = getUserInfoService.executeAndGetResponse();
    assertEquals(result, UserMessage.SUCCESS);
    assertTrue(worker2.serialize().similar(getUserInfoService.getUserFields()));
  }

  @Test
  public void noUser() {
    GetUserInfoService getUserInfoService = new GetUserInfoService(userDao, "no username");
    Message result = getUserInfoService.executeAndGetResponse();
    assertEquals(result, UserMessage.USER_NOT_FOUND);
    assertThrows(IllegalStateException.class, getUserInfoService::getUserFields, "user must be defined");
  }

  @Test
  public void sessionTokenCheck() {
    GetUserInfoService getUserInfoService = new GetUserInfoService(userDao, null);
    Message result = getUserInfoService.executeAndGetResponse();
    assertEquals(result, UserMessage.SESSION_TOKEN_FAILURE);
    assertThrows(IllegalStateException.class, getUserInfoService::getUserFields, "user must be defined");
  }
}
