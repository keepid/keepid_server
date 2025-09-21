package OptionalUserInformation.Services;

import Activity.UserActivity.UserInformationActivity.ChangeOptionalUserInformationActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import OptionalUserInformation.*;
import java.text.SimpleDateFormat;
import java.util.Objects;

public class UpdateOptionalInfoService implements Service {
  OptionalUserInformationDao optionalUserInformationDao;
  ActivityDao activityDao;
  OptionalUserInformation optionalUserInformation;

  public UpdateOptionalInfoService(
      OptionalUserInformationDao dao,
      ActivityDao activityDao,
      OptionalUserInformation optionalUserInformation) {
    this.optionalUserInformationDao = dao;
    this.activityDao = activityDao;
    this.optionalUserInformation = optionalUserInformation;
  }

  @Override
  public Message executeAndGetResponse() {
    OptionalUserInformation old;
    if (optionalUserInformationDao.get(optionalUserInformation.getUsername()).isEmpty()) {
      return UserMessage.USER_NOT_FOUND;
    } else {
      old = optionalUserInformationDao.get(optionalUserInformation.getUsername()).get();
    }
    updateOptionalUserInformation(old);
    return UserMessage.SUCCESS;
  }

  private void updateOptionalUserInformation(OptionalUserInformation old) {
    String username = old.getUsername();

    // Person
    Person oldPerson = old.getPerson();
    Person newPerson = optionalUserInformation.getPerson();
    checkAndRecord(username, "First Name", oldPerson.getFirstName(), newPerson.getFirstName());
    checkAndRecord(username, "Middle Name", oldPerson.getMiddleName(), newPerson.getMiddleName());
    checkAndRecord(username, "Last Name", oldPerson.getLastName(), newPerson.getLastName());
    checkAndRecord(username, "SSN", oldPerson.getSsn(), newPerson.getSsn());
    checkAndRecord(
        username,
        "Birth Date",
        new SimpleDateFormat("yyyy-MM-dd").format(oldPerson.getBirthDate()),
        new SimpleDateFormat("yyyy-MM-dd").format(newPerson.getBirthDate()));

    // Basic Info
    BasicInfo oldBasicInfo = old.getBasicInfo();
    BasicInfo newBasicInfo = optionalUserInformation.getBasicInfo();
    checkAndRecord(
        username,
        "Birth First Name",
        oldBasicInfo.getBirthFirstName(),
        newBasicInfo.getBirthFirstName());
    checkAndRecord(
        username,
        "Birth Middle Name",
        oldBasicInfo.getBirthMiddleName(),
        newBasicInfo.getBirthMiddleName());
    checkAndRecord(
        username,
        "Birth Last Name",
        oldBasicInfo.getBirthLastName(),
        newBasicInfo.getBirthLastName());
    checkAndRecord(username, "Suffix", oldBasicInfo.getSuffix(), newBasicInfo.getSuffix());
    checkAndRecord(
        username, "Birth Suffix", oldBasicInfo.getBirthSuffix(), newBasicInfo.getBirthSuffix());
    checkAndRecord(
        username, "State ID", oldBasicInfo.getStateIdNumber(), newBasicInfo.getStateIdNumber());
    checkAndRecord(
        username,
        "Disability Status",
        oldBasicInfo.getHaveDisability(),
        newBasicInfo.getHaveDisability());
    checkAndRecord(
        username,
        "Different Birth Name",
        oldBasicInfo.getDifferentBirthName(),
        newBasicInfo.getDifferentBirthName());
    checkAndRecord(
        username,
        "Gender Assigned at Birth",
        oldBasicInfo.getGenderAssignedAtBirth(),
        newBasicInfo.getGenderAssignedAtBirth());
    checkAndRecord(
        username, "Email Address", oldBasicInfo.getEmailAddress(), newBasicInfo.getEmailAddress());
    checkAndRecord(
        username, "Phone Number", oldBasicInfo.getPhoneNumber(), newBasicInfo.getPhoneNumber());
    checkAndRecord(
        username,
        "Residential Address",
        oldBasicInfo.getResidentialAddress(),
        newBasicInfo.getResidentialAddress());
    checkAndRecord(
        username,
        "Mailing Address",
        oldBasicInfo.getMailingAddress(),
        newBasicInfo.getMailingAddress());

    // Demographic Info
    DemographicInfo oldDemo = old.getDemographicInfo();
    DemographicInfo newDemo = optionalUserInformation.getDemographicInfo();
    checkAndRecord(
        username,
        "Language Preference",
        oldDemo.getLanguagePreference(),
        newDemo.getLanguagePreference());
    checkAndRecord(
        username,
        "Is Hispanic or Latino",
        oldDemo.getIsEthnicityHispanicLatino(),
        newDemo.getIsEthnicityHispanicLatino());
    checkAndRecord(username, "Race", oldDemo.getRace(), newDemo.getRace());
    checkAndRecord(username, "City of Birth", oldDemo.getCityOfBirth(), newDemo.getCityOfBirth());
    checkAndRecord(
        username, "State of Birth", oldDemo.getStateOfBirth(), newDemo.getStateOfBirth());
    checkAndRecord(
        username, "Country of Birth", oldDemo.getCountryOfBirth(), newDemo.getCountryOfBirth());
    checkAndRecord(username, "Citizenship", oldDemo.getCitizenship(), newDemo.getCitizenship());

    // Veteran Status
    VeteranStatus oldVet = old.getVeteranStatus();
    VeteranStatus newVet = optionalUserInformation.getVeteranStatus();
    checkAndRecord(username, "Is Veteran", oldVet.isVeteran(), newVet.isVeteran());
    checkAndRecord(
        username, "Is Protected Veteran", oldVet.isProtectedVeteran(), newVet.isProtectedVeteran());
    checkAndRecord(username, "Branch/Service", oldVet.getBranch(), newVet.getBranch());
    checkAndRecord(
        username, "Years of Service", oldVet.getYearsOfService(), newVet.getYearsOfService());
    checkAndRecord(username, "Discharge Type", oldVet.getDischarge(), newVet.getDischarge());
    checkAndRecord(username, "Rank at Discharge", oldVet.getRank(), newVet.getRank());

    optionalUserInformationDao.update(optionalUserInformation);
  }

  private <T> void checkAndRecord(String username, String fieldName, T oldValue, T newValue) {
    if (!Objects.equals(oldValue, newValue)) {
      recordChangeOptionalUserInformation(
          username, fieldName, String.valueOf(oldValue), String.valueOf(newValue));
    }
  }

  //  private void recordChangeOptionalUserInformation() {
  //    ChangeOptionalUserInformationActivity a =
  //        new ChangeOptionalUserInformationActivity(optionalUserInformation.getUsername());
  //    activityDao.save(a);
  //  }

  private void recordChangeOptionalUserInformation(
      String username, String attribute, String oldValue, String newValue) {
    ChangeOptionalUserInformationActivity a =
        new ChangeOptionalUserInformationActivity(username, attribute, oldValue, newValue);
    activityDao.save(a);
  }
}
