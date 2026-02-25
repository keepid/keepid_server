package UserTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import User.Address;
import User.Name;
import User.Services.GetUserInfoService;
import User.User;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class GetUserInfoServiceFlattenedMapTest {
    UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);

    @After
    public void reset() {
        if (userDao != null) {
            userDao.clear();
        }
    }

    @Test
    public void getFlattenedFieldMapReturnsRootLevelFields() {
        EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .withEmail("test@example.com")
                .withPhoneNumber("1234567890")
                .buildAndPersist(userDao);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        assertNotNull(flattened);
        assertEquals("testuser", flattened.get("username"));
        assertEquals("test@example.com", flattened.get("email"));
    }

    @Test
    public void getFlattenedFieldMapIncludesCurrentName() {
        EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .withFirstName("John")
                .withLastName("Doe")
                .buildAndPersist(userDao);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        assertEquals("John", flattened.get("currentName.first"));
        assertEquals("Doe", flattened.get("currentName.last"));
        assertEquals("John", flattened.get("firstName"));
        assertEquals("Doe", flattened.get("lastName"));
    }

    @Test
    public void getFlattenedFieldMapIncludesPersonalAddress() {
        EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .withAddress("123 Main St")
                .withCity("Philadelphia")
                .withState("PA")
                .withZipcode("19104")
                .buildAndPersist(userDao);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        assertEquals("123 Main St", flattened.get("personalAddress.line1"));
        assertEquals("Philadelphia", flattened.get("personalAddress.city"));
        assertEquals("PA", flattened.get("personalAddress.state"));
        assertEquals("19104", flattened.get("personalAddress.zip"));
    }

    @Test
    public void getFlattenedFieldMapIncludesPhoneBook() {
        EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .withPhoneNumber("1234567890")
                .buildAndPersist(userDao);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        assertEquals("primary", flattened.get("phoneBook.0.label"));
        assertEquals("1234567890", flattened.get("phoneBook.0.phoneNumber"));
    }

    @Test
    public void getFlattenedFieldMapIncludesParentNames() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        user.setMotherName(new Name("Mary", null, "Smith", null, "Jones"));
        user.setFatherName(new Name("John", null, "Smith", null, null));
        user.setSex("Male");
        userDao.save(user);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        assertEquals("Mary", flattened.get("motherName.first"));
        assertEquals("Smith", flattened.get("motherName.last"));
        assertEquals("Jones", flattened.get("motherName.maiden"));
        assertEquals("John", flattened.get("fatherName.first"));
        assertEquals("Smith", flattened.get("fatherName.last"));
        assertEquals("Male", flattened.get("sex"));
    }

    @Test
    public void getFlattenedFieldMapIncludesMailAddress() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        user.setMailAddress(new Address("PO Box 123", null, "NYC", "NY", "10001", null));
        userDao.save(user);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        assertEquals("PO Box 123", flattened.get("mailAddress.line1"));
        assertEquals("NYC", flattened.get("mailAddress.city"));
        assertEquals("NY", flattened.get("mailAddress.state"));
        assertEquals("10001", flattened.get("mailAddress.zip"));
    }

    @Test
    public void getFlattenedFieldMapIncludesNameHistory() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        user.setNameHistory(Arrays.asList(
            new Name("OldFirst", null, "OldLast", null, "Maiden1")
        ));
        userDao.save(user);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        assertEquals("OldFirst", flattened.get("nameHistory.0.first"));
        assertEquals("OldLast", flattened.get("nameHistory.0.last"));
        assertEquals("Maiden1", flattened.get("nameHistory.0.maiden"));
    }
}
