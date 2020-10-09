package Organization.Services;

import Activity.ActivityController;
import Activity.CreateOrgActivity;
import Bug.BugController;
import Config.Message;
import Config.Service;
import Organization.OrgEnrollmentStatus;
import Organization.Organization;
import Security.SecurityUtils;
import User.IpObject;
import User.User;
import User.UserMessage;
import User.UserType;
import Validation.ValidationException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.mongodb.client.model.Filters.eq;

public class EnrollOrganizationService implements Service {
  static final String newOrgActualURL = Objects.requireNonNull(System.getenv("NEW_ORG_ACTUALURL"));
  MongoDatabase db;
  Logger logger;
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
  String orgStreetAddress;
  String orgCity;
  String orgState;
  String orgZipcode;
  String orgEmail;
  String orgPhoneNumber;
  ActivityController activityController;

  public EnrollOrganizationService(
      MongoDatabase db,
      Logger logger,
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
      String orgStreetAddress,
      String orgCity,
      String orgState,
      String orgZipcode,
      String orgEmail,
      String orgPhoneNumber) {
    this.db = db;
    this.logger = logger;
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
    this.orgCity = orgCity;
    this.orgEIN = orgEIN;
    this.orgEmail = orgEmail;
    this.orgPhoneNumber = orgPhoneNumber;
    this.orgState = orgState;
    this.orgWebsite = orgWebsite;
    this.orgStreetAddress = orgStreetAddress;
    this.orgZipcode = orgZipcode;
    activityController = new ActivityController(db);
  }

  //    // for testing
  //    EnrollOrganizationService(MongoDatabase db, Logger logger, User user, String
  // sessionUsername, UserType sessionUserLevel){
  //        this.firstName = user.getFirstName();
  //        this.lastName = user.getLastName();
  //        this.birthDate = user.getBirthDate();
  //        this.email = user.getEmail();
  //        this.phone = user.getPhone();
  //        this.address = user.getAddress();
  //        this.city = user.getCity();
  //        this.state = user.getState();
  //        this.zipcode = user.getZipcode();
  //        this.twoFactorOn = user.getTwoFactorOn();
  //        this.username = user.getUsername();
  //        this.password = user.getPassword();
  //        this.userLevel = user.getUserType();
  //    }

  @Override
  public Message executeAndGetResponse() {
    Organization org;
    User user;

    logger.info("Attempting to create user and organization");
    try {
      org =
          new Organization(
              orgName,
              orgWebsite,
              orgEIN,
              orgStreetAddress,
              orgCity,
              orgState,
              orgZipcode,
              orgEmail,
              orgPhoneNumber);
      user =
          new User(
              firstName,
              lastName,
              birthDate,
              email,
              phone,
              orgName,
              address,
              city,
              state,
              zipcode,
              twoFactorOn,
              username,
              password,
              userLevel);
      CreateOrgActivity createOrgActivity = new CreateOrgActivity(user, org);
      activityController.addActivity(createOrgActivity);
    } catch (ValidationException ve) {
      logger.error("Could not create user and/or org");
      return OrgEnrollmentStatus.FAIL_TO_CREATE;
    }

    logger.info("Checking for existing user and organization");
    MongoCollection<Organization> orgCollection =
        db.getCollection("organization", Organization.class);
    Organization existingOrg = orgCollection.find(eq("orgName", org.getOrgName())).first();

    MongoCollection<User> userCollection = db.getCollection("user", User.class);
    User existingUser = userCollection.find(eq("username", user.getUsername())).first();

    if (existingOrg != null) {
      logger.error("Organization already exists");
      return OrgEnrollmentStatus.ORG_EXISTS;
    } else if (existingUser != null) {
      logger.error("User already exists");
      return UserMessage.USERNAME_ALREADY_EXISTS;
    } else {
      logger.info("Org and User are OK, hashing password");
      String passwordHash = SecurityUtils.hashPassword(password);
      if (passwordHash == null) {
        return OrgEnrollmentStatus.PASS_HASH_FAILURE;
      }

      logger.info("Setting password and inserting user and org into Mongo");
      user.setPassword(passwordHash);

      List<IpObject> logInInfo = new ArrayList<IpObject>(1000);
      user.setLogInHistory(logInInfo);
      userCollection.insertOne(user);
      orgCollection.insertOne(org);
      logger.info("Notifying Slack about new org");
      HttpResponse posted = makeBotMessage(org);
      if (!posted.isSuccess()) {
        logger.error("Failed to notify Slack about new org");
        JSONObject body = new JSONObject();
        body.put(
            "text",
            "You are receiving this because an new organization signed up but wasn't successfully "
                + "posted on Slack.");
        Unirest.post(BugController.bugReportActualURL).body(body.toString()).asEmpty();
      }
      logger.info("Done with enrollOrganization");
      return OrgEnrollmentStatus.SUCCESSFUL_ENROLLMENT;
    }
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