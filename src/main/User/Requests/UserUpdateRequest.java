package User.Requests;

import User.Address;
import User.Name;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

public class UserUpdateRequest {
  @JsonProperty("currentName")
  private Optional<Name> currentName;

  @JsonProperty("nameHistory")
  private Optional<List<Name>> nameHistory;

  @JsonProperty("birthDate")
  private Optional<String> birthDate;

  @JsonProperty("email")
  private Optional<String> email;

  @JsonProperty("personalAddress")
  private Optional<Address> personalAddress;

  @JsonProperty("mailAddress")
  private Optional<Address> mailAddress;

  @JsonProperty("sex")
  private Optional<String> sex;

  @JsonProperty("motherName")
  private Optional<Name> motherName;

  @JsonProperty("fatherName")
  private Optional<Name> fatherName;

  /** **************** GETTERS ********************* */
  public Optional<Name> getCurrentName() { return currentName; }

  public Optional<List<Name>> getNameHistory() { return nameHistory; }

  public Optional<String> getBirthDate() { return birthDate; }

  public Optional<String> getEmail() { return email; }

  public Optional<Address> getPersonalAddress() { return personalAddress; }

  public Optional<Address> getMailAddress() { return mailAddress; }

  public Optional<String> getSex() { return sex; }

  public Optional<Name> getMotherName() { return motherName; }

  public Optional<Name> getFatherName() { return fatherName; }

  /** **************** SETTERS ********************* */
  public void setCurrentName(Name currentName) { this.currentName = Optional.ofNullable(currentName); }

  public void setNameHistory(List<Name> nameHistory) { this.nameHistory = Optional.ofNullable(nameHistory); }

  public void setBirthDate(String birthDate) { this.birthDate = Optional.ofNullable(birthDate); }

  public void setEmail(String email) { this.email = Optional.ofNullable(email); }

  public void setPersonalAddress(Address personalAddress) { this.personalAddress = Optional.ofNullable(personalAddress); }

  public void setMailAddress(Address mailAddress) { this.mailAddress = Optional.ofNullable(mailAddress); }

  public void setSex(String sex) { this.sex = Optional.ofNullable(sex); }

  public void setMotherName(Name motherName) { this.motherName = Optional.ofNullable(motherName); }

  public void setFatherName(Name fatherName) { this.fatherName = Optional.ofNullable(fatherName); }
}
