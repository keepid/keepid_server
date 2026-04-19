package Database.ApplicationRegistry;

import Config.DeploymentLevel;
import Form.ApplicationRegistryEntry;
import Form.ApplicationRegistryEntry.OrgMapping;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;

public class ApplicationRegistryDaoTestImpl implements ApplicationRegistryDao {
  private final Map<ObjectId, ApplicationRegistryEntry> store = new LinkedHashMap<>();

  public ApplicationRegistryDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in-memory test database in production or staging");
    }
  }

  @Override
  public List<ApplicationRegistryEntry> getAll() {
    return new ArrayList<>(store.values());
  }

  @Override
  public Optional<ApplicationRegistryEntry> get(ObjectId id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public Optional<ApplicationRegistryEntry> find(
      String idCategoryType, String state, String applicationSubtype, String pidlSubtype) {
    return store.values().stream()
        .filter(
            e ->
                e.getIdCategoryType().equals(idCategoryType)
                    && e.getState().equals(state)
                    && e.getApplicationSubtype().equals(applicationSubtype)
                    && Objects.equals(e.getPidlSubtype(), pidlSubtype))
        .findFirst();
  }

  @Override
  public Optional<ApplicationRegistryEntry> findByLookupKey(String lookupKey) {
    return store.values().stream()
        .filter(e -> lookupKey.equals(e.getLookupKey()))
        .findFirst();
  }

  @Override
  public void save(ApplicationRegistryEntry entry) {
    store.put(entry.getId(), entry);
  }

  @Override
  public void update(ApplicationRegistryEntry entry) {
    entry.setLastModifiedAt(LocalDateTime.now());
    store.put(entry.getId(), entry);
  }

  @Override
  public void addOrgMapping(ObjectId registryId, OrgMapping mapping) {
    ApplicationRegistryEntry entry = store.get(registryId);
    if (entry != null) {
      entry.getOrgMappings().add(mapping);
      entry.setLastModifiedAt(LocalDateTime.now());
    }
  }

  @Override
  public void removeOrgMapping(ObjectId registryId, String orgName) {
    ApplicationRegistryEntry entry = store.get(registryId);
    if (entry != null) {
      entry.setOrgMappings(
          entry.getOrgMappings().stream()
              .filter(m -> !m.getOrgName().equals(orgName))
              .collect(Collectors.toList()));
      entry.setLastModifiedAt(LocalDateTime.now());
    }
  }

  @Override
  public void delete(ObjectId registryId) {
    store.remove(registryId);
  }

  @Override
  public void delete(ApplicationRegistryEntry entry) {
    store.remove(entry.getId());
  }

  @Override
  public int size() {
    return store.size();
  }

  @Override
  public void clear() {
    store.clear();
  }
}
