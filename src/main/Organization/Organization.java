package Organization;

import Organization.Requests.OrganizationUpdateRequest;
import User.Address;
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

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Setter
public class Organization implements Serializable {
  private ObjectId id;

  @BsonProperty(value = "orgName")
  private String orgName;

  @BsonProperty(value = "website")
  @JsonProperty("website")
  private String orgWebsite;

  @BsonProperty(value = "ein")
  @JsonProperty("ein")
  private String orgEIN;

  @BsonProperty(value = "address")
  @JsonProperty("address")
  private Address orgAddress;

  @BsonProperty(value = "email")
  @JsonProperty("email")
  private String orgEmail;

  @BsonProperty(value = "phone")
  @JsonProperty("phone")
  private String orgPhoneNumber;

  @BsonProperty(value = "creationDate")
  private Date creationDate;

  public Organization() {}

  public Organization(
      String orgName,
      String orgWebsite,
      String orgEIN,
      Address orgAddress,
      String orgEmail,
      String orgPhoneNumber)
      throws ValidationException {

    OrganizationValidationMessage ovm =
        Organization.isValid(orgName, orgWebsite, orgEIN, orgAddress, orgEmail, orgPhoneNumber);

    if (ovm != OrganizationValidationMessage.VALID)
      throw new ValidationException(OrganizationValidationMessage.toOrganizationMessageJSON(ovm));

    this.id = new ObjectId();
    this.orgName = orgName;
    this.orgWebsite = orgWebsite;
    this.orgEIN = orgEIN;
    this.orgAddress = orgAddress;
    this.orgEmail = orgEmail;
    this.orgPhoneNumber = orgPhoneNumber;
    this.creationDate = new Date();
  }

  public ObjectId getId() { return this.id; }
  public String getOrgName() { return this.orgName; }
  public String getOrgWebsite() { return this.orgWebsite; }
  public String getOrgEIN() { return this.orgEIN; }
  public Address getOrgAddress() { return this.orgAddress; }
  public String getOrgEmail() { return this.orgEmail; }
  public String getOrgPhoneNumber() { return this.orgPhoneNumber; }
  public Date getCreationDate() { return this.creationDate; }

  public Organization setOrgName(String orgName) { this.orgName = orgName; return this; }
  public Organization setOrgWebsite(String website) { this.orgWebsite = website; return this; }
  public Organization setOrgEIN(String ein) { this.orgEIN = ein; return this; }
  public Organization setOrgAddress(Address address) { this.orgAddress = address; return this; }
  public Organization setOrgEmail(String email) { this.orgEmail = email; return this; }
  public Organization setOrgPhoneNumber(String phoneNumber) { this.orgPhoneNumber = phoneNumber; return this; }
  public Organization setCreationDate(Date creationDate) { this.creationDate = creationDate; return this; }

  private static OrganizationValidationMessage isValid(
      String orgName,
      String orgWebsite,
      String orgEIN,
      Address orgAddress,
      String orgEmail,
      String orgPhoneNumber) {

    if (!ValidationUtils.isValidOrgName(orgName)) {
      log.error("Invalid orgname: " + orgName);
      return OrganizationValidationMessage.INVALID_NAME;
    }
    if (!ValidationUtils.isValidOrgWebsite(orgWebsite)) {
      log.error("Invalid website: " + orgWebsite);
      return OrganizationValidationMessage.INVALID_WEBSITE;
    }
    if (!ValidationUtils.isValidEIN(orgEIN)) {
      log.error("Invalid taxCode: " + orgEIN);
      return OrganizationValidationMessage.INVALID_EIN;
    }
    if (!ValidationUtils.isValidPhoneNumber(orgPhoneNumber)) {
      log.error("Invalid orgContactPhoneNumber: " + orgPhoneNumber);
      return OrganizationValidationMessage.INVALID_PHONE;
    }
    if (!ValidationUtils.isValidEmail(orgEmail)) {
      log.error("Invalid email: " + orgEmail);
      return OrganizationValidationMessage.INVALID_EMAIL;
    }
    if (orgAddress == null) {
      log.error("Null address");
      return OrganizationValidationMessage.INVALID_ADDRESS;
    }
    if (!ValidationUtils.isValidAddress(orgAddress.getLine1())) {
      log.error("Invalid address: " + orgAddress.getLine1());
      return OrganizationValidationMessage.INVALID_ADDRESS;
    }
    if (!ValidationUtils.isValidCity(orgAddress.getCity())) {
      log.error("Invalid city: " + orgAddress.getCity());
      return OrganizationValidationMessage.INVALID_CITY;
    }
    if (!ValidationUtils.isValidUSState(orgAddress.getState())) {
      log.error("Invalid state: " + orgAddress.getState());
      return OrganizationValidationMessage.INVALID_STATE;
    }
    if (!ValidationUtils.isValidZipCode(orgAddress.getZip())) {
      log.error("Invalid zipcode: " + orgAddress.getZip());
      return OrganizationValidationMessage.INVALID_ZIPCODE;
    }
    return OrganizationValidationMessage.VALID;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Organization {");
    sb.append("id=").append(this.id.toHexString());
    sb.append(", orgName=").append(this.orgName);
    sb.append(", orgWebsite=").append(this.orgWebsite);
    sb.append(", orgEIN=").append(this.orgEIN);
    sb.append(", orgAddress=").append(this.orgAddress);
    sb.append(", orgEmail=").append(this.orgEmail);
    sb.append(", orgPhoneNumber=").append(this.orgPhoneNumber);
    sb.append("}");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Organization org = (Organization) o;
    return Objects.equals(this.id, org.id)
        && Objects.equals(this.orgName, org.orgName)
        && Objects.equals(this.orgWebsite, org.orgWebsite)
        && Objects.equals(this.orgEIN, org.orgEIN)
        && Objects.equals(this.orgAddress, org.orgAddress)
        && Objects.equals(this.orgEmail, org.orgEmail)
        && Objects.equals(this.orgPhoneNumber, org.orgPhoneNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.id, this.orgName, this.orgWebsite, this.orgEIN,
        this.orgAddress, this.orgEmail, this.orgPhoneNumber);
  }

  public Map<String, Object> toMap() {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> result =
        objectMapper.convertValue(this, new TypeReference<Map<String, Object>>() {});
    result.remove("id");
    return result;
  }

  public JSONObject serialize() {
    JSONObject orgJSON = new JSONObject();
    orgJSON.put("orgName", orgName);
    orgJSON.put("orgWebsite", orgWebsite);
    orgJSON.put("orgEIN", orgEIN);
    orgJSON.put("orgAddress", orgAddress != null ? orgAddress.serialize() : JSONObject.NULL);
    orgJSON.put("orgEmail", orgEmail);
    orgJSON.put("orgPhoneNumber", orgPhoneNumber);
    orgJSON.put("creationDate", creationDate);
    return orgJSON;
  }

  public Organization updateProperties(OrganizationUpdateRequest updateRequest) {
    if (updateRequest.getOrgName() != null && updateRequest.getOrgName().isPresent()) {
      this.setOrgName(updateRequest.getOrgName().get());
    }
    if (updateRequest.getOrgWebsite() != null && updateRequest.getOrgWebsite().isPresent()) {
      this.setOrgWebsite(updateRequest.getOrgWebsite().get());
    }
    if (updateRequest.getOrgEIN() != null && updateRequest.getOrgEIN().isPresent()) {
      this.setOrgEIN(updateRequest.getOrgEIN().get());
    }
    if (updateRequest.getOrgAddress() != null) {
      this.setOrgAddress(updateRequest.getOrgAddress());
    }
    if (updateRequest.getOrgEmail() != null && updateRequest.getOrgEmail().isPresent()) {
      this.setOrgEmail(updateRequest.getOrgEmail().get());
    }
    if (updateRequest.getOrgPhoneNumber() != null && updateRequest.getOrgPhoneNumber().isPresent()) {
      this.setOrgPhoneNumber(updateRequest.getOrgPhoneNumber().get());
    }
    return this;
  }
}
