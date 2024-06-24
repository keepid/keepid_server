package OptionalUserInformationTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import Database.OptionalUserInformation.OptionalUserInformationDaoFactory;
import OptionalUserInformation.*;
import OptionalUserInformation.Services.CreateOptionalInfoService;
import OptionalUserInformation.Services.DeleteOptionalInfoService;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class DeleteOptionalUserInformationUnitTest {
    OptionalUserInformationDao optionalUserInformationDao = OptionalUserInformationDaoFactory.create(DeploymentLevel.IN_MEMORY);

    @After
    public void reset() {optionalUserInformationDao.clear();}

    @Test
    public void success(){
        CreateOptionalInfoService createOptionalInfoService = new CreateOptionalInfoService(
                optionalUserInformationDao,
                "testUser",
                // Parameters for Person
                "John", "Doe", "Doe", "123-45-6789", "2020-01-01",
                // Parameters for BasicInfo
                "Male", "test@example.com", "123-456-7890",
                Address.builder()
                        .streetAddress("123 Main St")
                        .apartmentNumber("101")
                        .city("City")
                        .state("State")
                        .zip("12345")
                        .build(),
                Address.builder()
                        .streetAddress("456 Elm St")
                        .apartmentNumber("101")
                        .city("City")
                        .state("State")
                        .zip("54321")
                        .build(),
                true,
                "Jr.", "John", "M", "Doe",
                "Jr.", "987654321", true,
                // Parameters for DemographicInfo
                "English", true, Race.WHITE,
                "City", "State", "Country", Citizenship.US_CITIZEN,
                // Parameters for FamilyInfo
                new ArrayList<>(), new ArrayList<>(), MaritalStatus.SINGLE, new Person(),
                new ArrayList<>(), new ArrayList<>(),
                // Parameters for VeteranStatus
                true, false, "Navy",
                "10", "Captain", "Honorable"
        );
        Message response = createOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response);
        OptionalUserInformation savedInfo = optionalUserInformationDao.get("testUser").orElse(null);
        assertNotNull(savedInfo);

        assertEquals("testUser", savedInfo.getUsername());

        DeleteOptionalInfoService deleteOptionalInfoService = new DeleteOptionalInfoService(optionalUserInformationDao
            ,savedInfo.getUsername());
        Message response1 = deleteOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response1);
        OptionalUserInformation savedInfo1 = optionalUserInformationDao.get("testUser").orElse(null);
        assertNull(savedInfo1);
        }

    @Test
    public void success2(){
        CreateOptionalInfoService createOptionalInfoService = new CreateOptionalInfoService(
                optionalUserInformationDao,
                "testUser",
                // Parameters for Person
                "John", "Doe", "Doe", "123-45-6789", "2020-01-01",
                // Parameters for BasicInfo
                "Male", "test@example.com", "123-456-7890",
                Address.builder()
                        .streetAddress("123 Main St")
                        .apartmentNumber("101")
                        .city("City")
                        .state("State")
                        .zip("12345")
                        .build(),
                Address.builder()
                        .streetAddress("456 Elm St")
                        .apartmentNumber("101")
                        .city("City")
                        .state("State")
                        .zip("54321")
                        .build(),
                true,
                "Jr.", "John", "M", "Doe",
                "Jr.", "987654321", true,
                // Parameters for DemographicInfo
                "English", true, Race.WHITE,
                "City", "State", "Country", Citizenship.US_CITIZEN,
                // Parameters for FamilyInfo
                new ArrayList<>(), new ArrayList<>(), MaritalStatus.SINGLE, new Person(),
                new ArrayList<>(), new ArrayList<>(),
                // Parameters for VeteranStatus
                true, false, "Navy",
                "10", "Captain", "Honorable"
        );
        Message response = createOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response);
        OptionalUserInformation savedInfo = optionalUserInformationDao.get("testUser").orElse(null);
        assertNotNull(savedInfo);

        assertEquals("testUser", savedInfo.getUsername());

        CreateOptionalInfoService createOptionalInfoService1 = new CreateOptionalInfoService(
                optionalUserInformationDao,
                "testUser1",
                // Parameters for Person
                "John", "Doe", "Doe", "123-45-6789", "2020-01-01",
                // Parameters for BasicInfo
                "Male", "test@example.com", "123-456-7890",
                Address.builder()
                        .streetAddress("123 Main St")
                        .apartmentNumber("101")
                        .city("City")
                        .state("State")
                        .zip("12345")
                        .build(),
                Address.builder()
                        .streetAddress("456 Elm St")
                        .apartmentNumber("101")
                        .city("City")
                        .state("State")
                        .zip("54321")
                        .build(),
                true,
                "Jr.", "John", "M", "Doe",
                "Jr.", "987654321", true,
                // Parameters for DemographicInfo
                "English", true, Race.WHITE,
                "City", "State", "Country", Citizenship.US_CITIZEN,
                // Parameters for FamilyInfo
                new ArrayList<>(), new ArrayList<>(), MaritalStatus.SINGLE, new Person(),
                new ArrayList<>(), new ArrayList<>(),
                // Parameters for VeteranStatus
                true, false, "Navy",
                "10", "Captain", "Honorable"
        );
        Message response1 = createOptionalInfoService1.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response1);
        OptionalUserInformation savedInfo1 = optionalUserInformationDao.get("testUser1").orElse(null);
        assertNotNull(savedInfo1);

        assertEquals("testUser1", savedInfo1.getUsername());

        //Deleted second user
        DeleteOptionalInfoService deleteOptionalInfoService = new DeleteOptionalInfoService(optionalUserInformationDao
                ,savedInfo1.getUsername());
        Message response2 = deleteOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response2);
        OptionalUserInformation DeletedInfo = optionalUserInformationDao.get("testUser1").orElse(null);
        assertNull(DeletedInfo);

        //Check if first user still exists
        OptionalUserInformation notDeletedInfo = optionalUserInformationDao.get("testUser").orElse(null);
        assertNotNull(notDeletedInfo);
    }

    @Test
    public void username_not_found(){
        CreateOptionalInfoService createOptionalInfoService = new CreateOptionalInfoService(
                optionalUserInformationDao,
                "testUser",
                // Parameters for Person
                "John", "Doe", "Doe", "123-45-6789", "2020-01-01",
                // Parameters for BasicInfo
                "Male", "test@example.com", "123-456-7890",
                Address.builder()
                        .streetAddress("123 Main St")
                        .apartmentNumber("101")
                        .city("City")
                        .state("State")
                        .zip("12345")
                        .build(),
                Address.builder()
                        .streetAddress("456 Elm St")
                        .apartmentNumber("101")
                        .city("City")
                        .state("State")
                        .zip("54321")
                        .build(),
                true,
                "Jr.", "John", "M", "Doe",
                "Jr.", "987654321", true,
                // Parameters for DemographicInfo
                "English", true, Race.WHITE,
                "City", "State", "Country", Citizenship.US_CITIZEN,
                // Parameters for FamilyInfo
                new ArrayList<>(), new ArrayList<>(), MaritalStatus.SINGLE, new Person(),
                new ArrayList<>(), new ArrayList<>(),
                // Parameters for VeteranStatus
                true, false, "Navy",
                "10", "Captain", "Honorable"
        );
        Message response = createOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response);
        OptionalUserInformation savedInfo = optionalUserInformationDao.get("testUser").orElse(null);
        assertNotNull(savedInfo);

        assertEquals("testUser", savedInfo.getUsername());

        DeleteOptionalInfoService deleteOptionalInfoService = new DeleteOptionalInfoService(optionalUserInformationDao
                ,"testUser1");
        Message response1 = deleteOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.USER_NOT_FOUND, response1);
        OptionalUserInformation savedInfo1 = optionalUserInformationDao.get("testUser").orElse(null);
        assertNotNull(savedInfo1);
    }
}
