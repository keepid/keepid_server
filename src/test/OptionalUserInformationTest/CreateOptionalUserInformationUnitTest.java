package OptionalUserInformationTest;

import static org.junit.jupiter.api.Assertions.*;

import Config.DeploymentLevel;
import Config.Message;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import Database.OptionalUserInformation.OptionalUserInformationDaoFactory;
import OptionalUserInformation.*;
import OptionalUserInformation.Services.CreateOptionalInfoService;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

@Slf4j
public class CreateOptionalUserInformationUnitTest {
  OptionalUserInformationDao optionalUserInformationDao =
      OptionalUserInformationDaoFactory.create(DeploymentLevel.IN_MEMORY);
  ActivityDao activityDao = ActivityDaoFactory.create(DeploymentLevel.IN_MEMORY);
  Format formatter = new SimpleDateFormat("yyyy-MM-dd");

  @After
  public void reset() {
    optionalUserInformationDao.clear();
  }

  @Test
  public void success() throws ParseException {
    CreateOptionalInfoService createOptionalInfoService =
        new CreateOptionalInfoService(
            optionalUserInformationDao,
            "testUser",
            // Parameters for Person
            "John",
            "Doe",
            "Doe",
            "123-45-6789",
            new SimpleDateFormat("yyyy-MM-dd").parse("2020-01-01"),
            // Parameters for BasicInfo
            "Male",
            "test@example.com",
            "123-456-7890",
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
            "Jr.",
            "John",
            "M",
            "Doe",
            "Jr.",
            "987654321",
            true,
            // Parameters for DemographicInfo
            "English",
            true,
            Race.WHITE,
            "City",
            "State",
            "Country",
            Citizenship.US_CITIZEN,
            // Parameters for FamilyInfo
            new ArrayList<>(),
            new ArrayList<>(),
            MaritalStatus.SINGLE,
            new Person(),
            new ArrayList<>(),
            new ArrayList<>(),
            // Parameters for VeteranStatus
            true,
            false,
            "Navy",
            "10",
            "Captain",
            "Honorable");
    Message response = createOptionalInfoService.executeAndGetResponse();
    assertEquals(UserMessage.SUCCESS, response);
    OptionalUserInformation savedInfo = optionalUserInformationDao.get("testUser").orElse(null);
    assertNotNull(savedInfo);

    assertEquals("testUser", savedInfo.getUsername());

    // For Person
    assertEquals("John", savedInfo.getPerson().getFirstName());
    assertEquals("Doe", savedInfo.getPerson().getMiddleName());
    assertEquals("Doe", savedInfo.getPerson().getLastName());
    assertEquals("123-45-6789", savedInfo.getPerson().getSsn());
    assertEquals("2020-01-01", formatter.format(savedInfo.getPerson().getBirthDate()));

    // For BasicInfo
    assertEquals("Male", savedInfo.getBasicInfo().getGenderAssignedAtBirth());
    assertEquals("test@example.com", savedInfo.getBasicInfo().getEmailAddress());
    assertEquals("123-456-7890", savedInfo.getBasicInfo().getPhoneNumber());
    assertEquals("John", savedInfo.getBasicInfo().getFirstName());
    assertEquals("M", savedInfo.getBasicInfo().getBirthMiddleName());
    assertEquals("Doe", savedInfo.getBasicInfo().getLastName());
    assertEquals("Jr.", savedInfo.getBasicInfo().getSuffix());
    assertEquals("987654321", savedInfo.getBasicInfo().getStateIdNumber());
    assertEquals(true, savedInfo.getBasicInfo().getHaveDisability());
    assertEquals("123 Main St", savedInfo.getBasicInfo().getMailingAddress().getStreetAddress());
    assertEquals("456 Elm St", savedInfo.getBasicInfo().getResidentialAddress().getStreetAddress());

    // For DemographicInfo
    assertEquals("English", savedInfo.getDemographicInfo().getLanguagePreference());
    assertEquals(true, savedInfo.getDemographicInfo().getIsEthnicityHispanicLatino());
    assertEquals(Race.WHITE, savedInfo.getDemographicInfo().getRace());
    assertEquals("City", savedInfo.getDemographicInfo().getCityOfBirth());
    assertEquals(Citizenship.US_CITIZEN, savedInfo.getDemographicInfo().getCitizenship());

    // For FamilyInfo
    assertEquals(MaritalStatus.SINGLE, savedInfo.getFamilyInfo().getMaritalStatus());

    // For VeteranStatus
    assertTrue(savedInfo.getVeteranStatus().isVeteran());
    assertFalse(savedInfo.getVeteranStatus().isProtectedVeteran());
    assertEquals("Navy", savedInfo.getVeteranStatus().getBranch());
    assertEquals("Captain", savedInfo.getVeteranStatus().getRank());
    assertEquals("Honorable", savedInfo.getVeteranStatus().getDischarge());
  }

  @Test
  public void repeated_user_name() throws ParseException {
    CreateOptionalInfoService createOptionalInfoService =
        new CreateOptionalInfoService(
            optionalUserInformationDao,
            "testUser",
            // Parameters for Person
            "John",
            "Doe",
            "Doe",
            "123-45-6789",
            new SimpleDateFormat("yyyy-MM-dd").parse("2020-01-01"),
            // Parameters for BasicInfo
            "Male",
            "test@example.com",
            "123-456-7890",
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
            "Jr.",
            "John",
            "M",
            "Doe",
            "Jr.",
            "987654321",
            true,
            // Parameters for DemographicInfo
            "English",
            true,
            Race.WHITE,
            "City",
            "State",
            "Country",
            Citizenship.US_CITIZEN,
            // Parameters for FamilyInfo
            new ArrayList<>(),
            new ArrayList<>(),
            MaritalStatus.SINGLE,
            new Person(),
            new ArrayList<>(),
            new ArrayList<>(),
            // Parameters for VeteranStatus
            true,
            false,
            "Navy",
            "10",
            "Captain",
            "Honorable");
    Message response = createOptionalInfoService.executeAndGetResponse();
    assertEquals(UserMessage.SUCCESS, response);
    OptionalUserInformation savedInfo = optionalUserInformationDao.get("testUser").orElse(null);
    assertNotNull(savedInfo);

    assertEquals("testUser", savedInfo.getUsername());

    CreateOptionalInfoService createOptionalInfoService1 =
        new CreateOptionalInfoService(
            optionalUserInformationDao,
            "testUser",
            // Parameters for Person
            "John",
            "Doe",
            "Doe",
            "123-45-6789",
            new SimpleDateFormat("yyyy-MM-dd").parse("2020-01-01"),
            // Parameters for BasicInfo
            "Male",
            "test@example.com",
            "123-456-7890",
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
            "Jr.",
            "John",
            "M",
            "Doe",
            "Jr.",
            "987654321",
            true,
            // Parameters for DemographicInfo
            "English",
            true,
            Race.WHITE,
            "City",
            "State",
            "Country",
            Citizenship.US_CITIZEN,
            // Parameters for FamilyInfo
            new ArrayList<>(),
            new ArrayList<>(),
            MaritalStatus.SINGLE,
            new Person(),
            new ArrayList<>(),
            new ArrayList<>(),
            // Parameters for VeteranStatus
            true,
            false,
            "Navy",
            "10",
            "Captain",
            "Honorable");
    Message response1 = createOptionalInfoService.executeAndGetResponse();
    assertEquals(UserMessage.USERNAME_ALREADY_EXISTS, response1);
  }

  @Test
  public void success_multiple_users() throws ParseException {
    CreateOptionalInfoService createOptionalInfoService =
        new CreateOptionalInfoService(
            optionalUserInformationDao,
            "testUser",
            // Parameters for Person
            "John",
            "Doe",
            "Doe",
            "123-45-6789",
            new SimpleDateFormat("yyyy-MM-dd").parse("2020-01-01"),
            // Parameters for BasicInfo
            "Male",
            "test@example.com",
            "123-456-7890",
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
            "Jr.",
            "John",
            "M",
            "Doe",
            "Jr.",
            "987654321",
            true,
            // Parameters for DemographicInfo
            "English",
            true,
            Race.WHITE,
            "City",
            "State",
            "Country",
            Citizenship.US_CITIZEN,
            // Parameters for FamilyInfo
            new ArrayList<>(),
            new ArrayList<>(),
            MaritalStatus.SINGLE,
            new Person(),
            new ArrayList<>(),
            new ArrayList<>(),
            // Parameters for VeteranStatus
            true,
            false,
            "Navy",
            "10",
            "Captain",
            "Honorable");
    Message response = createOptionalInfoService.executeAndGetResponse();
    assertEquals(UserMessage.SUCCESS, response);
    OptionalUserInformation savedInfo = optionalUserInformationDao.get("testUser").orElse(null);
    assertNotNull(savedInfo);

    assertEquals("testUser", savedInfo.getUsername());

    // For Person
    assertEquals("John", savedInfo.getPerson().getFirstName());
    assertEquals("Doe", savedInfo.getPerson().getMiddleName());
    assertEquals("Doe", savedInfo.getPerson().getLastName());
    assertEquals("123-45-6789", savedInfo.getPerson().getSsn());
    assertEquals("2020-01-01", formatter.format(savedInfo.getPerson().getBirthDate()));

    // For BasicInfo
    assertEquals("Male", savedInfo.getBasicInfo().getGenderAssignedAtBirth());
    assertEquals("test@example.com", savedInfo.getBasicInfo().getEmailAddress());
    assertEquals("123-456-7890", savedInfo.getBasicInfo().getPhoneNumber());
    assertEquals("John", savedInfo.getBasicInfo().getFirstName());
    assertEquals("M", savedInfo.getBasicInfo().getBirthMiddleName());
    assertEquals("Doe", savedInfo.getBasicInfo().getLastName());
    assertEquals("Jr.", savedInfo.getBasicInfo().getSuffix());
    assertEquals("987654321", savedInfo.getBasicInfo().getStateIdNumber());
    assertEquals(true, savedInfo.getBasicInfo().getHaveDisability());
    assertEquals("123 Main St", savedInfo.getBasicInfo().getMailingAddress().getStreetAddress());
    assertEquals("456 Elm St", savedInfo.getBasicInfo().getResidentialAddress().getStreetAddress());

    // For DemographicInfo
    assertEquals("English", savedInfo.getDemographicInfo().getLanguagePreference());
    assertEquals(true, savedInfo.getDemographicInfo().getIsEthnicityHispanicLatino());
    assertEquals(Race.WHITE, savedInfo.getDemographicInfo().getRace());
    assertEquals("City", savedInfo.getDemographicInfo().getCityOfBirth());
    assertEquals(Citizenship.US_CITIZEN, savedInfo.getDemographicInfo().getCitizenship());

    // For FamilyInfo
    assertEquals(MaritalStatus.SINGLE, savedInfo.getFamilyInfo().getMaritalStatus());

    // For VeteranStatus
    assertTrue(savedInfo.getVeteranStatus().isVeteran());
    assertFalse(savedInfo.getVeteranStatus().isProtectedVeteran());
    assertEquals("Navy", savedInfo.getVeteranStatus().getBranch());
    assertEquals("Captain", savedInfo.getVeteranStatus().getRank());
    assertEquals("Honorable", savedInfo.getVeteranStatus().getDischarge());

    CreateOptionalInfoService createOptionalInfoService1 =
        new CreateOptionalInfoService(
            optionalUserInformationDao,
            "testUser1",
            // Parameters for Person
            "John",
            "Doe",
            "Doe",
            "123-45-6789",
            new SimpleDateFormat("yyyy-MM-dd").parse("2020-01-01"),
            // Parameters for BasicInfo
            "Male",
            "test@example.com",
            "123-456-7890",
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
            "Jr.",
            "John",
            "M",
            "Doe",
            "Jr.",
            "987654321",
            true,
            // Parameters for DemographicInfo
            "English",
            true,
            Race.WHITE,
            "City",
            "State",
            "Country",
            Citizenship.US_CITIZEN,
            // Parameters for FamilyInfo
            new ArrayList<>(),
            new ArrayList<>(),
            MaritalStatus.SINGLE,
            new Person(),
            new ArrayList<>(),
            new ArrayList<>(),
            // Parameters for VeteranStatus
            true,
            false,
            "Navy",
            "10",
            "Captain",
            "Honorable");
    Message response1 = createOptionalInfoService1.executeAndGetResponse();
    assertEquals(UserMessage.SUCCESS, response1);
    OptionalUserInformation savedInfo1 = optionalUserInformationDao.get("testUser1").orElse(null);
    assertNotNull(savedInfo1);

    assertEquals("testUser1", savedInfo1.getUsername());

    // For Person
    assertEquals("John", savedInfo1.getPerson().getFirstName());
    assertEquals("Doe", savedInfo1.getPerson().getMiddleName());
    assertEquals("Doe", savedInfo1.getPerson().getLastName());
    assertEquals("123-45-6789", savedInfo1.getPerson().getSsn());
    assertEquals("2020-01-01", formatter.format(savedInfo1.getPerson().getBirthDate()));

    // For BasicInfo
    assertEquals("Male", savedInfo1.getBasicInfo().getGenderAssignedAtBirth());
    assertEquals("test@example.com", savedInfo1.getBasicInfo().getEmailAddress());
    assertEquals("123-456-7890", savedInfo1.getBasicInfo().getPhoneNumber());
    assertEquals("John", savedInfo1.getBasicInfo().getFirstName());
    assertEquals("M", savedInfo1.getBasicInfo().getBirthMiddleName());
    assertEquals("Doe", savedInfo1.getBasicInfo().getLastName());
    assertEquals("Jr.", savedInfo1.getBasicInfo().getSuffix());
    assertEquals("987654321", savedInfo1.getBasicInfo().getStateIdNumber());
    assertEquals(true, savedInfo1.getBasicInfo().getHaveDisability());
    assertEquals("123 Main St", savedInfo1.getBasicInfo().getMailingAddress().getStreetAddress());
    assertEquals(
        "456 Elm St", savedInfo1.getBasicInfo().getResidentialAddress().getStreetAddress());

    // For DemographicInfo
    assertEquals("English", savedInfo1.getDemographicInfo().getLanguagePreference());
    assertEquals(true, savedInfo1.getDemographicInfo().getIsEthnicityHispanicLatino());
    assertEquals(Race.WHITE, savedInfo1.getDemographicInfo().getRace());
    assertEquals("City", savedInfo1.getDemographicInfo().getCityOfBirth());
    assertEquals(Citizenship.US_CITIZEN, savedInfo1.getDemographicInfo().getCitizenship());

    // For FamilyInfo
    assertEquals(MaritalStatus.SINGLE, savedInfo1.getFamilyInfo().getMaritalStatus());

    // For VeteranStatus
    assertTrue(savedInfo1.getVeteranStatus().isVeteran());
    assertFalse(savedInfo1.getVeteranStatus().isProtectedVeteran());
    assertEquals("Navy", savedInfo1.getVeteranStatus().getBranch());
    assertEquals("Captain", savedInfo1.getVeteranStatus().getRank());
    assertEquals("Honorable", savedInfo1.getVeteranStatus().getDischarge());
  }
}
