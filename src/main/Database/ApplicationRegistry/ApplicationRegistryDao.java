package Database.ApplicationRegistry;

import Database.Dao;
import Form.ApplicationRegistryEntry;
import Form.ApplicationRegistryEntry.OrgMapping;
import java.util.Optional;
import org.bson.types.ObjectId;

public interface ApplicationRegistryDao extends Dao<ApplicationRegistryEntry> {

  /**
   * Finds a registry entry by its compound key. pidlSubtype may be null for non-PIDL categories.
   */
  Optional<ApplicationRegistryEntry> find(
      String idCategoryType, String state, String applicationSubtype, String pidlSubtype);

  void addOrgMapping(ObjectId registryId, OrgMapping mapping);

  void removeOrgMapping(ObjectId registryId, String orgName);

  void delete(ObjectId registryId);
}
