package User;

import User.Onboarding.OnboardingStatus;
import User.Services.DocumentType;
import Validation.ValidationException;
import Validation.ValidationUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.*;

@Slf4j
@Setter
public class User {
  private ObjectId id;

  @BsonProperty(value = "currentName")
  private Name currentName;

  @BsonProperty(value = "nameHistory")
  private List<Name> nameHistory;

  @BsonProperty(value = "birthDate")
  private String birthDate;

  @BsonProperty(value = "email")
  private String email;

  @BsonProperty(value = "organization")
  private String organization;

  @BsonProperty(value = "personalAddress")
  private Address personalAddress;

  @BsonProperty(value = "mailAddress")
  private Address mailAddress;

  @BsonProperty(value = "username")
  private String username;

  @BsonProperty(value = "password")
  private String password;

  @BsonProperty(value = "privilegeLevel")
  @JsonProperty("privilegeLevel")
  private UserType userType;

  @BsonProperty(value = "twoFactorOn")
  private boolean twoFactorOn;

  @BsonProperty(value = "creationDate")
  private Date creationDate;

  @BsonProperty(value = "logInHistory")
  private List<IpObject> logInHistory;

  @BsonProperty(value = "defaultIds")
  private Map<String, String> defaultIds;

  @BsonProperty(value = "assignedWorkerUsernames")
  private List<String> assignedWorkerUsernames;

  @BsonProperty(value = "onboardingStatus")
  private OnboardingStatus onboardingStatus;

  @BsonProperty(value = "phoneBook")
  private List<PhoneBookEntry> phoneBook;

  @BsonProperty(value = "sex")
  private String sex;

  @BsonProperty(value = "motherName")
  private Name motherName;

  @BsonProperty(value = "fatherName")
  private Name fatherName;

  public User() {
  }

  public User(
      Name currentName,
      String birthDate,
      String email,
      String phone,
      String organization,
      Address personalAddress,
      Boolean twoFactorOn,
      String username,
      String password,
      UserType userType)
      throws ValidationException {

    UserValidationMessage validationMessage = isValid(
        currentName,
        birthDate,
        email,
        phone,
        organization,
        personalAddress,
        username,
        password,
        userType);

    if (validationMessage != UserValidationMessage.VALID)
      throw new ValidationException(UserValidationMessage.toUserMessageJSON(validationMessage));

    Date date = new Date();

    this.id = new ObjectId();
    this.currentName = currentName;
    this.birthDate = birthDate;
    this.email = email;
    this.organization = organization;
    this.personalAddress = personalAddress;
    this.twoFactorOn = twoFactorOn;
    this.username = username;
    this.password = password;
    this.userType = userType;
    this.creationDate = date;
    this.defaultIds = new HashMap<>();
    this.assignedWorkerUsernames = new ArrayList<>();
    this.phoneBook = new ArrayList<>();
    if (phone != null && !phone.isBlank()) {
      this.phoneBook.add(new PhoneBookEntry(PhoneBookEntry.PRIMARY_LABEL, phone));
    }
  }

  /** **************** GETTERS ********************* */
  public ObjectId getId() {
    return this.id;
  }

  public Name getCurrentName() {
    return this.currentName;
  }

  public String getFirstName() {
    return currentName != null ? currentName.getFirst() : null;
  }

  public String getLastName() {
    return currentName != null ? currentName.getLast() : null;
  }

  public List<Name> getNameHistory() {
    return this.nameHistory;
  }

  public String getBirthDate() {
    return this.birthDate;
  }

  public String getEmail() {
    return this.email;
  }

  /**
   * Returns the primary phone number by finding the phoneBook entry labeled "primary".
   */
  public String getPhone() {
    if (phoneBook != null) {
      for (PhoneBookEntry entry : phoneBook) {
        if (entry.hasPrimaryLabel()) {
          return entry.getPhoneNumber();
        }
      }
    }
    return null;
  }

  public String getOrganization() {
    return this.organization;
  }

  public Address getPersonalAddress() {
    return this.personalAddress;
  }

  public Address getMailAddress() {
    return this.mailAddress;
  }

  public String getUsername() {
    return this.username;
  }

  public String getPassword() {
    return this.password;
  }

  public Map<String, String> getDefaultIds() {
    return this.defaultIds;
  }

  public UserType getUserType() {
    return this.userType;
  }

  public boolean getTwoFactorOn() {
    return this.twoFactorOn;
  }

  public Date getCreationDate() {
    return this.creationDate;
  }

  public List<IpObject> getLogInHistory() {
    return this.logInHistory;
  }

  public List<String> getAssignedWorkerUsernames() {
    return this.assignedWorkerUsernames;
  }

  public OnboardingStatus getOnboardingStatus() {
    return this.onboardingStatus;
  }

  public List<PhoneBookEntry> getPhoneBook() {
    return this.phoneBook;
  }

  public String getSex() {
    return this.sex;
  }

  public Name getMotherName() {
    return this.motherName;
  }

  public Name getFatherName() {
    return this.fatherName;
  }

  /** *************** SETTERS ********************* */
  public User setCurrentName(Name currentName) {
    this.currentName = currentName;
    return this;
  }

  public User setNameHistory(List<Name> nameHistory) {
    this.nameHistory = nameHistory;
    return this;
  }

  public User setBirthDate(String birthDate) {
    this.birthDate = birthDate;
    return this;
  }

  public User setEmail(String email) {
    this.email = email;
    return this;
  }

  /**
   * Sets the primary phone number by updating the "primary" labeled phoneBook entry.
   * Creates one if none exists.
   */
  public User setPhone(String phone) {
    if (phoneBook != null) {
      for (PhoneBookEntry entry : phoneBook) {
        if (entry.hasPrimaryLabel()) {
          entry.setPhoneNumber(phone);
          return this;
        }
      }
      if (phone != null && !phone.isBlank()) {
        phoneBook.add(new PhoneBookEntry(PhoneBookEntry.PRIMARY_LABEL, phone));
      }
    }
    return this;
  }

  public User setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public User setPersonalAddress(Address personalAddress) {
    this.personalAddress = personalAddress;
    return this;
  }

  public User setMailAddress(Address mailAddress) {
    this.mailAddress = mailAddress;
    return this;
  }

  public User setCreationDate(Date date) {
    this.creationDate = date;
    return this;
  }

  public User setTwoFactorOn(Boolean twoFactorOn) {
    this.twoFactorOn = twoFactorOn;
    return this;
  }

  public User setUsername(String username) {
    this.username = username;
    return this;
  }

  public User setPassword(String password) {
    this.password = password;
    return this;
  }

  public User setDefaultId(DocumentType documentType, String id) {
    this.defaultIds.put(DocumentType.stringFromDocumentType(documentType), id);
    return this;
  }

  public User setUserType(UserType userType) {
    this.userType = userType;
    return this;
  }

  public User setLogInHistory(List<IpObject> logInHistory) {
    this.logInHistory = logInHistory;
    return this;
  }

  public User setAssignedWorkerUsernames(List<String> assignedWorkerUsernames) {
    this.assignedWorkerUsernames = assignedWorkerUsernames;
    return this;
  }

  public User addAssignedWorker(String assignedWorkerUsername) {
    this.assignedWorkerUsernames.add(assignedWorkerUsername);
    return this;
  }

  public User setOnboardingStatus(OnboardingStatus onboardingStatus) {
    this.onboardingStatus = onboardingStatus;
    return this;
  }

  public User setPhoneBook(List<PhoneBookEntry> phoneBook) {
    this.phoneBook = phoneBook;
    return this;
  }

  public User setSex(String sex) {
    this.sex = sex;
    return this;
  }

  public User setMotherName(Name motherName) {
    this.motherName = motherName;
    return this;
  }

  public User setFatherName(Name fatherName) {
    this.fatherName = fatherName;
    return this;
  }

  private static UserValidationMessage isValid(
      Name currentName,
      String birthDate,
      String email,
      String phone,
      String organization,
      Address personalAddress,
      String username,
      String password,
      UserType userType) {

    if (currentName == null || !ValidationUtils.isValidFirstName(currentName.getFirst())) {
      log.error("Invalid firstName");
      return UserValidationMessage.INVALID_FIRSTNAME;
    }
    if (!ValidationUtils.isValidLastName(currentName.getLast())) {
      log.error("Invalid lastName");
      return UserValidationMessage.INVALID_LASTNAME;
    }
    if (!ValidationUtils.isValidBirthDate(birthDate)) {
      log.error("Invalid birthDate: " + birthDate);
      return UserValidationMessage.INVALID_BIRTHDATE;
    }
    if (ValidationUtils.hasValue(phone) && !ValidationUtils.isValidPhoneNumber(phone)) {
      log.error("Invalid phone: " + phone);
      return UserValidationMessage.INVALID_PHONENUMBER;
    }
    boolean requiresOrganization = userType != UserType.Developer;
    if (requiresOrganization && !ValidationUtils.isValidOrganizationName(organization)) {
      log.error("Invalid organization: " + organization);
      return UserValidationMessage.INVALID_ORGANIZATION;
    }
    if (ValidationUtils.hasValue(email) && !ValidationUtils.isValidEmail(email)) {
      log.error("Invalid email: " + email);
      return UserValidationMessage.INVALID_EMAIL;
    }
    if (personalAddress != null) {
      if (ValidationUtils.hasValue(personalAddress.getLine1())
          && !ValidationUtils.isValidAddress(personalAddress.getLine1())) {
        log.error("Invalid address line1");
        return UserValidationMessage.INVALID_ADDRESS;
      }
      if (ValidationUtils.hasValue(personalAddress.getCity())
          && !ValidationUtils.isValidCity(personalAddress.getCity())) {
        log.error("Invalid city");
        return UserValidationMessage.INVALID_CITY;
      }
      if (ValidationUtils.hasValue(personalAddress.getState())
          && !ValidationUtils.isValidUSState(personalAddress.getState())) {
        log.error("Invalid state");
        return UserValidationMessage.INVALID_STATE;
      }
      if (ValidationUtils.hasValue(personalAddress.getZip())
          && !ValidationUtils.isValidZipCode(personalAddress.getZip())) {
        log.error("Invalid zipcode");
        return UserValidationMessage.INVALID_ZIPCODE;
      }
    }
    if (!ValidationUtils.isValidUsername(username)) {
      log.error("Invalid username: " + username);
      return UserValidationMessage.INVALID_USERNAME;
    }
    if (!ValidationUtils.isValidPassword(password)) {
      log.error("Invalid password");
      return UserValidationMessage.INVALID_PASSWORD;
    }
    if (!ValidationUtils.isValidUserType(userType.toString())) {
      log.error("Invalid UserType: " + userType);
      return UserValidationMessage.INVALID_USERTYPE;
    }

    return UserValidationMessage.VALID;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("User {");
    sb.append("id=").append(this.id != null ? this.id.toHexString() : "null");
    sb.append(", currentName=").append(this.currentName);
    sb.append(", birthDate=").append(this.birthDate);
    sb.append(", email=").append(this.email);
    sb.append(", phone=").append(this.getPhone());
    sb.append(", personalAddress=").append(this.personalAddress);
    sb.append(", mailAddress=").append(this.mailAddress);
    sb.append(", username=").append(this.username);
    sb.append(", password=").append(this.password);
    sb.append(", defaultIds=").append(this.defaultIds != null ? this.defaultIds.toString() : "null");
    sb.append(", userType=").append(this.userType);
    sb.append(", twoFactorOn=").append(this.twoFactorOn);
    sb.append(", creationDate=").append(this.creationDate);
    sb.append(", assignedWorkerUsernames=").append(this.assignedWorkerUsernames != null ? this.assignedWorkerUsernames.toString() : "null");
    sb.append(", phoneBook=").append(this.phoneBook != null ? this.phoneBook.toString() : "null");
    sb.append(", sex=").append(this.sex);
    sb.append("}");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    User user = (User) o;
    return Objects.equals(this.id, user.id)
        && Objects.equals(this.currentName, user.currentName)
        && Objects.equals(this.birthDate, user.birthDate)
        && Objects.equals(this.email, user.email)
        && Objects.equals(this.getPhone(), user.getPhone())
        && Objects.equals(this.personalAddress, user.personalAddress)
        && Objects.equals(this.mailAddress, user.mailAddress)
        && Objects.equals(this.username, user.username)
        && Objects.equals(this.password, user.password)
        && Objects.equals(this.defaultIds, user.defaultIds)
        && Objects.equals(this.userType, user.userType)
        && Objects.equals(this.twoFactorOn, user.twoFactorOn)
        && Objects.equals(this.phoneBook, user.phoneBook)
        && Objects.equals(this.sex, user.sex)
        && Objects.equals(this.motherName, user.motherName)
        && Objects.equals(this.fatherName, user.fatherName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.id,
        this.currentName,
        this.birthDate,
        this.email,
        this.getPhone(),
        this.personalAddress,
        this.mailAddress,
        this.username,
        this.password,
        this.defaultIds,
        this.userType,
        this.twoFactorOn,
        this.assignedWorkerUsernames,
        this.phoneBook,
        this.sex,
        this.motherName,
        this.fatherName);
  }

  public JSONObject serialize() {
    JSONObject userJSON = new JSONObject();
    userJSON.put("username", username);
    userJSON.put("birthDate", birthDate);
    userJSON.put("privilegeLevel", userType);
    userJSON.put("userType", userType);
    userJSON.put("email", email);
    userJSON.put("phone", getPhone());
    userJSON.put("organization", organization);
    userJSON.put("logInHistory", logInHistory);
    userJSON.put("creationDate", creationDate);
    userJSON.put("twoFactorOn", twoFactorOn);
    userJSON.put("defaultIds", defaultIds);
    userJSON.put("assignedWorkerUsernames", assignedWorkerUsernames);
    userJSON.put("sex", sex);

    if (currentName != null) {
      userJSON.put("currentName", currentName.serialize());
      userJSON.put("firstName", currentName.getFirst());
      userJSON.put("lastName", currentName.getLast());
    }
    if (nameHistory != null) {
      org.json.JSONArray nameHistoryArray = new org.json.JSONArray();
      for (Name name : nameHistory) {
        nameHistoryArray.put(name.serialize());
      }
      userJSON.put("nameHistory", nameHistoryArray);
    }
    if (personalAddress != null) {
      userJSON.put("personalAddress", personalAddress.serialize());
    }
    if (mailAddress != null) {
      userJSON.put("mailAddress", mailAddress.serialize());
    }
    if (motherName != null) {
      userJSON.put("motherName", motherName.serialize());
    }
    if (fatherName != null) {
      userJSON.put("fatherName", fatherName.serialize());
    }
    if (phoneBook != null && !phoneBook.isEmpty()) {
      org.json.JSONArray phoneBookArray = new org.json.JSONArray();
      for (PhoneBookEntry entry : phoneBook) {
        JSONObject entryJSON = new JSONObject();
        entryJSON.put("label", entry.getLabel());
        entryJSON.put("phoneNumber", entry.getPhoneNumber());
        phoneBookArray.put(entryJSON);
      }
      userJSON.put("phoneBook", phoneBookArray);
    }
    return userJSON;
  }

  public Map<String, Object> toMap() {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> result = objectMapper.convertValue(this, new TypeReference<Map<String, Object>>() {
    });
    result.remove("id");
    return result;
  }
}
