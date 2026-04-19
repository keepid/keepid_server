package Organization;

import Config.Message;
import Database.Activity.ActivityDao;
import Organization.Services.EnrollOrganizationService;
import Organization.Services.FindMemberService;
import Organization.Services.InviteUserService;
import Organization.Services.ListOrgsService;
import Security.EmailSender;
import Security.EmailSenderFactory;
import Security.EncryptionUtils;
import User.Address;
import User.UserType;
import Validation.ValidationException;
import Validation.ValidationUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class OrganizationController {

  MongoDatabase db;
  ActivityDao activityDao;
  EmailSender emailSender;

  public static final String newOrgTestURL =
      Objects.requireNonNull(System.getenv("NEW_ORG_TESTURL"));
  public static final String newOrgActualURL =
      Objects.requireNonNull(System.getenv("NEW_ORG_ACTUALURL"));
  EncryptionUtils encryptionUtils;

  public OrganizationController(MongoDatabase db, ActivityDao activityDao) {
    this(db, activityDao, EmailSenderFactory.smtp());
  }

  public OrganizationController(MongoDatabase db, ActivityDao activityDao, EmailSender emailSender) {
    this.db = db;
    this.encryptionUtils = EncryptionUtils.getInstance();
    this.activityDao = activityDao;
    this.emailSender = emailSender;
  }
  
  public Handler organizationSignupValidator =
      ctx -> {
        log.info("Starting organizationSignupValidator");
        JSONObject req = new JSONObject(ctx.body());

        String orgName = req.getString("organizationName").strip();
        String orgWebsite = req.getString("organizationWebsite").toLowerCase().strip();
        String orgEIN = req.getString("organizationEIN").strip();
        String orgStreetAddress = req.getString("organizationAddressStreet").toUpperCase().strip();
        String orgCity = req.getString("organizationAddressCity").toUpperCase().strip();
        String orgState = req.getString("organizationAddressState").toUpperCase().strip();
        String orgZipcode = req.getString("organizationAddressZipcode").strip();
        String orgEmail = req.getString("organizationEmail").strip();
        String orgPhoneNumber = req.getString("organizationPhoneNumber").strip();

        log.info("Checking for existing organizations");
        MongoCollection<Organization> orgCollection = db.getCollection("organization", Organization.class);
        Organization existingOrg = orgCollection.find(eq("orgName", orgName)).first();

        if (existingOrg != null) {
          log.error("Attempted to sign-up org that already exists");
          ctx.result(OrgEnrollmentStatus.ORG_EXISTS.toJSON().toString());
          return;
        }

        log.info("Trying to create organization");
        try {
          new Organization(
              orgName,
              orgWebsite,
              orgEIN,
              new Address(orgStreetAddress, orgCity, orgState, orgZipcode),
              orgEmail,
              orgPhoneNumber);
          ctx.result(
              OrganizationValidationMessage.toOrganizationMessageJSON(OrganizationValidationMessage.VALID)
                  .toString());
          log.info("Organization created");
        } catch (ValidationException ve) {
          log.error("Couldn't create organization object");
          ctx.result(ve.getJSON().toString());
        }
        log.info("Done with organizationSignupValidator");
      };

  // Takes in a json object specifying
  // s and orgnames
  //
  //  {userTypes : [],
  //  organizations : []}
  public Handler findMembersOfOrgs =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        JSONArray userTypes = req.getJSONArray("userTypes");
        JSONArray orgs = req.getJSONArray("organizations");
        FindMemberService fm = new FindMemberService(db, userTypes, orgs);
        Message message = fm.executeAndGetResponse();
        JSONObject res = fm.getMembers();
        if (res == null) {
          res = new JSONObject();
        }
        res.put("status", message.getErrorName());
        res.put("message", message.getErrorDescription());
        ctx.result(res.toString());
      };

  public Handler listOrgs =
      ctx -> {
        ListOrgsService loservice = new ListOrgsService(db);
        Message message = loservice.executeAndGetResponse();
        JSONArray orgs = loservice.getOrgs();
        JSONObject ret = new JSONObject();
        ret.put("status", message.getErrorName());
        ret.put("message", message.getErrorDescription());
        ret.put("organizations", orgs);
        ctx.result(ret.toString());
      };

  public Handler enrollOrganization =
      ctx -> {
        log.info("Starting enrollOrganization handler");
        JSONObject req = new JSONObject(ctx.body());

        log.info("Getting fields from form");
        String firstName = req.getString("firstname").toUpperCase().strip();
        String lastName = req.getString("lastname").toUpperCase().strip();
        String birthDate = req.getString("birthDate").strip();
        String orgEmail = req.getString("organizationEmail").toLowerCase().strip();
        String accountEmail = req.optString("email", "").toLowerCase().strip();
        String email = accountEmail.isEmpty() ? orgEmail : accountEmail;
        String phone = req.optString("phonenumber", "").strip();
        String address = req.optString("address", "").toUpperCase().strip();
        String city = req.optString("city", "").toUpperCase().strip();
        String state = req.optString("state", "").toUpperCase().strip();
        String zipcode = req.optString("zipcode", "").strip();
        Boolean twoFactorOn = req.optBoolean("twoFactorOn", false);
        String username = req.optString("username", "").strip();
        if (username.isEmpty()) {
          String dobCompact = birthDate.replace("-", "");
          String randomSuffix = UUID.randomUUID().toString().substring(0, 4);
          username =
              ValidationUtils.slugForEnrollmentUsernameSegment(firstName)
                  + "-"
                  + ValidationUtils.slugForEnrollmentUsernameSegment(lastName)
                  + "-"
                  + dobCompact
                  + "-"
                  + randomSuffix;
        }
        String password = req.getString("password").strip();
        UserType userLevel = UserType.Admin;

        String orgName = req.getString("organizationName").strip();
        String orgWebsite = req.getString("organizationWebsite").toLowerCase().strip();
        String orgEIN = req.getString("organizationEIN").strip();
        String orgStreetAddress = req.getString("organizationAddressStreet").toUpperCase().strip();
        String orgCity = req.getString("organizationAddressCity").toUpperCase().strip();
        String orgState = req.getString("organizationAddressState").toUpperCase().strip();
        String orgZipcode = req.getString("organizationAddressZipcode").strip();
        Address orgAddress = new Address(orgStreetAddress, orgCity, orgState, orgZipcode);
        String orgPhoneNumber = req.getString("organizationPhoneNumber").strip();

        EnrollOrganizationService eoService =
            new EnrollOrganizationService(
                db,
                activityDao,
                firstName,
                lastName,
                birthDate,
                email,
                phone,
                address,
                city,
                state,
                zipcode,
                twoFactorOn,
                username,
                password,
                userLevel,
                orgName,
                orgWebsite,
                orgEIN,
                orgAddress,
                orgEmail,
                orgPhoneNumber);
        ctx.result(eoService.executeAndGetResponse().toJSON().toString());
      };
  /*  Invite users through email under an organization with a JSON Object formatted as:
      {“senderName”: “senderName”,
       "organization": "orgName",
               data: [
                      {
                          “firstName”:”exampleFirstName”,
                          “lastName”:”exampleLastName”,
                          “email”:”exampleEmail”,
                          “role”: “Worker”,
                      }
         ]
      }
  */
  public Handler inviteUsers =
      ctx -> {
        log.info("Starting inviteUsers handler");
        JSONObject req = new JSONObject(ctx.body());
        JSONArray people = req.getJSONArray("data");

        String sender = ctx.sessionAttribute("fullName");
        String org = ctx.sessionAttribute("orgName");

        InviteUserService iuservice = new InviteUserService(db, people, sender, org, emailSender);
        ctx.result(iuservice.executeAndGetResponse().toResponseString());
      };
}
