package User;

import User.Requests.UserUpdateRequest;
import User.Services.DocumentType;
import User.UserValidationMessage;
import Validation.ValidationException;
import Validation.ValidationUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import User.IpObject;
import User.UserType;
import org.json.JSONObject;

import java.util.*;

@Slf4j
@Setter
public class User {
  private ObjectId id;

  @BsonProperty(value = "firstName")
  private String firstName;

  @BsonProperty(value = "lastName")
  private String lastName;

  @BsonProperty(value = "email")
  private String email;

  @BsonProperty(value = "organization")
  private String organization;

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

  public User(
          String firstName,
          String lastName,
          String email,
          String organization,
          Boolean twoFactorOn,
          String username,
          String password,
          UserType userType)
          throws ValidationException {

    UserValidationMessage validationMessage =
            User.isValid(
                    firstName,
                    lastName,
                    email,
                    organization,
                    username,
                    password,
                    userType);

    if (validationMessage != UserValidationMessage.VALID)
      throw new ValidationException(UserValidationMessage.toUserMessageJSON(validationMessage));

    Date date = new Date();

    this.id = new ObjectId();
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.organization = organization;
    this.twoFactorOn = twoFactorOn;
    this.username = username;
    this.password = password;
    this.userType = userType;
    this.creationDate = date;
    this.defaultIds = new HashMap<>();
    this.assignedWorkerUsernames = new ArrayList<>();
  }

  /** **************** GETTERS ********************* */
  public User getSelf() {return this;}

  public ObjectId getId() {
    return this.id;
  }

  public String getFirstName() {
    return this.firstName;
  }

  public String getLastName() {
    return this.lastName;
  }

  public String getEmail() {
    return this.email;
  }

  public String getOrganization() {
    return this.organization;
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
  ;

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

  /** *************** SETTERS ********************* */
  public User setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public User setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }


  public User setEmail(String email) {
    this.email = email;
    return this;
  }

  public User setOrganization(String organization) {
    this.organization = organization;
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

  private static UserValidationMessage isValid(
          String firstName,
          String lastName,
          String email,
          String organization,
          String username,
          String password,
          UserType userType) {

    if (!ValidationUtils.isValidFirstName(firstName)) {
      log.error("Invalid firstName: " + firstName);
      return UserValidationMessage.INVALID_FIRSTNAME;
    }
    if (!ValidationUtils.isValidLastName(lastName)) {
      log.error("Invalid lastName: " + lastName);
      return UserValidationMessage.INVALID_LASTNAME;
    }
    if (!ValidationUtils.isValidOrganizationName(organization)) {
      log.error("Invalid organization: " + organization);
      return UserValidationMessage.INVALID_ORGANIZATION;
    }
    if (ValidationUtils.hasValue(email) && !ValidationUtils.isValidEmail(email)) {
      log.error("Invalid email: " + email);
      return UserValidationMessage.INVALID_EMAIL;
    }
    if (!ValidationUtils.isValidUsername(username)) {
      log.error("Invalid username: " + username);
      return UserValidationMessage.INVALID_USERNAME;
    }
    if (!ValidationUtils.isValidPassword(password)) {
      log.error("Invalid password: " + password);
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
    sb.append("id=").append(this.id.toHexString());
    sb.append(", firstName=").append(this.firstName);
    sb.append(", lastName=").append(this.lastName);
    sb.append(", email=").append(this.email);
    sb.append(", username=").append(this.username);
    sb.append(", password=").append(this.password);
    sb.append(", defaultIds=").append(this.defaultIds.toString());
    sb.append(", userType=").append(this.userType);
    sb.append(", twoFactorOn=").append(this.twoFactorOn);
    sb.append(", creationDate=").append(this.creationDate);
    sb.append(", assignedWorkerUsernames=").append(this.assignedWorkerUsernames.toString());
    sb.append("}");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(this.id, user.id)
            && Objects.equals(this.firstName, user.firstName)
            && Objects.equals(this.lastName, user.lastName)
            && Objects.equals(this.email, user.email)
            && Objects.equals(this.username, user.username)
            && Objects.equals(this.password, user.password)
            && Objects.equals(this.defaultIds, user.defaultIds)
            && Objects.equals(this.userType, user.userType)
            && Objects.equals(this.twoFactorOn, user.twoFactorOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
            this.id,
            this.firstName,
            this.lastName,
            this.email,
            this.username,
            this.password,
            this.defaultIds,
            this.userType,
            this.twoFactorOn,
            this.assignedWorkerUsernames);
  }

  public JSONObject serialize() {
    JSONObject userJSON = new JSONObject();
    userJSON.put("username", username);
    userJSON.put("privilegeLevel", userType);
    userJSON.put("userType", userType);
    userJSON.put("firstName", firstName);
    userJSON.put("lastName", lastName);
    userJSON.put("email", email);
    userJSON.put("organization", organization);
    userJSON.put("logInHistory", logInHistory);
    userJSON.put("creationDate", creationDate);
    userJSON.put("twoFactorOn", twoFactorOn);
    userJSON.put("defaultIds", defaultIds);
    userJSON.put("assignedWorkerUsernames", assignedWorkerUsernames);
    return userJSON;
  }

  public Map<String, Object> toMap() {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> result =
            objectMapper.convertValue(this, new TypeReference<Map<String, Object>>() {});
    result.remove("id");
    return result;
  }

  public User updateProperties(UserUpdateRequest updateRequest) {
    if (updateRequest.getFirstName() != null && updateRequest.getFirstName().isPresent()) {
      this.setFirstName(updateRequest.getFirstName().get());
    }

    if (updateRequest.getLastName() != null && updateRequest.getLastName().isPresent()) {
      this.setLastName(updateRequest.getLastName().get());
    }

    if (updateRequest.getEmail() != null && updateRequest.getEmail().isPresent()) {
      this.setEmail(updateRequest.getEmail().get());
    }

    return this;
  }

}
