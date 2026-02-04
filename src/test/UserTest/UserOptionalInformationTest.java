package UserTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import OptionalUserInformation.*;
import TestUtils.EntityFactory;
import User.OptionalInformation;
import User.User;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.json.JSONObject;

import java.util.Date;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
public class UserOptionalInformationTest {
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);

  @After
  public void reset() {
    if (userDao != null) {
      userDao.clear();
    }
  }

  @Test
  public void userCanHaveOptionalInformation() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    // Initially, optionalInformation should be null
    assertNull(user.getOptionalInformation());

    // Set optionalInformation
    OptionalInformation optionalInfo = new OptionalInformation();
    Person person = new Person();
    person.setFirstName("John");
    person.setLastName("Doe");
    person.setMiddleName("Middle");
    optionalInfo.setPerson(person);

    user.setOptionalInformation(optionalInfo);
    userDao.update(user);

    // Retrieve and verify
    User retrievedUser = userDao.get("testuser").orElse(null);
    assertNotNull(retrievedUser);
    assertNotNull(retrievedUser.getOptionalInformation());
    assertNotNull(retrievedUser.getOptionalInformation().getPerson());
    assertEquals("John", retrievedUser.getOptionalInformation().getPerson().getFirstName());
    assertEquals("Doe", retrievedUser.getOptionalInformation().getPerson().getLastName());
  }

  @Test
  public void serializeIncludesOptionalInformation() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .build();

    OptionalInformation optionalInfo = new OptionalInformation();
    
    // Set Person
    Person person = new Person();
    person.setFirstName("Jane");
    person.setMiddleName("M");
    person.setLastName("Smith");
    person.setSsn("123-45-6789");
    person.setBirthDate(new Date());
    optionalInfo.setPerson(person);

    // Set BasicInfo
    BasicInfo basicInfo = new BasicInfo();
    basicInfo.setGenderAssignedAtBirth("F");
    basicInfo.setEmailAddress("jane@example.com");
    basicInfo.setPhoneNumber("555-1234");
    optionalInfo.setBasicInfo(basicInfo);

    user.setOptionalInformation(optionalInfo);

    JSONObject serialized = user.serialize();
    
    assertTrue(serialized.has("optionalInformation"));
    JSONObject optionalInfoJSON = serialized.getJSONObject("optionalInformation");
    assertTrue(optionalInfoJSON.has("person"));
    assertTrue(optionalInfoJSON.has("basicInfo"));
    
    JSONObject personJSON = optionalInfoJSON.getJSONObject("person");
    assertEquals("Jane", personJSON.getString("firstName"));
    assertEquals("Smith", personJSON.getString("lastName"));
  }

  @Test
  public void serializeWorksWhenOptionalInformationIsNull() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .build();

    // optionalInformation is null by default
    assertNull(user.getOptionalInformation());

    JSONObject serialized = user.serialize();
    
    // Should still serialize successfully
    assertNotNull(serialized);
    assertEquals("testuser", serialized.getString("username"));
    
    // optionalInformation should not be in JSON if null
    assertFalse(serialized.has("optionalInformation"));
  }

  @Test
  public void serializeIncludesAllNestedObjects() {
    User user = EntityFactory.createUser()
        .withUsername("testuser")
        .withUserType(UserType.Client)
        .build();

    OptionalInformation optionalInfo = new OptionalInformation();
    
    // Set all nested objects
    optionalInfo.setPerson(new Person());
    optionalInfo.setBasicInfo(new BasicInfo());
    optionalInfo.setDemographicInfo(new DemographicInfo());
    optionalInfo.setFamilyInfo(new FamilyInfo());
    optionalInfo.setVeteranStatus(new VeteranStatus());

    user.setOptionalInformation(optionalInfo);

    JSONObject serialized = user.serialize();
    JSONObject optionalInfoJSON = serialized.getJSONObject("optionalInformation");
    
    assertTrue(optionalInfoJSON.has("person"));
    assertTrue(optionalInfoJSON.has("basicInfo"));
    assertTrue(optionalInfoJSON.has("demographicInfo"));
    assertTrue(optionalInfoJSON.has("familyInfo"));
    assertTrue(optionalInfoJSON.has("veteranStatus"));
  }
}
