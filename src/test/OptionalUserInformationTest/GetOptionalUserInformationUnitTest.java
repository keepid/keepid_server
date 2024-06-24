package OptionalUserInformationTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import Database.OptionalUserInformation.OptionalUserInformationDaoFactory;
import OptionalUserInformation.*;
import OptionalUserInformation.Services.CreateOptionalInfoService;
import OptionalUserInformation.Services.GetOptionalInfoService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class GetOptionalUserInformationUnitTest {
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


        GetOptionalInfoService getOptionalInfoService = new GetOptionalInfoService(optionalUserInformationDao,
                savedInfo.getUsername());
        Message response1 = getOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response1);
        JSONObject savedInfo1 = getOptionalInfoService.getOptionalInformationFields();

        assertTrue(savedInfo.serialize().similar(savedInfo1));
    }

    @Test
    public void get_different_user(){
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
                "John12", "Doe12", "Doe12", "123-45-678912", "2020-01-01",
                // Parameters for BasicInfo
                "Male", "test12@example.com", "123-456-1212",
                Address.builder()
                        .streetAddress("12312 Main St")
                        .apartmentNumber("10112")
                        .city("City1212")
                        .state("State1212")
                        .zip("123451212")
                        .build(),
                Address.builder()
                        .streetAddress("4561212 Elm St")
                        .apartmentNumber("1011212")
                        .city("City1212")
                        .state("State1212")
                        .zip("543211212")
                        .build(),
                true,
                "Jr.12", "John12", "M12", "Doe12",
                "Jr.12", "98765432112", true,
                // Parameters for DemographicInfo
                "English", true, Race.ASIAN,
                "City12", "State12", "Country12", Citizenship.OTHER,
                // Parameters for FamilyInfo
                new ArrayList<>(), new ArrayList<>(), MaritalStatus.MARRIED, new Person(),
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

        GetOptionalInfoService getOptionalInfoService = new GetOptionalInfoService(optionalUserInformationDao,
                savedInfo.getUsername());
        Message response2 = getOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response2);
        JSONObject SerializedInfo = getOptionalInfoService.getOptionalInformationFields();

        GetOptionalInfoService getOptionalInfoService1 = new GetOptionalInfoService(optionalUserInformationDao,
                savedInfo1.getUsername());
        Message response3 = getOptionalInfoService1.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response3);
        JSONObject SerializedInfo1 = getOptionalInfoService1.getOptionalInformationFields();

        assertTrue(savedInfo.serialize().similar(SerializedInfo));
        assertFalse(savedInfo.serialize().similar(SerializedInfo1));
        assertTrue(savedInfo1.serialize().similar(SerializedInfo1));
        assertFalse(savedInfo1.serialize().similar(SerializedInfo));
    }

    @Test
    public void no_user_found(){
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

        GetOptionalInfoService getOptionalInfoService = new GetOptionalInfoService(optionalUserInformationDao,
                "testUser1");
        Message response1 = getOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.USER_NOT_FOUND, response1);
        }
}