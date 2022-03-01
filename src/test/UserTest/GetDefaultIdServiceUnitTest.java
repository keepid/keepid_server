package UserTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoTestImpl;
import TestUtils.EntityFactory;
import User.Services.GetUserDefaultIdService;
import User.User;
import org.junit.Test;
import Config.Message;
import User.UserMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetDefaultIdServiceUnitTest {
    private UserDao userDao;

    @Test
    public void getDefaultIds() {
        UserDao userDao = new UserDaoTestImpl(DeploymentLevel.IN_MEMORY);

        User user = EntityFactory.createUser()
                .withFirstName("Jason")
                .withLastName("Zhang")
                .withUsername("jzhang0107")
                .buildAndPersist(userDao);

        String documentType = "SSN";
        user.setDefaultId(documentType, "123456789");

        GetUserDefaultIdService getUserDefaultIdService = new GetUserDefaultIdService(userDao, user.getUsername(), documentType);
        Message response = getUserDefaultIdService.executeAndGetResponse();
        String retrievedId = getUserDefaultIdService.getId();
        User retrievedUser = getUserDefaultIdService.getUser();

        assertEquals(UserMessage.SUCCESS, response);
        assertEquals("123456789", retrievedId);
        assertEquals(user, retrievedUser);
    }
}