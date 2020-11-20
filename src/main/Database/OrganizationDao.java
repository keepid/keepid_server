package Database;

import Organization.Organization;

import java.util.Optional;

public interface OrganizationDao extends Dao<Organization> {
  Optional<Organization> get(String orgName);

  void delete(String orgName);
}
