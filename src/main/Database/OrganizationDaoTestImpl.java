package Database;

import Config.DeploymentLevel;
import Organization.Organization;
import org.bson.types.ObjectId;

import java.util.*;

public class OrganizationDaoTestImpl implements OrganizationDao {
  Map<String, Organization> orgMap;

  public OrganizationDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in memory test database in production or staging");
    }
    orgMap = new LinkedHashMap<>();
  }

  @Override
  public Optional<Organization> get(String orgName) {
    return Optional.ofNullable(orgMap.get(orgName));
  }

  @Override
  public void delete(String orgName) {
    orgMap.remove(orgName);
  }

  @Override
  public Optional<Organization> get(ObjectId id) {
    for (Organization org : orgMap.values()) {
      if (org.getId().equals(id)) {
        return Optional.of(org);
      }
    }
    return Optional.empty();
  }

  @Override
  public List<Organization> getAll() {
    return new ArrayList<Organization>(orgMap.values());
  }

  @Override
  public int size() {
    return orgMap.size();
  }

  @Override
  public void delete(Organization org) {
    orgMap.remove(org.getOrgName());
  }

  @Override
  public void clear() {
    orgMap.clear();
  }

  @Override
  public void update(Organization org, String[] params) {
    // implement later
  }

  @Override
  public void save(Organization org) {
    orgMap.put(org.getOrgName(), org);
  }
}
