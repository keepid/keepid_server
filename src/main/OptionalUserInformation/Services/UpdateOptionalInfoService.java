package OptionalUserInformation.Services;

import Activity.UserActivity.UserInformationActivity.ChangeOptionalUserInformationActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import OptionalUserInformation.*;
import java.text.Format;
import java.text.SimpleDateFormat;

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
    if (!oldPerson.getFirstName().equals(newPerson.getFirstName())) {
      recordChangeOptionalUserInformation(
          username, "First Name", oldPerson.getFirstName(), newPerson.getFirstName());
    }
    if (!oldPerson.getMiddleName().equals(newPerson.getMiddleName())) {
      recordChangeOptionalUserInformation(
          username, "Middle Name", oldPerson.getMiddleName(), newPerson.getMiddleName());
    }
    if (!oldPerson.getLastName().equals(newPerson.getLastName())) {
      recordChangeOptionalUserInformation(
          username, "Last Name", oldPerson.getLastName(), newPerson.getLastName());
    }
    if (!oldPerson.getSsn().equals(newPerson.getSsn())) {
      recordChangeOptionalUserInformation(username, "SSN", oldPerson.getSsn(), newPerson.getSsn());
    }
    if (!oldPerson.getBirthDate().equals(newPerson.getBirthDate())) {
      Format formatter = new SimpleDateFormat("yyyy-MM-dd");
      recordChangeOptionalUserInformation(
          username,
          "Birth Date",
          formatter.format(oldPerson.getBirthDate()),
          formatter.format(newPerson.getBirthDate()));
    }

    // Basic Info
    BasicInfo oldBasicInfo = old.getBasicInfo();
    BasicInfo newBasicInfo = optionalUserInformation.getBasicInfo();
    if (!oldBasicInfo.getBirthFirstName().equals(newBasicInfo.getBirthFirstName())) {
      recordChangeOptionalUserInformation(
          username,
          "Birth First Name",
          oldBasicInfo.getBirthFirstName(),
          newBasicInfo.getBirthFirstName());
    }
    if (!oldBasicInfo.getBirthMiddleName().equals(newBasicInfo.getBirthMiddleName())) {
      recordChangeOptionalUserInformation(
          username,
          "Birth Middle Name",
          oldBasicInfo.getBirthMiddleName(),
          newBasicInfo.getBirthMiddleName());
    }
    if (!oldBasicInfo.getBirthLastName().equals(newBasicInfo.getBirthLastName())) {
      recordChangeOptionalUserInformation(
          username,
          "Birth Last Name",
          oldBasicInfo.getBirthLastName(),
          newBasicInfo.getBirthLastName());
    }
    if (!oldBasicInfo.getSuffix().equals(newBasicInfo.getSuffix())) {
      recordChangeOptionalUserInformation(
          username, "Suffix", oldBasicInfo.getSuffix(), newBasicInfo.getSuffix());
    }
    if (!oldBasicInfo.getBirthSuffix().equals(newBasicInfo.getBirthSuffix())) {
      recordChangeOptionalUserInformation(
          username, "Birth Suffix", oldBasicInfo.getBirthSuffix(), newBasicInfo.getBirthSuffix());
    }
    if (!oldBasicInfo.getStateIdNumber().equals(newBasicInfo.getStateIdNumber())) {
      recordChangeOptionalUserInformation(
          username, "State ID", oldBasicInfo.getStateIdNumber(), newBasicInfo.getStateIdNumber());
    }
    if (!oldBasicInfo.getHaveDisability().equals(newBasicInfo.getHaveDisability())) {
      recordChangeOptionalUserInformation(
          username,
          "Disability Status",
          String.valueOf(oldBasicInfo.getHaveDisability()),
          String.valueOf(newBasicInfo.getHaveDisability()));
    }
    if (!oldBasicInfo.getDifferentBirthName().equals(newBasicInfo.getDifferentBirthName())) {
      recordChangeOptionalUserInformation(
          username,
          "Different Birth Name",
          String.valueOf(oldBasicInfo.getDifferentBirthName()),
          String.valueOf(newBasicInfo.getDifferentBirthName()));
    }
    if (!oldBasicInfo.getGenderAssignedAtBirth().equals(newBasicInfo.getGenderAssignedAtBirth())) {
      recordChangeOptionalUserInformation(
          username,
          "Gender Assigned at Birth",
          oldBasicInfo.getGenderAssignedAtBirth(),
          newBasicInfo.getGenderAssignedAtBirth());
    }
    if (!oldBasicInfo.getEmailAddress().equals(newBasicInfo.getEmailAddress())) {
      recordChangeOptionalUserInformation(
          username,
          "Email Address",
          oldBasicInfo.getEmailAddress(),
          newBasicInfo.getEmailAddress());
    }
    if (!oldBasicInfo.getPhoneNumber().equals(newBasicInfo.getPhoneNumber())) {
      recordChangeOptionalUserInformation(
          username, "Phone Number", oldBasicInfo.getPhoneNumber(), newBasicInfo.getPhoneNumber());
    }
    if (!oldBasicInfo.getResidentialAddress().equals(newBasicInfo.getResidentialAddress())) {
      recordChangeOptionalUserInformation(
          username,
          "Residential Address",
          oldBasicInfo.getResidentialAddress().toString(),
          newBasicInfo.getResidentialAddress().toString());
    }
    if (!oldBasicInfo.getMailingAddress().equals(newBasicInfo.getMailingAddress())) {
      recordChangeOptionalUserInformation(
          username,
          "Mailing Address",
          oldBasicInfo.getMailingAddress().toString(),
          newBasicInfo.getMailingAddress().toString());
    }

    // Demographic Info
    DemographicInfo oldDemographicInfo = old.getDemographicInfo();
    DemographicInfo newDemographicInfo = optionalUserInformation.getDemographicInfo();
    if (!oldDemographicInfo
        .getLanguagePreference()
        .equals(newDemographicInfo.getLanguagePreference())) {
      recordChangeOptionalUserInformation(
          username,
          "Language Preference",
          oldDemographicInfo.getLanguagePreference(),
          newDemographicInfo.getLanguagePreference());
    }
    if (!oldDemographicInfo
        .getIsEthnicityHispanicLatino()
        .equals(newDemographicInfo.getIsEthnicityHispanicLatino())) {
      recordChangeOptionalUserInformation(
          username,
          "Is Hispanic or Latino",
          String.valueOf(oldDemographicInfo.getIsEthnicityHispanicLatino()),
          String.valueOf(newDemographicInfo.getIsEthnicityHispanicLatino()));
    }
    if (!oldDemographicInfo.getRace().equals(newDemographicInfo.getRace())) {
      recordChangeOptionalUserInformation(
          username,
          "Race",
          oldDemographicInfo.getRace().toString(),
          newDemographicInfo.getRace().toString());
    }
    if (!oldDemographicInfo.getCityOfBirth().equals(newDemographicInfo.getCityOfBirth())) {
      recordChangeOptionalUserInformation(
          username,
          "City of Birth",
          oldDemographicInfo.getCityOfBirth(),
          newDemographicInfo.getCityOfBirth());
    }
    if (!oldDemographicInfo.getStateOfBirth().equals(newDemographicInfo.getStateOfBirth())) {
      recordChangeOptionalUserInformation(
          username,
          "State of Birth",
          oldDemographicInfo.getStateOfBirth(),
          newDemographicInfo.getStateOfBirth());
    }
    if (!oldDemographicInfo.getCountryOfBirth().equals(newDemographicInfo.getCountryOfBirth())) {
      recordChangeOptionalUserInformation(
          username,
          "Country of Birth",
          oldDemographicInfo.getCountryOfBirth(),
          newDemographicInfo.getCountryOfBirth());
    }
    if (!oldDemographicInfo.getCitizenship().equals(newDemographicInfo.getCitizenship())) {
      recordChangeOptionalUserInformation(
          username,
          "Citizenship",
          oldDemographicInfo.getCitizenship().toString(),
          newDemographicInfo.getCitizenship().toString());
    }

    // Skipping Family Info for now since it's extremely annoying

    // Veteran Status
    VeteranStatus oldVeteranStatus = old.getVeteranStatus();
    VeteranStatus newVeteranStatus = optionalUserInformation.getVeteranStatus();
    if (oldVeteranStatus.isVeteran() != newVeteranStatus.isVeteran()) {
      recordChangeOptionalUserInformation(
          username,
          "Is Veteran",
          String.valueOf(oldVeteranStatus.isVeteran()),
          String.valueOf(newVeteranStatus.isVeteran()));
    }
    if (oldVeteranStatus.isProtectedVeteran() != newVeteranStatus.isProtectedVeteran()) {
      recordChangeOptionalUserInformation(
          username,
          "Is Protected Veteran",
          String.valueOf(oldVeteranStatus.isProtectedVeteran()),
          String.valueOf(newVeteranStatus.isVeteran()));
    }
    if (!oldVeteranStatus.getBranch().equals(newVeteranStatus.getBranch())) {
      recordChangeOptionalUserInformation(
          username, "Branch/Service", oldVeteranStatus.getBranch(), newVeteranStatus.getBranch());
    }
    if (!oldVeteranStatus.getYearsOfService().equals(newVeteranStatus.getYearsOfService())) {
      recordChangeOptionalUserInformation(
          username,
          "Years of Service",
          oldVeteranStatus.getYearsOfService(),
          newVeteranStatus.getYearsOfService());
    }
    if (!oldVeteranStatus.getDischarge().equals(newVeteranStatus.getDischarge())) {
      recordChangeOptionalUserInformation(
          username,
          "Discharge Type",
          oldVeteranStatus.getDischarge(),
          newVeteranStatus.getDischarge());
    }
    if (!oldVeteranStatus.getRank().equals(newVeteranStatus.getRank())) {
      recordChangeOptionalUserInformation(
          username, "Rank at Discharge", oldVeteranStatus.getRank(), newVeteranStatus.getRank());
    }

    optionalUserInformationDao.update(optionalUserInformation);
  }

  private void recordChangeOptionalUserInformation() {
    ChangeOptionalUserInformationActivity a =
        new ChangeOptionalUserInformationActivity(optionalUserInformation.getUsername());
    activityDao.save(a);
  }

  private void recordChangeOptionalUserInformation(
      String username, String attribute, String oldValue, String newValue) {
    ChangeOptionalUserInformationActivity a =
        new ChangeOptionalUserInformationActivity(username, attribute, oldValue, newValue);
    activityDao.save(a);
  }
}
