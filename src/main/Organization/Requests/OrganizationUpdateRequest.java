package Organization.Requests;

import User.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.Optional;

public class OrganizationUpdateRequest {
  @BsonProperty(value = "orgName")
  private Optional<String> orgName;

  @BsonProperty(value = "orgWebsite")
  @JsonProperty("orgWebsite")
  private Optional<String> orgWebsite;

  @BsonProperty(value = "orgEIN")
  @JsonProperty("orgEIN")
  private Optional<String> orgEIN;

  @BsonProperty(value = "orgAddress")
  @JsonProperty("orgAddress")
  private Address orgAddress;

  @BsonProperty(value = "orgEmail")
  @JsonProperty("orgEmail")
  private Optional<String> orgEmail;

  @BsonProperty(value = "orgPhoneNumber")
  @JsonProperty("orgPhoneNumber")
  private Optional<String> orgPhoneNumber;

  public Optional<String> getOrgName() { return this.orgName; }
  public Optional<String> getOrgWebsite() { return this.orgWebsite; }
  public Optional<String> getOrgEIN() { return this.orgEIN; }
  public Address getOrgAddress() { return this.orgAddress; }
  public Optional<String> getOrgEmail() { return this.orgEmail; }
  public Optional<String> getOrgPhoneNumber() { return this.orgPhoneNumber; }

  public OrganizationUpdateRequest setOrgName(String orgName) { this.orgName = Optional.ofNullable(orgName); return this; }
  public OrganizationUpdateRequest setOrgWebsite(String website) { this.orgWebsite = Optional.ofNullable(website); return this; }
  public OrganizationUpdateRequest setOrgEIN(String ein) { this.orgEIN = Optional.ofNullable(ein); return this; }
  public OrganizationUpdateRequest setOrgAddress(Address address) { this.orgAddress = address; return this; }
  public OrganizationUpdateRequest setOrgEmail(String email) { this.orgEmail = Optional.ofNullable(email); return this; }
  public OrganizationUpdateRequest setOrgPhoneNumber(String phoneNumber) { this.orgPhoneNumber = Optional.ofNullable(phoneNumber); return this; }
}
