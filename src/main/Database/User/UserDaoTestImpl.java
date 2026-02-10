package Database.User;

import Config.DeploymentLevel;
import User.UserInformation.*;
import User.OptionalInformation;
import User.User;
import java.util.*;
import org.bson.types.ObjectId;

public class UserDaoTestImpl implements UserDao {
  Map<String, User> userMap;

  public UserDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in memory test database in production or staging");
    }
    userMap = new LinkedHashMap<>();
  }

  @Override
  public Optional<User> get(String username) {
    return Optional.ofNullable(userMap.get(username));
  }

  @Override
  public Optional<User> getByEmail(String email) {
    return Optional.empty();
  }

  @Override
  public void delete(String username) {
    userMap.remove(username);
  }

  @Override
  public Optional<User> get(ObjectId id) {
    for (User user : userMap.values()) {
      if (user.getId().equals(id)) {
        return Optional.of(user);
      }
    }
    return Optional.empty();
  }

  @Override
  public List<User> getAll() {
    return new ArrayList<User>(userMap.values());
  }

  @Override
  public List<User> getAllFromOrg(String orgName) {
    List<User> result = new ArrayList<>();
    for (User user : userMap.values()) {
      if (user.getOrganization().equals(orgName)) {
        result.add(user);
      }
    }
    return result;
  }

  @Override
  public List<User> getAllFromOrg(ObjectId objectId) {
    List<User> result = new ArrayList<>();
    for (User user : userMap.values()) {
      if (user.getId().equals(objectId)) {
        result.add(user);
      }
    }
    return result;
  }

  @Override
  public int size() {
    return userMap.size();
  }

  @Override
  public void delete(User user) {
    userMap.remove(user.getUsername());
  }

  @Override
  public void clear() {
    userMap.clear();
  }

  @Override
  public void update(User user) {
    userMap.put(user.getUsername(), user);
  }

  @Override
  public void resetPassword(User user, String password) {
    user.setPassword(password);
    userMap.put(user.getUsername(), user);
  }

  @Override
  public void save(User user) {
    userMap.put(user.getUsername(), user);
  }

  @Override
  public void deleteField(String username, String fieldPath) {
    User user = userMap.get(username);
    if (user == null) {
      return;
    }

    // For in-memory implementation, manually delete the field
    // This is a simplified version - in production MongoDB handles this with $unset
    deleteFieldFromUser(user, fieldPath);
    userMap.put(username, user);
  }

  @Override
  public void updateField(String username, String fieldPath, Object value) {
    User user = userMap.get(username);
    if (user == null) {
      return;
    }

    // For in-memory implementation, manually update the field
    // This is a simplified version - in production MongoDB handles this with $set
    updateFieldInUser(user, fieldPath, value);
    userMap.put(username, user);
  }

  private void updateFieldInUser(User user, String fieldPath, Object value) {
    try {
      String[] parts = fieldPath.split("\\.");

      if (parts.length == 1) {
        // Root level field
        updateRootField(user, parts[0], value);
      } else if (parts.length == 2 && parts[0].equals("optionalInformation")) {
        // optionalInformation.fieldName
        if (user.getOptionalInformation() == null) {
          user.setOptionalInformation(new OptionalInformation());
        }
        updateOptionalInformationField(value);
      } else if (parts.length >= 3 && parts[0].equals("optionalInformation")) {
        // optionalInformation.nested.field
        if (user.getOptionalInformation() == null) {
          user.setOptionalInformation(new OptionalInformation());
        }
        updateNestedField(user.getOptionalInformation(), parts, 1, value);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error updating field: " + fieldPath, e);
    }
  }

  private void updateRootField(User user, String fieldName, Object value) {
    // Handle root level fields that are allowed to be updated
    switch (fieldName) {
      case "email":
        user.setEmail((String) value);
        break;
      case "phone":
        user.setPhone((String) value);
        break;
      case "address":
        user.setAddress((String) value);
        break;
      case "city":
        user.setCity((String) value);
        break;
      case "state":
        user.setState((String) value);
        break;
      case "zipcode":
        user.setZipcode((String) value);
        break;
      case "firstName":
        user.setFirstName((String) value);
        break;
      case "lastName":
        user.setLastName((String) value);
        break;
      default:
        // Unknown field name - ignore
        break;
    }
  }

  @SuppressWarnings("unused")
  private void updateOptionalInformationField(Object value) {
    // This would be for setting entire objects, which is less common
    // Most updates will go through updateNestedField
    // Parameter kept for potential future use
  }

  private void updateNestedField(OptionalInformation optionalInfo, String[] parts, int startIndex, Object value) {
    if (startIndex >= parts.length) {
      return;
    }

    String currentPart = parts[startIndex];

    if (currentPart.equals("person")) {
      if (optionalInfo.getPerson() == null) {
        optionalInfo.setPerson(new Person());
      }
      if (startIndex + 1 < parts.length) {
        updatePersonField(optionalInfo.getPerson(), parts, startIndex + 1, value);
      }
    } else if (currentPart.equals("basicInfo")) {
      if (optionalInfo.getBasicInfo() == null) {
        optionalInfo.setBasicInfo(new BasicInfo());
      }
      if (startIndex + 1 < parts.length) {
        updateBasicInfoField(optionalInfo.getBasicInfo(), parts, startIndex + 1, value);
      }
    } else if (currentPart.equals("veteranStatus")) {
      if (optionalInfo.getVeteranStatus() == null) {
        optionalInfo.setVeteranStatus(new VeteranStatus());
      }
      if (startIndex + 1 < parts.length) {
        updateVeteranStatusField(optionalInfo.getVeteranStatus(), parts, startIndex + 1, value);
      }
    } else if (currentPart.equals("demographicInfo")) {
      if (optionalInfo.getDemographicInfo() == null) {
        optionalInfo.setDemographicInfo(new DemographicInfo());
      }
      if (startIndex + 1 < parts.length) {
        updateDemographicInfoField(optionalInfo.getDemographicInfo(), parts, startIndex + 1, value);
      }
    }
  }

  private void updatePersonField(Person person, String[] parts, int startIndex, Object value) {
    if (startIndex >= parts.length) {
      return;
    }
    String field = parts[startIndex];
    switch (field) {
      case "middleName":
        person.setMiddleName((String) value);
        break;
      case "ssn":
        person.setSsn((String) value);
        break;
      case "birthDate":
        person.setBirthDate((Date) value);
        break;
      default:
        // Unknown field name - ignore
        break;
    }
  }

  private void updateBasicInfoField(BasicInfo basicInfo, String[] parts, int startIndex, Object value) {
    if (startIndex >= parts.length) {
      return;
    }
    String field = parts[startIndex];
    switch (field) {
      case "genderAssignedAtBirth":
        basicInfo.setGenderAssignedAtBirth((String) value);
        break;
      case "emailAddress":
        basicInfo.setEmailAddress((String) value);
        break;
      case "phoneNumber":
        basicInfo.setPhoneNumber((String) value);
        break;
      case "mailingAddress":
        if (startIndex + 1 < parts.length) {
          if (basicInfo.getMailingAddress() == null) {
            basicInfo.setMailingAddress(new Address());
          }
          updateAddressField(basicInfo.getMailingAddress(), parts, startIndex + 1, value);
        } else {
          basicInfo.setMailingAddress((Address) value);
        }
        break;
      case "residentialAddress":
        if (startIndex + 1 < parts.length) {
          if (basicInfo.getResidentialAddress() == null) {
            basicInfo.setResidentialAddress(new Address());
          }
          updateAddressField(basicInfo.getResidentialAddress(), parts, startIndex + 1, value);
        } else {
          basicInfo.setResidentialAddress((Address) value);
        }
        break;
      default:
        // Unknown field name - ignore
        break;
    }
  }

  private void updateAddressField(Address address, String[] parts, int startIndex, Object value) {
    if (startIndex >= parts.length) {
      return;
    }
    String field = parts[startIndex];
    switch (field) {
      case "streetAddress":
        address.setStreetAddress((String) value);
        break;
      case "apartmentNumber":
        address.setApartmentNumber((String) value);
        break;
      case "city":
        address.setCity((String) value);
        break;
      case "state":
        address.setState((String) value);
        break;
      case "zip":
        address.setZip((String) value);
        break;
      default:
        // Unknown field name - ignore
        break;
    }
  }

  private void updateVeteranStatusField(VeteranStatus veteranStatus, String[] parts, int startIndex, Object value) {
    if (startIndex >= parts.length) {
      return;
    }
    String field = parts[startIndex];
    switch (field) {
      case "branch":
        veteranStatus.setBranch((String) value);
        break;
      case "yearsOfService":
        veteranStatus.setYearsOfService((String) value);
        break;
      case "rank":
        veteranStatus.setRank((String) value);
        break;
      case "discharge":
        veteranStatus.setDischarge((String) value);
        break;
      default:
        // Unknown field name - ignore
        break;
    }
  }

  private void updateDemographicInfoField(DemographicInfo demographicInfo, String[] parts, int startIndex,
      Object value) {
    if (startIndex >= parts.length) {
      return;
    }
    String field = parts[startIndex];
    switch (field) {
      case "languagePreference":
        demographicInfo.setLanguagePreference((String) value);
        break;
      case "isEthnicityHispanicLatino":
        demographicInfo.setIsEthnicityHispanicLatino((Boolean) value);
        break;
      case "race":
        demographicInfo.setRace((Race) value);
        break;
      case "cityOfBirth":
        demographicInfo.setCityOfBirth((String) value);
        break;
      case "stateOfBirth":
        demographicInfo.setStateOfBirth((String) value);
        break;
      case "countryOfBirth":
        demographicInfo.setCountryOfBirth((String) value);
        break;
      case "citizenship":
        demographicInfo.setCitizenship((Citizenship) value);
        break;
      default:
        // Unknown field name - ignore
        break;
    }
  }

  private void deleteFieldFromUser(User user, String fieldPath) {
    try {
      String[] parts = fieldPath.split("\\.");

      if (parts.length == 1) {
        // Root level field - not supported for required fields, but handle optional
        // ones
        // This should be caught by validation, but handle gracefully
        return;
      } else if (parts.length == 2 && parts[0].equals("optionalInformation")) {
        // optionalInformation.fieldName
        if (user.getOptionalInformation() == null) {
          return;
        }
        deleteFromOptionalInformation(user.getOptionalInformation(), parts[1]);
      } else if (parts.length >= 3 && parts[0].equals("optionalInformation")) {
        // optionalInformation.nested.field
        if (user.getOptionalInformation() == null) {
          return;
        }
        deleteNestedField(user.getOptionalInformation(), parts, 1);
      }
    } catch (Exception e) {
      // Log and rethrow to see what's happening
      throw new IllegalStateException("Error deleting field: " + fieldPath, e);
    }
  }

  private void deleteFromOptionalInformation(OptionalInformation optionalInfo, String fieldName) {
    switch (fieldName) {
      case "person":
        optionalInfo.setPerson(null);
        break;
      case "basicInfo":
        optionalInfo.setBasicInfo(null);
        break;
      case "demographicInfo":
        optionalInfo.setDemographicInfo(null);
        break;
      case "familyInfo":
        optionalInfo.setFamilyInfo(null);
        break;
      case "veteranStatus":
        optionalInfo.setVeteranStatus(null);
        break;
      default:
        // Unknown field name - ignore
        break;
    }
  }

  private void deleteNestedField(OptionalInformation optionalInfo, String[] parts, int startIndex) {
    if (startIndex >= parts.length) {
      return;
    }

    String currentPart = parts[startIndex];

    if (currentPart.equals("person") && optionalInfo.getPerson() != null && startIndex + 1 < parts.length) {
      deleteFromPerson(optionalInfo.getPerson(), parts, startIndex + 1);
    } else if (currentPart.equals("basicInfo") && optionalInfo.getBasicInfo() != null
        && startIndex + 1 < parts.length) {
      deleteFromBasicInfo(optionalInfo.getBasicInfo(), parts, startIndex + 1);
    } else if (currentPart.equals("veteranStatus") && optionalInfo.getVeteranStatus() != null
        && startIndex + 1 < parts.length) {
      deleteFromVeteranStatus(optionalInfo.getVeteranStatus(), parts, startIndex + 1);
    } else if (currentPart.equals("demographicInfo") && optionalInfo.getDemographicInfo() != null
        && startIndex + 1 < parts.length) {
      deleteFromDemographicInfo(optionalInfo.getDemographicInfo(), parts, startIndex + 1);
    } else if (currentPart.equals("familyInfo") && optionalInfo.getFamilyInfo() != null
        && startIndex + 1 < parts.length) {
      deleteFromFamilyInfo(optionalInfo.getFamilyInfo(), parts, startIndex + 1);
    }
  }

  private void deleteFromPerson(Person person, String[] parts, int startIndex) {
    if (startIndex >= parts.length) {
      return;
    }
    String field = parts[startIndex];
    switch (field) {
      case "middleName":
        person.setMiddleName(null);
        break;
      case "ssn":
        person.setSsn(null);
        break;
      case "birthDate":
        person.setBirthDate(null);
        break;
      default:
        // Unknown field, ignore
        break;
    }
  }

  private void deleteFromBasicInfo(BasicInfo basicInfo, String[] parts, int startIndex) {
    if (startIndex >= parts.length) {
      return;
    }
    String field = parts[startIndex];
    switch (field) {
      case "mailingAddress":
        if (startIndex + 1 >= parts.length) {
          // Delete entire mailingAddress object
          basicInfo.setMailingAddress(null);
        } else {
          // Delete nested field within mailingAddress
          Address mailingAddr = basicInfo.getMailingAddress();
          if (mailingAddr != null) {
            deleteFromAddress(mailingAddr, parts, startIndex + 1);
            // If all fields are null, we could set the whole address to null, but keep it
            // for now
          }
        }
        break;
      case "residentialAddress":
        if (startIndex + 1 >= parts.length) {
          // Delete entire residentialAddress object
          basicInfo.setResidentialAddress(null);
        } else {
          // Delete nested field within residentialAddress
          Address residentialAddr = basicInfo.getResidentialAddress();
          if (residentialAddr != null) {
            deleteFromAddress(residentialAddr, parts, startIndex + 1);
          }
        }
        break;
      case "genderAssignedAtBirth":
        basicInfo.setGenderAssignedAtBirth(null);
        break;
      case "emailAddress":
        basicInfo.setEmailAddress(null);
        break;
      case "phoneNumber":
        basicInfo.setPhoneNumber(null);
        break;
      default:
        // Handle other BasicInfo fields using reflection or setter methods
        // For now, just log that we don't handle this field
        break;
    }
  }

  private void deleteFromAddress(Address address, String[] parts, int startIndex) {
    if (address == null || startIndex >= parts.length) {
      return;
    }
    String field = parts[startIndex];
    switch (field) {
      case "streetAddress":
        address.setStreetAddress(null);
        break;
      case "apartmentNumber":
        address.setApartmentNumber(null);
        break;
      case "city":
        address.setCity(null);
        break;
      case "state":
        address.setState(null);
        break;
      case "zip":
        address.setZip(null);
        break;
      default:
        // Unknown field name - ignore
        break;
    }
  }

  private void deleteFromVeteranStatus(VeteranStatus veteranStatus, String[] parts, int startIndex) {
    if (startIndex >= parts.length) {
      return;
    }
    String field = parts[startIndex];
    switch (field) {
      case "branch":
        veteranStatus.setBranch(null);
        break;
      case "yearsOfService":
        veteranStatus.setYearsOfService(null);
        break;
      case "rank":
        veteranStatus.setRank(null);
        break;
      case "discharge":
        veteranStatus.setDischarge(null);
        break;
      default:
        // Unknown field name - ignore
        break;
    }
  }

  private void deleteFromDemographicInfo(DemographicInfo demographicInfo, String[] parts, int startIndex) {
    if (startIndex >= parts.length) {
      return;
    }
    String field = parts[startIndex];
    switch (field) {
      case "languagePreference":
        demographicInfo.setLanguagePreference(null);
        break;
      case "isEthnicityHispanicLatino":
        demographicInfo.setIsEthnicityHispanicLatino(null);
        break;
      case "race":
        demographicInfo.setRace(null);
        break;
      case "cityOfBirth":
        demographicInfo.setCityOfBirth(null);
        break;
      case "stateOfBirth":
        demographicInfo.setStateOfBirth(null);
        break;
      case "countryOfBirth":
        demographicInfo.setCountryOfBirth(null);
        break;
      case "citizenship":
        demographicInfo.setCitizenship(null);
        break;
      default:
        // Unknown field name - ignore
        break;
    }
  }

  private void deleteFromFamilyInfo(FamilyInfo familyInfo, String[] parts, int startIndex) {
    if (startIndex >= parts.length) {
      return;
    }
    String field = parts[startIndex];
    switch (field) {
      case "parents":
        familyInfo.setParents(null);
        break;
      case "legalGuardians":
        familyInfo.setLegalGuardians(null);
        break;
      case "maritalStatus":
        familyInfo.setMaritalStatus(null);
        break;
      case "spouse":
        familyInfo.setSpouse(null);
        break;
      case "children":
        familyInfo.setChildren(null);
        break;
      case "siblings":
        familyInfo.setSiblings(null);
        break;
      default:
        // Unknown field name - ignore
        break;
    }
  }
}
