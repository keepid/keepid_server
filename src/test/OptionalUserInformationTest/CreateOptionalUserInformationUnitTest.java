package OptionalUserInformationTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import Database.OptionalUserInformation.OptionalUserInformationDaoFactory;
import Database.User.UserDao;
import User.*;
import User.Services.CreateOptionalInfoService;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class CreateOptionalUserInformationUnitTest {
    OptionalUserInformationDao optionalUserInformationDao = OptionalUserInformationDaoFactory.create(DeploymentLevel.IN_MEMORY);

    @After
    public void reset() {optionalUserInformationDao.clear();}

    @Test
    public void success(){
        CreateOptionalInfoService createOptionalInfoService = new CreateOptionalInfoService(
                optionalUserInformationDao,
                "testuser",
                // Parameters for Person
                "John", "Doe", "Doe", "123-45-6789", new Date(),
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
        assertEquals(response, UserMessage.SUCCESS);
    }

}
