package Organization.Requests;

import User.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class OrganizationCreateRequest {
  @BsonProperty(value = "orgName")
  private String orgName;

  @BsonProperty(value = "orgWebsite")
  @JsonProperty("orgWebsite")
  private String orgWebsite;

  @BsonProperty(value = "orgEIN")
  @JsonProperty("orgEIN")
  private String orgEIN;

  @BsonProperty(value = "orgAddress")
  @JsonProperty("orgAddress")
  private Address orgAddress;

  @BsonProperty(value = "orgEmail")
  @JsonProperty("orgEmail")
  private String orgEmail;

  @BsonProperty(value = "orgPhoneNumber")
  @JsonProperty("orgPhoneNumber")
  private String orgPhoneNumber;

  public String getOrgName() { return this.orgName; }
  public String getOrgWebsite() { return this.orgWebsite; }
  public String getOrgEIN() { return this.orgEIN; }
  public Address getOrgAddress() { return this.orgAddress; }
  public String getOrgEmail() { return this.orgEmail; }
  public String getOrgPhoneNumber() { return this.orgPhoneNumber; }

  public OrganizationCreateRequest setOrgName(String orgName) { this.orgName = orgName; return this; }
  public OrganizationCreateRequest setOrgWebsite(String website) { this.orgWebsite = website; return this; }
  public OrganizationCreateRequest setOrgEIN(String ein) { this.orgEIN = ein; return this; }
  public OrganizationCreateRequest setOrgAddress(Address address) { this.orgAddress = address; return this; }
  public OrganizationCreateRequest setOrgEmail(String email) { this.orgEmail = email; return this; }
  public OrganizationCreateRequest setOrgPhoneNumber(String phoneNumber) { this.orgPhoneNumber = phoneNumber; return this; }
}
