package OptionalUserInformationTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import Database.OptionalUserInformation.OptionalUserInformationDaoFactory;
import OptionalUserInformation.*;
import OptionalUserInformation.Services.CreateOptionalInfoService;
import OptionalUserInformation.Services.UpdateOptionalInfoService;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class UpdateOptionalUserInformationUnitTest {
    OptionalUserInformationDao optionalUserInformationDao = OptionalUserInformationDaoFactory.create(DeploymentLevel.IN_MEMORY);

    @After
    public void reset() {optionalUserInformationDao.clear();}

    @Test
    public void success() throws IOException, ClassNotFoundException {
        int year = 2023;
        int month = Calendar.JUNE;
        int dayOfMonth = 24;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        Date date = calendar.getTime();

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

        assertEquals("John", savedInfo.getPerson().getFirstName());
        assertEquals("Doe", savedInfo.getPerson().getMiddleName());
        assertEquals("Doe", savedInfo.getPerson().getLastName());
        assertEquals("123-45-6789", savedInfo.getPerson().getSsn());
        assertEquals(date, savedInfo.getPerson().getBirthDate());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(savedInfo);
        oos.close();

        // Deserialize the object
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        OptionalUserInformation copiedOpt = (OptionalUserInformation) ois.readObject();
        ois.close();


        copiedOpt.setPerson(new Person("Jet", "Stream", "Sam", "987-65-4321"
                , "2022-01-01"));

        UpdateOptionalInfoService updateOptionalInfoService = new UpdateOptionalInfoService(optionalUserInformationDao,
                copiedOpt);
        Message response1 = updateOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response1);
        OptionalUserInformation updatedInfo = optionalUserInformationDao.get("testUser").orElse(null);
        assertNotNull(updatedInfo);

        assertEquals("testUser", updatedInfo.getUsername());

        //For Person
        assertEquals("Jet", updatedInfo.getPerson().getFirstName());
        assertEquals("Stream", updatedInfo.getPerson().getMiddleName());
        assertEquals("Sam", updatedInfo.getPerson().getLastName());
        assertEquals("987-65-4321", updatedInfo.getPerson().getSsn());
        assertEquals("2022-01-01", updatedInfo.getPerson().getBirthDate());

        // For BasicInfo
        assertEquals("Male", updatedInfo.getBasicInfo().getGenderAssignedAtBirth());
        assertEquals("test@example.com", updatedInfo.getBasicInfo().getEmailAddress());
        assertEquals("123-456-7890", updatedInfo.getBasicInfo().getPhoneNumber());
        assertEquals("John", updatedInfo.getBasicInfo().getFirstName());
        assertEquals("M", updatedInfo.getBasicInfo().getBirthMiddleName());
        assertEquals("Doe", updatedInfo.getBasicInfo().getLastName());
        assertEquals("Jr.", updatedInfo.getBasicInfo().getSuffix());
        assertEquals("987654321", updatedInfo.getBasicInfo().getStateIdNumber());
        assertEquals(true, updatedInfo.getBasicInfo().getHaveDisability());
        assertEquals("123 Main St", updatedInfo.getBasicInfo().getMailingAddress().getStreetAddress());
        assertEquals("456 Elm St", updatedInfo.getBasicInfo().getResidentialAddress().getStreetAddress());

        // For DemographicInfo
        assertEquals("English", updatedInfo.getDemographicInfo().getLanguagePreference());
        assertEquals(true, updatedInfo.getDemographicInfo().getIsEthnicityHispanicLatino());
        assertEquals(Race.WHITE, updatedInfo.getDemographicInfo().getRace());
        assertEquals("City", updatedInfo.getDemographicInfo().getCityOfBirth());
        assertEquals(Citizenship.US_CITIZEN, updatedInfo.getDemographicInfo().getCitizenship());

        // For FamilyInfo
        assertEquals(MaritalStatus.SINGLE, updatedInfo.getFamilyInfo().getMaritalStatus());

        // For VeteranStatus
        assertTrue(updatedInfo.getVeteranStatus().isVeteran());
        assertFalse(updatedInfo.getVeteranStatus().isProtectedVeteran());
        assertEquals("Navy", updatedInfo.getVeteranStatus().getBranch());
        assertEquals("Captain", updatedInfo.getVeteranStatus().getRank());
        assertEquals("Honorable", updatedInfo.getVeteranStatus().getDischarge());
    }

    @Test
    public void user_not_found(){
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

        OptionalUserInformation optionalUserInformationDifferentUsername = new OptionalUserInformation();
        optionalUserInformationDifferentUsername.setUsername("testUser1");

        UpdateOptionalInfoService updateOptionalInfoService = new UpdateOptionalInfoService(optionalUserInformationDao,
                optionalUserInformationDifferentUsername);
        Message response1 = updateOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.USER_NOT_FOUND, response1);
    }

    @Test
    public void success_multiple_users(){
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

        assertEquals("John", savedInfo.getPerson().getFirstName());
        assertEquals("Doe", savedInfo.getPerson().getMiddleName());
        assertEquals("Doe", savedInfo.getPerson().getLastName());
        assertEquals("123-45-6789", savedInfo.getPerson().getSsn());
        assertEquals(Date.class, savedInfo.getPerson().getBirthDate().getClass());

        //Create Second User
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

        savedInfo.setPerson(new Person("Jet", "Stream", "Sam", "987-65-4321"
                , "2022-01-01"));

        UpdateOptionalInfoService updateOptionalInfoService = new UpdateOptionalInfoService(optionalUserInformationDao,
                savedInfo);
        Message response2 = updateOptionalInfoService.executeAndGetResponse();
        assertEquals(UserMessage.SUCCESS, response2);
        OptionalUserInformation updatedInfo = optionalUserInformationDao.get("testUser").orElse(null);
        assertNotNull(updatedInfo);

        assertEquals("testUser", updatedInfo.getUsername());

        //For Person
        assertEquals("Jet", updatedInfo.getPerson().getFirstName());
        assertEquals("Stream", updatedInfo.getPerson().getMiddleName());
        assertEquals("Sam", updatedInfo.getPerson().getLastName());
        assertEquals("987-65-4321", updatedInfo.getPerson().getSsn());
        assertEquals("2022-01-01", updatedInfo.getPerson().getBirthDate());

        OptionalUserInformation notUpdatedInfo = optionalUserInformationDao.get("testUser1").orElse(null);
        assertNotNull(notUpdatedInfo);

        assertEquals("testUser1", notUpdatedInfo.getUsername());

        assertEquals("John", notUpdatedInfo.getPerson().getFirstName());
        assertEquals("Doe", notUpdatedInfo.getPerson().getMiddleName());
        assertEquals("Doe", notUpdatedInfo.getPerson().getLastName());
        assertEquals("123-45-6789", notUpdatedInfo.getPerson().getSsn());
        assertEquals("2020-01-01", notUpdatedInfo.getPerson().getBirthDate());

    }
}
