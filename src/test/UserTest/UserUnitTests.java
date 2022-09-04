package UserTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import User.User;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertTrue;

@Slf4j
public class UserUnitTests {
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);

  @After
  public void reset() {
    userDao.clear();
  }

  @Test
  public void serialize_success() {
    Date creationDate = new Calendar.Builder().setDate(2022, 1, 1).build().getTime();
    User workerInOrg = EntityFactory.createUser()
        .withUsername("myUsername")
        .withEmail("myEmail@gmail.com")
        .withZipcode("19104")
        .withState("PA")
        .withAddress("some address")
        .withBirthDate("01-01-1997")
        .withPassword("some password")
        .withFirstName("testFirstName")
        .withLastName("testLastName")
        .withPhoneNumber("1112223333")
        .withUserType(UserType.Worker)
        .withOrgName("myOrg")
        .withCreationDate(creationDate)
        .buildAndPersist(userDao);
    JSONObject expected = new JSONObject()
        .put("username", "myUsername")
        .put("firstName", "testFirstName")
        .put("lastName", "testLastName")
        .put("birthDate", "01-01-1997")
        .put("email", "myEmail@gmail.com")
        .put("phone", "1112223333")
        .put("organization", "myOrg")
        .put("address", "some address")
        .put("city", "Philadelphia")
        .put("state", "PA")
        .put("zipcode", "19104")
        .put("userType", UserType.Worker)
        .put("privilegeLevel", UserType.Worker)
        .put("twoFactorOn", false)
        .put("creationDate", creationDate)
        .put("logInHistory", emptyList())
        .put("defaultIds", emptyMap())
        .put("assignedWorkerUsernames", emptyList());

    JSONObject serialized = workerInOrg.serialize();
    assertTrue(expected.similar(serialized));
  }

}
