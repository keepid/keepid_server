package OptionalUserInformation.Services;

import Config.Message;
import Config.Service;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import OptionalUserInformation.*;
import java.util.Date;
import java.util.List;

public class CreateOptionalInfoService implements Service {
  private final OptionalUserInformationDao optionalUserInformationDao;

  // Attributes for OptionalUserInformation
  private String username;

  // Attributes for Person
  private String firstName, middleName, lastName, ssn;
  Date birthDate;
  // Attributes for BasicInfo (including Address)
  private String genderAssignedAtBirth,
      emailAddress,
      phoneNumber,
      suffix,
      birthFirstName,
      birthMiddleName,
      birthLastName,
      birthSuffix,
      stateIdNumber;
  private Address mailingAddress, residentialAddress;
  private Boolean differentBirthName, haveDisability;

  // Attributes for DemographicInfo
  private String languagePreference;
  private Boolean isEthnicityHispanicLatino;
  private Race race;
  private String cityOfBirth, stateOfBirth, countryOfBirth;
  private Citizenship citizenship;

  // Attributes for FamilyInfo
  private List<Person> parents, legalGuardians, children, siblings;
  private MaritalStatus maritalStatus;
  private Person spouse;

  // Attributes for VeteranStatus
  private boolean isVeteran, isProtectedVeteran;
  private String branch, yearsOfService, rank, discharge;

  public CreateOptionalInfoService(
      OptionalUserInformationDao dao,
      String username,
      // Parameters for Person
      String firstName,
      String middleName,
      String lastName,
      String ssn,
      Date birthDate,
      // Parameters for BasicInfo
      String genderAssignedAtBirth,
      String emailAddress,
      String phoneNumber,
      Address mailingAddress,
      Address residentialAddress,
      Boolean differentBirthName,
      String suffix,
      String birthFirstName,
      String birthMiddleName,
      String birthLastName,
      String birthSuffix,
      String stateIdNumber,
      Boolean haveDisability,
      // Parameters for DemographicInfo
      String languagePreference,
      Boolean isEthnicityHispanicLatino,
      Race race,
      String cityOfBirth,
      String stateOfBirth,
      String countryOfBirth,
      Citizenship citizenship,
      // Parameters for FamilyInfo
      List<Person> parents,
      List<Person> legalGuardians,
      MaritalStatus maritalStatus,
      Person spouse,
      List<Person> children,
      List<Person> siblings,
      // Parameters for VeteranStatus
      boolean isVeteran,
      boolean isProtectedVeteran,
      String branch,
      String yearsOfService,
      String rank,
      String discharge) {
    // Initialize all the fields with the provided parameters
    this.optionalUserInformationDao = dao;
    this.username = username;

    // Initialize fields for Person
    this.firstName = firstName;
    this.middleName = middleName;
    this.lastName = lastName;
    this.ssn = ssn;
    this.birthDate = birthDate;

    // Initialize fields for BasicInfo
    this.genderAssignedAtBirth = genderAssignedAtBirth;
    this.emailAddress = emailAddress;
    this.phoneNumber = phoneNumber;
    this.mailingAddress = mailingAddress;
    this.residentialAddress = residentialAddress;
    this.differentBirthName = differentBirthName;
    this.suffix = suffix;
    this.birthFirstName = birthFirstName;
    this.birthMiddleName = birthMiddleName;
    this.birthLastName = birthLastName;
    this.birthSuffix = birthSuffix;
    this.stateIdNumber = stateIdNumber;
    this.haveDisability = haveDisability;

    // Initialize fields for DemographicInfo
    this.languagePreference = languagePreference;
    this.isEthnicityHispanicLatino = isEthnicityHispanicLatino;
    this.race = race;
    this.cityOfBirth = cityOfBirth;
    this.stateOfBirth = stateOfBirth;
    this.countryOfBirth = countryOfBirth;
    this.citizenship = citizenship;

    // Initialize fields for FamilyInfo
    this.parents = parents;
    this.legalGuardians = legalGuardians;
    this.maritalStatus = maritalStatus;
    this.spouse = spouse;
    this.children = children;
    this.siblings = siblings;

    // Initialize fields for VeteranStatus
    this.isVeteran = isVeteran;
    this.isProtectedVeteran = isProtectedVeteran;
    this.branch = branch;
    this.yearsOfService = yearsOfService;
    this.rank = rank;
    this.discharge = discharge;
  }

  public OptionalUserInformation build() {
    // Constructing nested objects
    Person self =
        Person.builder()
            .firstName(firstName)
            .middleName(middleName)
            .lastName(lastName)
            .ssn(ssn)
            .birthDate(birthDate)
            .build();

    BasicInfo basicInfo =
        BasicInfo.builder()
            .genderAssignedAtBirth(genderAssignedAtBirth)
            .mailingAddress(mailingAddress)
            .residentialAddress(residentialAddress)
            .emailAddress(emailAddress)
            .phoneNumber(phoneNumber)
            .differentBirthName(differentBirthName)
            .firstName(firstName)
            .middleName(middleName)
            .lastName(lastName)
            .suffix(suffix)
            .birthFirstName(birthFirstName)
            .birthMiddleName(birthMiddleName)
            .birthLastName(birthLastName)
            .birthSuffix(birthSuffix)
            .ssn(ssn)
            .haveDisability(haveDisability)
            .stateIdNumber(stateIdNumber)
            .build();

    DemographicInfo demographicInfo =
        DemographicInfo.builder()
            .languagePreference(languagePreference)
            .isEthnicityHispanicLatino(isEthnicityHispanicLatino)
            .race(race)
            .cityOfBirth(cityOfBirth)
            .stateOfBirth(stateOfBirth)
            .countryOfBirth(countryOfBirth)
            .citizenship(citizenship)
            .build();

    FamilyInfo familyInfo =
        FamilyInfo.builder()
            .parents(parents)
            .legalGuardians(legalGuardians)
            .maritalStatus(maritalStatus)
            .spouse(spouse)
            .children(children)
            .siblings(siblings)
            .build();

    VeteranStatus veteranStatus =
        VeteranStatus.builder()
            .isVeteran(isVeteran)
            .isProtectedVeteran(isProtectedVeteran)
            .branch(branch)
            .yearsOfService(yearsOfService)
            .rank(rank)
            .discharge(discharge)
            .build();

    // Saving the object
    return OptionalUserInformation.builder()
        .username(username)
        .person(self)
        .basicInfo(basicInfo)
        .demographicInfo(demographicInfo)
        .familyInfo(familyInfo)
        .veteranStatus(veteranStatus)
        .build();
  }

  @Override
  public Message executeAndGetResponse() {
    if (optionalUserInformationDao.get(username).isPresent()) {
      return UserMessage.USERNAME_ALREADY_EXISTS;
    }
    OptionalUserInformation optionalUserInformation = this.build();
    optionalUserInformationDao.save(optionalUserInformation);
    return UserMessage.SUCCESS;
  }
}
