package Organization;

import Database.Organization.OrgDao;
import com.google.inject.Inject;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

@Slf4j
public class OrgControllerV2 implements CrudHandler {

  private final OrgDao orgDao;

  @Inject
  public OrgControllerV2(OrgDao orgDao) {
    this.orgDao = orgDao;
  }

  public void getAll(Context context) {
    log.debug("Getting all organizations");
    List<Organization> orgList = orgDao.getAll();
    context.result(orgList.toString());
  }

  public void getOne(Context context, String orgId) {
    log.debug("Getting organization orgId={}", orgId);
    ObjectId objectId = new ObjectId(orgId);
    Optional<Organization> optionalOrg = orgDao.get(objectId);
    if (optionalOrg.isPresent()) {
      context.result(optionalOrg.get().toString());
    } else {
      log.error("There was an error getting organization orgId={}", orgId);
      context.status(403);
    }
  }

  public void create(Context context) {
    Organization org = context.bodyAsClass(Organization.class);
    log.debug("Creating new org orgName={}", org.getOrgName());
    orgDao.save(org);
  }

  public void update(Context context, String orgId) {
    log.debug("Updating organization orgId={}", orgId);
    Organization org = context.bodyAsClass(Organization.class);
    ObjectId objectId = new ObjectId(orgId);
    org.setId(objectId);
    orgDao.update(org);
  }

  public void delete(Context context, String orgId) {
    log.debug("Deleting org orgId={}", orgId);
    orgDao.delete(orgId);
  }
}
