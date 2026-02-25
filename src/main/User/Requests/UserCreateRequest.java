package User.Requests;

import User.Address;
import User.Name;
import User.UserType;
import org.bson.codecs.pojo.annotations.BsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;

public class UserCreateRequest {
  @JsonProperty("currentName")
  private Name currentName;

  @BsonProperty(value = "birthDate")
  @JsonProperty("birthDate")
  private String birthDate;

  @BsonProperty(value = "email")
  @JsonProperty("email")
  private String email;

  @BsonProperty(value = "phone")
  @JsonProperty("phone")
  private String phone;

  @JsonProperty("personalAddress")
  private Address personalAddress;

  @BsonProperty(value = "privilegeLevel")
  @JsonProperty("privilegeLevel")
  private UserType userType;

  @BsonProperty(value = "organization")
  private String organization;

  @BsonProperty(value = "username")
  private String username;

  @BsonProperty(value = "password")
  private String password;

  @BsonProperty(value = "defaultIds")
  private HashMap<String, String> defaultIds;

  /** **************** GETTERS ********************* */
  public Name getCurrentName() { return currentName; }

  public String getBirthDate() { return birthDate; }

  public String getEmail() { return email; }

  public String getPhone() { return phone; }

  public Address getPersonalAddress() { return personalAddress; }

  public UserType getUserType() { return userType; }

  public String getOrganization() { return organization; }

  public String getUsername() { return username; }

  public String getPassword() { return password; }

  public HashMap<String, String> getDefaultIds() { return defaultIds; }

  /** **************** SETTERS ********************* */
  public void setCurrentName(Name currentName) { this.currentName = currentName; }

  public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

  public void setEmail(String email) { this.email = email; }

  public void setPhone(String phone) { this.phone = phone; }

  public void setPersonalAddress(Address personalAddress) { this.personalAddress = personalAddress; }

  public void setOrganization(String organization) { this.organization = organization; }

  public void setUserType(UserType userType) { this.userType = userType; }

  public void setUsername(String username) { this.username = username; }

  public void setPassword(String password) { this.password = password; }

  public void setDefaultIds(String documentType, String id) { this.defaultIds.put(documentType, id); }
}
