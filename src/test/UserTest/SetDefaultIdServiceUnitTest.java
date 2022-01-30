package UserTest;

import Config.Message;
import Database.User.UserDao;
import TestUtils.EntityFactory;
import User.Services.SetUserDefaultIdService;
import User.User;
import User.UserMessage;
import User.UserType;
import Validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class SetDefaultIdServiceUnitTest {

    private UserDao userDao;

    @Test
    public void setDefaultIds() {
        userDao = mock(UserDao.class);

        User user = EntityFactory.createUser()
                        .withFirstName("Jason")
                        .withLastName("Zhang")
                        .buildAndPersist(userDao);

        SetUserDefaultIdService setUserDefaultIdService = new SetUserDefaultIdService(userDao, user.getUsername(), "SSN", "123456789");
        Message response = setUserDefaultIdService.executeAndGetResponse();

        assertEquals(user.getDefaultIds().get("SSN"), "123456789");
        // assertEquals(response, UserMessage.SUCCESS);
    }
}
