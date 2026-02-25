package Organization.Services;

import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Organization.OrgEnrollmentStatus;
import Organization.Organization;
import User.Address;
import User.User;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import java.util.Objects;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

@Slf4j
public class EnrollOrganizationService implements Service {
  static final String newOrgActualURL = Objects.requireNonNull(System.getenv("NEW_ORG_ACTUALURL"));
  MongoDatabase db;
  ActivityDao activityDao;
  String firstName;
  String lastName;
  String birthDate;
  String email;
  String phone;
  String address;
  String city;
  String state;
  String zipcode;
  Boolean twoFactorOn;
  String username;
  String password;
  UserType userLevel = UserType.Admin;
  String orgName;
  String orgWebsite;
  String orgEIN;
  Address orgAddress;
  String orgEmail;
  String orgPhoneNumber;

  public EnrollOrganizationService(
      MongoDatabase db,
      ActivityDao activityDao,
      String firstName,
      String lastName,
      String birthDate,
      String email,
      String phone,
      String address,
      String city,
      String state,
      String zipcode,
      Boolean twoFactorOn,
      String username,
      String password,
      UserType userType,
      String orgName,
      String orgWebsite,
      String orgEIN,
      Address orgAddress,
      String orgEmail,
      String orgPhoneNumber) {
    this.db = db;
    this.firstName = firstName;
    this.lastName = lastName;
    this.birthDate = birthDate;
    this.email = email;
    this.phone = phone;
    this.address = address;
    this.city = city;
    this.state = state;
    this.zipcode = zipcode;
    this.twoFactorOn = twoFactorOn;
    this.orgName = orgName;
    this.username = username;
    this.password = password;
    this.userLevel = userType;
    this.orgAddress = orgAddress;
    this.orgEIN = orgEIN;
    this.orgEmail = orgEmail;
    this.orgPhoneNumber = orgPhoneNumber;
    this.orgWebsite = orgWebsite;
    this.activityDao = activityDao;
  }

  @Override
  public Message executeAndGetResponse() {
    Organization org;
    User user;

    log.error("Not Allowing for the Creation of New Organizations at this Moment");
    return OrgEnrollmentStatus.FAIL_TO_CREATE;
  }

  private HttpResponse makeBotMessage(Organization org) {
    JSONArray blocks = new JSONArray();
    JSONObject titleJson = new JSONObject();
    JSONObject titleText = new JSONObject();
    titleText.put("text", "*Organization Name: * " + org.getOrgName());
    titleText.put("type", "mrkdwn");
    titleJson.put("type", "section");
    titleJson.put("text", titleText);
    blocks.put(titleJson);
    JSONObject desJson = new JSONObject();
    JSONObject desText = new JSONObject();
    desText.put("text", "*Orgnization Contact: * " + org.getOrgEmail());
    desText.put("type", "mrkdwn");
    desJson.put("text", desText);
    desJson.put("type", "section");
    blocks.put(desJson);
    JSONObject input = new JSONObject();
    input.put("blocks", blocks);

    HttpResponse posted =
        Unirest.post(newOrgActualURL)
            .header("accept", "application/json")
            .body(input.toString())
            .asEmpty();
    return posted;
  }
}
