package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import User.Onboarding.OnboardingStatus;
import User.Services.GetOnboardingChecklistService;
import User.Services.PostOnboardingChecklistService;
import User.User;
import User.UserMessage;
import User.UserType;
import org.junit.After;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostOnboardingChecklistServiceUnitTest {
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);
  FormDao formDao = FormDaoFactory.create(DeploymentLevel.IN_MEMORY);
  FileDao fileDao = FileDaoFactory.create(DeploymentLevel.IN_MEMORY);

  @After
  public void reset() {
    if (userDao != null) {
      userDao.clear();
    }
    if (formDao != null) {
      formDao.clear();
    }
    if (fileDao != null) {
      fileDao.clear();
    }
  }

  @Test
  public void success() throws Exception {
    User client =
        EntityFactory.createUser()
            .withUsername("user1")
            .withEmail("testemail@email.com")
            .withUserType(UserType.Client)
            .buildAndPersist(userDao);

    PostOnboardingChecklistService postChecklistService = new PostOnboardingChecklistService(userDao,
        "user1", new OnboardingStatus());
    Message result = postChecklistService.executeAndGetResponse();
    assertEquals(UserMessage.AUTH_SUCCESS, result);
  }

  @Test
  public void noUser() throws Exception {
    PostOnboardingChecklistService postChecklistService = new PostOnboardingChecklistService(userDao,
        "user1", new OnboardingStatus());
    Message result = postChecklistService.executeAndGetResponse();
    assertEquals(UserMessage.USER_NOT_FOUND, result);
  }
}
