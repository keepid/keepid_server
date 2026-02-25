package Production;

import Database.Organization.OrgDao;
import Database.User.UserDao;
import Organization.Organization;
import Organization.Requests.OrganizationCreateRequest;
import Organization.Requests.OrganizationUpdateRequest;
import Security.SecurityUtils;
import User.Address;
import User.Name;
import User.Requests.UserCreateRequest;
import User.User;
import io.javalin.http.Handler;
import io.javalin.http.HttpResponseException;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONArray;

import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
public class ProductionController {

  private OrgDao orgDao;
  private UserDao userDao;

  public ProductionController(OrgDao orgDao, UserDao userDao) {
    this.orgDao = orgDao;
    this.userDao = userDao;
  }

  public Handler createOrg =
      ctx -> {
        OrganizationCreateRequest organizationCreateRequest = ctx.bodyAsClass(OrganizationCreateRequest.class);

        if (orgDao.get(organizationCreateRequest.getOrgName()).isPresent()) {
          throw new HttpResponseException(409, "Organization with orgName '" + organizationCreateRequest.getOrgName() + "' already exists", new HashMap<>());
        }

        Organization organization = new Organization(
            organizationCreateRequest.getOrgName(),
            organizationCreateRequest.getOrgWebsite(),
            organizationCreateRequest.getOrgEIN(),
            organizationCreateRequest.getOrgStreetAddress(),
            organizationCreateRequest.getOrgCity(),
            organizationCreateRequest.getOrgState(),
            organizationCreateRequest.getOrgZipcode(),
            organizationCreateRequest.getOrgEmail(),
            organizationCreateRequest.getOrgPhoneNumber());

        orgDao.save(organization);

        ctx.status(201).json(organization.serialize().toMap());
      };

  public Handler readOrg =
      ctx -> {
        ObjectId objectId = new ObjectId(ctx.pathParam("orgId"));
        Optional<Organization> organizationOptional = orgDao.get(objectId);
        organizationOptional.ifPresent(organization -> ctx.json(organization.serialize().toMap()));
      };

  public Handler updateOrg =
      ctx -> {
        ObjectId objectId = new ObjectId(ctx.pathParam("orgId"));
        var updateRequest = ctx.bodyAsClass(OrganizationUpdateRequest.class);

        Optional<Organization> organizationOptional = orgDao.get(objectId);
        organizationOptional.ifPresent(value -> {
          var organization = value.updateProperties(updateRequest);
          orgDao.update(organization);
          ctx.json(organization.serialize().toMap());
        });
      };

  public Handler deleteOrg =
      ctx -> {
        ObjectId objectId = new ObjectId(ctx.pathParam("orgId"));
        orgDao.delete(objectId);
        ctx.status(204);
      };

  public Handler readAllOrgs =
      ctx -> {
        var orgs = new JSONArray(orgDao.getAll().stream().map(org -> org.serialize()).toArray()).toList();
        ctx.json(orgs);
      };

  public Handler getUsersFromOrg =
      ctx -> {
        ObjectId objectId = new ObjectId(ctx.pathParam("orgId"));
        var users = new JSONArray(userDao.getAllFromOrg(objectId).stream().map(u -> u.serialize()).toArray()).toList();
        ctx.json(users);
      };

  public Handler createUser =
      ctx -> {
        UserCreateRequest req = ctx.bodyAsClass(UserCreateRequest.class);
        Name currentName = req.getCurrentName();
        if (currentName == null) {
          throw new HttpResponseException(400, "currentName is required", new HashMap<>());
        }
        User user = new User(
            currentName,
            req.getBirthDate(),
            req.getEmail(),
            req.getPhone(),
            req.getOrganization(),
            req.getPersonalAddress(),
            false,
            req.getUsername(),
            req.getPassword(),
            req.getUserType()
        );
        user.setId(new ObjectId());
        user.setCreationDate(new Date());
        String hashedPassword = SecurityUtils.hashPassword(user.getPassword());
        user.setPassword(hashedPassword);

        if (userDao.get(user.getUsername()).isPresent()) {
          throw new HttpResponseException(409, "User with username '" + user.getUsername() + "' already exists", new HashMap<>());
        }

        if (user.getOrganization() == null) {
          throw new HttpResponseException(400, "Organization required", new HashMap<>());
        }

        if (orgDao.get(user.getOrganization()).isEmpty()) {
          throw new HttpResponseException(400, "Specified Organization does not exist", new HashMap<>());
        }

        userDao.save(user);
        ctx.status(201).json(user.serialize().toMap());
      };

  public Handler readUser =
      ctx -> {
        Optional<User> userOptional = userDao.get(ctx.pathParam("username"));
        userOptional.ifPresent(user -> ctx.json(user.serialize().toMap()));
      };

  public Handler updateUser =
      ctx -> {
        // Production API update - accepts JSON body with field-level updates
        // This endpoint is admin-only and protected by the before filter in AppConfig
        Optional<User> userOptional = userDao.get(ctx.pathParam("username"));
        userOptional.ifPresent(user -> {
          userDao.update(user);
          ctx.json(user.serialize().toMap());
        });
      };

  public Handler deleteUser =
      ctx -> {
        userDao.delete(ctx.pathParam("username"));
        ctx.status(204);
      };

  public Handler readAllUsers =
      ctx -> {
        var users = new JSONArray(userDao.getAll().stream().map(user -> user.serialize()).toArray()).toList();
        ctx.json(users);
      };
}
