package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import User.Address;
import User.Name;
import User.Services.DeleteUserProfileFieldService;
import User.User;
import User.UserMessage;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class DeleteUserProfileFieldServiceTest {
    UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);

    @After
    public void reset() {
        if (userDao != null) {
            userDao.clear();
        }
    }

    @Test
    public void deleteMailAddress() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();
        user.setMailAddress(new Address("PO Box 123", null, "NYC", "NY", "10001", null));
        userDao.save(user);

        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(
                userDao, "testuser", "mailAddress");
        Message response = service.executeAndGetResponse();

        assertEquals(UserMessage.SUCCESS, response);

        User updatedUser = userDao.get("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertNull(updatedUser.getMailAddress());
    }

    @Test
    public void deleteSex() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();
        user.setSex("Male");
        userDao.save(user);

        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(
                userDao, "testuser", "sex");
        Message response = service.executeAndGetResponse();

        assertEquals(UserMessage.SUCCESS, response);

        User updatedUser = userDao.get("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertNull(updatedUser.getSex());
    }

    @Test
    public void deleteMotherName() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();
        user.setMotherName(new Name("Mary", null, "Smith", null, "Jones"));
        userDao.save(user);

        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(
                userDao, "testuser", "motherName");
        Message response = service.executeAndGetResponse();

        assertEquals(UserMessage.SUCCESS, response);

        User updatedUser = userDao.get("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertNull(updatedUser.getMotherName());
    }

    @Test
    public void cannotDeleteRequiredFields() {
        EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .withEmail("test@example.com")
                .buildAndPersist(userDao);

        DeleteUserProfileFieldService service1 = new DeleteUserProfileFieldService(userDao, "testuser", "username");
        assertEquals(UserMessage.INVALID_PARAMETER, service1.executeAndGetResponse());

        DeleteUserProfileFieldService service2 = new DeleteUserProfileFieldService(userDao, "testuser", "email");
        assertEquals(UserMessage.INVALID_PARAMETER, service2.executeAndGetResponse());

        DeleteUserProfileFieldService service3 = new DeleteUserProfileFieldService(userDao, "testuser", "currentName");
        assertEquals(UserMessage.INVALID_PARAMETER, service3.executeAndGetResponse());
    }

    @Test
    public void userNotFoundReturnsError() {
        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(
                userDao, "nonexistent", "sex");
        assertEquals(UserMessage.USER_NOT_FOUND, service.executeAndGetResponse());
    }

    @Test
    public void deleteNestedNameField() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();
        user.setMotherName(new Name("Mary", "Ann", "Smith", null, "Jones"));
        userDao.save(user);

        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(
                userDao, "testuser", "motherName.maiden");
        Message response = service.executeAndGetResponse();

        assertEquals(UserMessage.SUCCESS, response);

        User updatedUser = userDao.get("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getMotherName());
        assertEquals("Mary", updatedUser.getMotherName().getFirst());
        assertNull(updatedUser.getMotherName().getMaiden());
    }
}
