package UserTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import OptionalUserInformation.*;
import TestUtils.EntityFactory;
import User.OptionalInformation;
import User.Services.GetUserInfoService;
import User.User;
import User.UserMessage;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

import java.util.Date;
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
        assertEquals("1234567890", flattened.get("phone"));
    }

    @Test
    public void getFlattenedFieldMapIncludesOptionalInformationFields() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        OptionalInformation optionalInfo = new OptionalInformation();
        Person person = new Person();
        // firstName/lastName should be null for user's own Person (they come from root
        // level)
        person.setMiddleName("Middle");
        person.setSsn("123-45-6789");
        optionalInfo.setPerson(person);

        BasicInfo basicInfo = new BasicInfo();
        basicInfo.setGenderAssignedAtBirth("M");
        basicInfo.setEmailAddress("john@example.com");
        basicInfo.setPhoneNumber("1234567890");
        optionalInfo.setBasicInfo(basicInfo);

        user.setOptionalInformation(optionalInfo);
        userDao.save(user);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        // firstName/lastName should NOT be in flattened map from Person (they come from
        // root level)
        assertFalse(flattened.containsKey("optionalInformation.person.firstName"));
        assertFalse(flattened.containsKey("optionalInformation.person.lastName"));
        assertEquals("Middle", flattened.get("optionalInformation.person.middleName"));
        assertEquals("123-45-6789", flattened.get("optionalInformation.person.ssn"));
        assertEquals("M", flattened.get("optionalInformation.basicInfo.genderAssignedAtBirth"));
        assertEquals("john@example.com", flattened.get("optionalInformation.basicInfo.emailAddress"));
        assertEquals("1234567890", flattened.get("optionalInformation.basicInfo.phoneNumber"));
    }

    @Test
    public void getFlattenedFieldMapHandlesNestedAddresses() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        OptionalInformation optionalInfo = new OptionalInformation();
        BasicInfo basicInfo = new BasicInfo();

        Address mailingAddress = new Address();
        mailingAddress.setStreetAddress("123 Main St");
        mailingAddress.setApartmentNumber("Apt 4B");
        mailingAddress.setCity("Philadelphia");
        mailingAddress.setState("PA");
        mailingAddress.setZip("19104");
        basicInfo.setMailingAddress(mailingAddress);

        optionalInfo.setBasicInfo(basicInfo);
        user.setOptionalInformation(optionalInfo);
        userDao.save(user);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        assertEquals("123 Main St", flattened.get("optionalInformation.basicInfo.mailingAddress.streetAddress"));
        assertEquals("Apt 4B", flattened.get("optionalInformation.basicInfo.mailingAddress.apartmentNumber"));
        assertEquals("Philadelphia", flattened.get("optionalInformation.basicInfo.mailingAddress.city"));
        assertEquals("PA", flattened.get("optionalInformation.basicInfo.mailingAddress.state"));
        assertEquals("19104", flattened.get("optionalInformation.basicInfo.mailingAddress.zip"));
    }

    @Test
    public void getFlattenedFieldMapHandlesArrays() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        OptionalInformation optionalInfo = new OptionalInformation();
        FamilyInfo familyInfo = new FamilyInfo();

        Person parent1 = new Person();
        parent1.setFirstName("Parent1");
        parent1.setLastName("Last");

        Person parent2 = new Person();
        parent2.setFirstName("Parent2");
        parent2.setLastName("Last");

        familyInfo.setParents(java.util.Arrays.asList(parent1, parent2));
        optionalInfo.setFamilyInfo(familyInfo);
        user.setOptionalInformation(optionalInfo);
        userDao.save(user);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        assertEquals("Parent1", flattened.get("optionalInformation.familyInfo.parents.0.firstName"));
        assertEquals("Last", flattened.get("optionalInformation.familyInfo.parents.0.lastName"));
        assertEquals("Parent2", flattened.get("optionalInformation.familyInfo.parents.1.firstName"));
        assertEquals("Last", flattened.get("optionalInformation.familyInfo.parents.1.lastName"));
    }

    @Test
    public void getFlattenedFieldMapReturnsEmptyMapForNullFields() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .buildAndPersist(userDao);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        // Should not contain keys for null optionalInformation
        assertFalse(flattened.containsKey("optionalInformation.person.firstName"));
        assertFalse(flattened.containsKey("optionalInformation.basicInfo.genderAssignedAtBirth"));
    }

    @Test
    public void getFlattenedFieldMapHandlesAllNestedObjects() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        OptionalInformation optionalInfo = new OptionalInformation();

        // Set Person (firstName/lastName should be null - they come from root level)
        Person person = new Person();
        person.setMiddleName("M");
        optionalInfo.setPerson(person);

        // Set BasicInfo
        BasicInfo basicInfo = new BasicInfo();
        basicInfo.setGenderAssignedAtBirth("F");
        optionalInfo.setBasicInfo(basicInfo);

        // Set DemographicInfo - need to handle @NonNull fields
        DemographicInfo demographicInfo = new DemographicInfo();
        demographicInfo.setIsEthnicityHispanicLatino(false);
        demographicInfo.setRace(Race.UNSELECTED);
        demographicInfo.setCityOfBirth("Philadelphia");
        demographicInfo.setStateOfBirth("PA");
        demographicInfo.setCountryOfBirth("USA");
        demographicInfo.setCitizenship(Citizenship.UNSELECTED);
        optionalInfo.setDemographicInfo(demographicInfo);

        // Set FamilyInfo
        FamilyInfo familyInfo = new FamilyInfo();
        familyInfo.setMaritalStatus(MaritalStatus.SINGLE);
        optionalInfo.setFamilyInfo(familyInfo);

        // Set VeteranStatus - use builder pattern since primitives need values
        VeteranStatus veteranStatus = VeteranStatus.builder()
                .isVeteran(false)
                .isProtectedVeteran(false)
                .build();
        optionalInfo.setVeteranStatus(veteranStatus);

        user.setOptionalInformation(optionalInfo);
        userDao.save(user);

        GetUserInfoService service = new GetUserInfoService(userDao, "testuser");
        service.executeAndGetResponse();

        Map<String, String> flattened = service.getFlattenedFieldMap();

        // Verify all nested objects are included
        // firstName/lastName should NOT be in flattened map from Person
        assertFalse(flattened.containsKey("optionalInformation.person.firstName"));
        assertEquals("M", flattened.get("optionalInformation.person.middleName"));
        assertEquals("F", flattened.get("optionalInformation.basicInfo.genderAssignedAtBirth"));
        assertEquals("false", flattened.get("optionalInformation.demographicInfo.isEthnicityHispanicLatino"));
        assertEquals("SINGLE", flattened.get("optionalInformation.familyInfo.maritalStatus"));
        assertEquals("false", flattened.get("optionalInformation.veteranStatus.isVeteran"));
    }
}
