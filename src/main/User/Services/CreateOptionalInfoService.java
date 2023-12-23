package User.Services;

import Config.Message;
import Config.Service;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import User.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;

public class CreateOptionalInfoService implements Service {
    private OptionalUserInformationDao optionalUserInformationDao;

    // Attributes for OptionalUserInformation
    private String username;

    // Attributes for Person
    private String firstName, middleName, lastName, ssn;
    private Date birthDate;

    // Attributes for BasicInfo (including Address)
    private String genderAssignedAtBirth, emailAddress, phoneNumber;
    private Address mailingAddress, residentialAddress;
    private Boolean differentBirthName;
    // ... add all other attributes from BasicInfo

    // Attributes for DemographicInfo
    private String languagePreference;
    private Boolean isEthnicityHispanicLatino;
    private Race race; // Assuming Race is an enum
    private String cityOfBirth, stateOfBirth, countryOfBirth;
    private Citizenship citizenship; // Assuming Citizenship is an enum

    // Attributes for FamilyInfo
    private List<Person> parents, legalGuardians, children, siblings;
    private MaritalStatus maritalStatus; // Assuming MaritalStatus is an enum
    private Person spouse;

    // Attributes for VeteranStatus
    private boolean isVeteran, isProtectedVeteran;
    private String militaryBranch, yearsOfService, militaryRank, dischargeType;


    public CreateOptionalInfoService(
            OptionalUserInformationDao dao,
            String username,
            // Parameters for Person
            String firstName, String middleName, String lastName, String ssn, Date birthDate,
            // Parameters for BasicInfo
            String genderAssignedAtBirth, String emailAddress, String phoneNumber,
            Address mailingAddress, Address residentialAddress, Boolean differentBirthName,
            // ... add all other parameters for BasicInfo
            // Parameters for DemographicInfo
            String languagePreference, Boolean isEthnicityHispanicLatino, Race race,
            String cityOfBirth, String stateOfBirth, String countryOfBirth, Citizenship citizenship,
            // Parameters for FamilyInfo
            List<Person> parents, List<Person> legalGuardians, MaritalStatus maritalStatus, Person spouse,
            List<Person> children, List<Person> siblings,
            // Parameters for VeteranStatus
            boolean isVeteran, boolean isProtectedVeteran, String militaryBranch,
            String yearsOfService, String militaryRank, String dischargeType
    ) {
        // Initialize all the fields with the provided parameters
        this.optionalUserInformationDao = dao;
        this.username = username;
        // ... initialize other fields
    }
    
    @Override
    public Message executeAndGetResponse() {
        try{
            OptionalUserInformation userInfo = objectMapper.readValue(json, OptionalUserInformation.class);
            optionalUserInformationDao.save(userInfo);
            return UserMessage.SUCCESS;
        } catch (JsonProcessingException e) {
            return UserMessage.INVALID_PARAMETER;
        }
    }
}
