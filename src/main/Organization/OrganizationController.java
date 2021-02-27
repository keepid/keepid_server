package Organization;

import Activity.ActivityController;
import Activity.CreateOrgActivity;
import Database.Organization.OrgDao;
import Database.User.UserDao;
import Organization.Services.InviteUserService;
import Security.EncryptionUtils;
import Security.SecurityUtils;
import User.User;
import com.google.inject.Inject;
import io.javalin.http.Handler;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

@Slf4j
public class OrganizationController {

  OrgDao orgDao;
  UserDao userDao;
  ActivityController activityController;

  public static final String newOrgTestURL =
      Objects.requireNonNull(System.getenv("NEW_ORG_TESTURL"));
  public static final String newOrgActualURL =
      Objects.requireNonNull(System.getenv("NEW_ORG_ACTUALURL"));
  EncryptionUtils encryptionUtils;

  @Inject
  public OrganizationController(
      OrgDao orgDao, UserDao userDao, ActivityController activityController) {
    this.encryptionUtils = EncryptionUtils.getInstance();
    this.orgDao = orgDao;
    this.activityController = activityController;
  }

  public Handler listOrgs =
      ctx -> {
        List<Organization> orgList = orgDao.getAll();
        JSONObject ret = new JSONObject();
        ret.put("organizations", orgList);
        ctx.result(ret.toString());
      };

  public Handler enrollOrganization =
      ctx -> {
        log.info("Starting enrollOrganization handler");
        EnrollOrgRequest req = ctx.bodyAsClass(EnrollOrgRequest.class);
        CreateOrgActivity createOrgActivity =
            new CreateOrgActivity(req.getUser(), req.getOrganization());
        activityController.addActivity(createOrgActivity);

        User userToSave = req.getUser();
        Organization orgToSave = req.getOrganization();

        String newUserPassword = hashPassword(userToSave.getPassword());
        assert newUserPassword != null;
        userToSave.setPassword(hashPassword(userToSave.getPassword()));
        userDao.save(userToSave);
        orgDao.save(orgToSave);
        makeBotMessage(orgToSave);
      };

  private String hashPassword(String password) {
    return SecurityUtils.hashPassword(password);
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

        InviteUserService iuservice = new InviteUserService(people, sender, org);
        ctx.result(iuservice.executeAndGetResponse().toResponseString());
      };
}
