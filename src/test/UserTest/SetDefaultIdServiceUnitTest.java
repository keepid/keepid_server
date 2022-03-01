package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoTestImpl;
import TestUtils.EntityFactory;
import User.Services.SetUserDefaultIdService;
import User.User;
import User.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class SetDefaultIdServiceUnitTest {

    private UserDao userDao;

    @Test
    public void setDefaultIds() {
        UserDao userDao = new UserDaoTestImpl(DeploymentLevel.IN_MEMORY);

        User user = EntityFactory.createUser()
                        .withFirstName("Jason")
                        .withLastName("Zhang")
                        .withUsername("jzhang0107")
                        .buildAndPersist(userDao);

        String documentType = "SSN";
        SetUserDefaultIdService setUserDefaultIdService = new SetUserDefaultIdService(userDao, user.getUsername(), documentType, "123456789");

        Message response = setUserDefaultIdService.executeAndGetResponse();
        String retrievedId = setUserDefaultIdService.getDocumentTypeId(documentType);
        User userRetrieved = setUserDefaultIdService.getUser();

        assertEquals(UserMessage.SUCCESS, response);
        assertEquals( "123456789", retrievedId);
        assertEquals(user, userRetrieved);
    }
}
