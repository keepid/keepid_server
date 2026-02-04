package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import OptionalUserInformation.*;
import TestUtils.EntityFactory;
import User.OptionalInformation;
import User.Services.DeleteUserProfileFieldService;
import User.User;
import User.UserMessage;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

import java.util.Date;

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
    public void deleteNestedOptionalInformationField() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        OptionalInformation optionalInfo = new OptionalInformation();
        Person person = new Person();
        person.setMiddleName("Middle");
        person.setSsn("123-45-6789");
        optionalInfo.setPerson(person);

        BasicInfo basicInfo = new BasicInfo();
        basicInfo.setGenderAssignedAtBirth("M");
        optionalInfo.setBasicInfo(basicInfo);

        user.setOptionalInformation(optionalInfo);
        userDao.save(user);

        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(userDao, "testuser",
                "optionalInformation.person.middleName");
        Message response = service.executeAndGetResponse();

        assertEquals(UserMessage.SUCCESS, response);

        User updatedUser = userDao.get("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getOptionalInformation());
        assertNotNull(updatedUser.getOptionalInformation().getPerson());
        assertNull(updatedUser.getOptionalInformation().getPerson().getMiddleName());
        // SSN should still be there
        assertEquals("123-45-6789", updatedUser.getOptionalInformation().getPerson().getSsn());
    }

    @Test
    public void deleteNestedAddressField() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        OptionalInformation optionalInfo = new OptionalInformation();
        BasicInfo basicInfo = new BasicInfo();
        Address mailingAddress = new Address();
        mailingAddress.setStreetAddress("123 Main St");
        mailingAddress.setCity("Philadelphia");
        mailingAddress.setState("PA");
        mailingAddress.setZip("19104");
        basicInfo.setMailingAddress(mailingAddress);
        optionalInfo.setBasicInfo(basicInfo);
        user.setOptionalInformation(optionalInfo);
        userDao.save(user);

        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(userDao, "testuser",
                "optionalInformation.basicInfo.mailingAddress");
        Message response = service.executeAndGetResponse();

        assertEquals(UserMessage.SUCCESS, response);

        User updatedUser = userDao.get("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getOptionalInformation());
        assertNotNull(updatedUser.getOptionalInformation().getBasicInfo());
        assertNull(updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress());
    }

    @Test
    public void deleteNestedFieldWithDotNotation() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        OptionalInformation optionalInfo = new OptionalInformation();
        BasicInfo basicInfo = new BasicInfo();
        Address mailingAddress = new Address();
        mailingAddress.setStreetAddress("123 Main St");
        mailingAddress.setCity("Philadelphia");
        basicInfo.setMailingAddress(mailingAddress);
        optionalInfo.setBasicInfo(basicInfo);
        user.setOptionalInformation(optionalInfo);
        userDao.save(user);

        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(userDao, "testuser",
                "optionalInformation.basicInfo.mailingAddress.city");
        Message response = service.executeAndGetResponse();

        assertEquals(UserMessage.SUCCESS, response);

        User updatedUser = userDao.get("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getOptionalInformation());
        assertNotNull(updatedUser.getOptionalInformation().getBasicInfo());
        assertNotNull(updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress());
        assertNull(updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress().getCity());
        // Street address should still be there
        assertEquals("123 Main St",
                updatedUser.getOptionalInformation().getBasicInfo().getMailingAddress().getStreetAddress());
    }

    @Test
    public void cannotDeleteRequiredFields() {
        EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .withEmail("test@example.com")
                .buildAndPersist(userDao);

        // Try to delete required fields
        DeleteUserProfileFieldService service1 = new DeleteUserProfileFieldService(userDao, "testuser", "username");
        Message response1 = service1.executeAndGetResponse();
        assertEquals(UserMessage.INVALID_PARAMETER, response1);

        DeleteUserProfileFieldService service2 = new DeleteUserProfileFieldService(userDao, "testuser", "email");
        Message response2 = service2.executeAndGetResponse();
        assertEquals(UserMessage.INVALID_PARAMETER, response2);

        DeleteUserProfileFieldService service3 = new DeleteUserProfileFieldService(userDao, "testuser", "organization");
        Message response3 = service3.executeAndGetResponse();
        assertEquals(UserMessage.INVALID_PARAMETER, response3);
    }

    @Test
    public void userNotFoundReturnsError() {
        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(userDao, "nonexistent",
                "optionalInformation.person.middleName");
        Message response = service.executeAndGetResponse();

        assertEquals(UserMessage.USER_NOT_FOUND, response);
    }

    @Test
    public void deleteVeteranStatusField() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        OptionalInformation optionalInfo = new OptionalInformation();
        VeteranStatus veteranStatus = VeteranStatus.builder()
                .isVeteran(true)
                .isProtectedVeteran(false)
                .branch("Army")
                .build();
        optionalInfo.setVeteranStatus(veteranStatus);
        user.setOptionalInformation(optionalInfo);
        userDao.save(user);

        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(userDao, "testuser",
                "optionalInformation.veteranStatus.branch");
        Message response = service.executeAndGetResponse();

        assertEquals(UserMessage.SUCCESS, response);

        User updatedUser = userDao.get("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getOptionalInformation());
        assertNotNull(updatedUser.getOptionalInformation().getVeteranStatus());
        assertNull(updatedUser.getOptionalInformation().getVeteranStatus().getBranch());
        // isVeteran should still be there
        assertTrue(updatedUser.getOptionalInformation().getVeteranStatus().isVeteran());
    }

    @Test
    public void deleteEntireNestedObject() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        OptionalInformation optionalInfo = new OptionalInformation();
        Person person = new Person();
        person.setMiddleName("Middle");
        optionalInfo.setPerson(person);

        BasicInfo basicInfo = new BasicInfo();
        basicInfo.setGenderAssignedAtBirth("M");
        optionalInfo.setBasicInfo(basicInfo);
        user.setOptionalInformation(optionalInfo);
        userDao.save(user);

        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(userDao, "testuser",
                "optionalInformation.person");
        Message response = service.executeAndGetResponse();

        assertEquals(UserMessage.SUCCESS, response);

        User updatedUser = userDao.get("testuser").orElse(null);
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getOptionalInformation());
        assertNull(updatedUser.getOptionalInformation().getPerson());
        // basicInfo should still be there
        assertNotNull(updatedUser.getOptionalInformation().getBasicInfo());
    }

    @Test
    public void invalidFieldPathReturnsError() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .buildAndPersist(userDao);

        DeleteUserProfileFieldService service = new DeleteUserProfileFieldService(userDao, "testuser",
                "invalid.path.that.does.not.exist");
        Message response = service.executeAndGetResponse();

        // Should handle gracefully - either return error or succeed (field doesn't
        // exist anyway)
        // For now, let's say it returns success (no-op if field doesn't exist)
        assertEquals(UserMessage.SUCCESS, response);
    }

    @Test
    public void deleteIndividualDemographicFields() {
        User user = EntityFactory.createUser()
                .withUsername("testuser")
                .withUserType(UserType.Client)
                .build();

        OptionalInformation optionalInfo = new OptionalInformation();
        DemographicInfo demographicInfo = new DemographicInfo();
        demographicInfo.setLanguagePreference("Spanish");
        demographicInfo.setIsEthnicityHispanicLatino(true);
        demographicInfo.setRace(Race.ASIAN);
        demographicInfo.setCityOfBirth("Philadelphia");
        demographicInfo.setStateOfBirth("PA");
        demographicInfo.setCountryOfBirth("USA");
        demographicInfo.setCitizenship(Citizenship.US_CITIZEN);
        optionalInfo.setDemographicInfo(demographicInfo);
        user.setOptionalInformation(optionalInfo);
        userDao.save(user);

        // Delete only languagePreference
        DeleteUserProfileFieldService service1 = new DeleteUserProfileFieldService(userDao, "testuser",
                "optionalInformation.demographicInfo.languagePreference");
        Message response1 = service1.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response1);

        User updatedUser1 = userDao.get("testuser").orElse(null);
        assertNotNull(updatedUser1);
        assertNotNull(updatedUser1.getOptionalInformation());
        assertNotNull(updatedUser1.getOptionalInformation().getDemographicInfo());
        assertNull(updatedUser1.getOptionalInformation().getDemographicInfo().getLanguagePreference());
        // Other fields should still be there
        assertTrue(updatedUser1.getOptionalInformation().getDemographicInfo().getIsEthnicityHispanicLatino());
        assertEquals(Race.ASIAN, updatedUser1.getOptionalInformation().getDemographicInfo().getRace());
        assertEquals("Philadelphia", updatedUser1.getOptionalInformation().getDemographicInfo().getCityOfBirth());

        // Delete only race
        DeleteUserProfileFieldService service2 = new DeleteUserProfileFieldService(userDao, "testuser",
                "optionalInformation.demographicInfo.race");
        Message response2 = service2.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response2);

        User updatedUser2 = userDao.get("testuser").orElse(null);
        assertNotNull(updatedUser2);
        assertNotNull(updatedUser2.getOptionalInformation());
        assertNotNull(updatedUser2.getOptionalInformation().getDemographicInfo());
        assertNull(updatedUser2.getOptionalInformation().getDemographicInfo().getRace());
        // Other fields should still be there
        assertTrue(updatedUser2.getOptionalInformation().getDemographicInfo().getIsEthnicityHispanicLatino());
        assertEquals("Philadelphia", updatedUser2.getOptionalInformation().getDemographicInfo().getCityOfBirth());
    }
}
