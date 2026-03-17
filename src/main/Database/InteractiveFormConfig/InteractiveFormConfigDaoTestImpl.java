package Database.InteractiveFormConfig;

import Config.DeploymentLevel;
import Form.InteractiveFormConfig;
import java.time.LocalDateTime;
import java.util.*;
import org.bson.types.ObjectId;

public class InteractiveFormConfigDaoTestImpl implements InteractiveFormConfigDao {
  private final Map<ObjectId, InteractiveFormConfig> store = new LinkedHashMap<>();

  public InteractiveFormConfigDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in-memory test database in production or staging");
    }
  }

  @Override
  public Optional<InteractiveFormConfig> get(ObjectId id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public Optional<InteractiveFormConfig> getByFileId(ObjectId fileId) {
    return store.values().stream()
        .filter(c -> fileId.equals(c.getFileId()))
        .findFirst();
  }

  @Override
  public List<InteractiveFormConfig> getAll() {
    return new ArrayList<>(store.values());
  }

  @Override
  public int size() {
    return store.size();
  }

  @Override
  public void save(InteractiveFormConfig config) {
    store.put(config.getId(), config);
  }

  @Override
  public void update(InteractiveFormConfig config) {
    config.setLastModifiedAt(LocalDateTime.now());
    store.put(config.getId(), config);
  }

  @Override
  public void upsertByFileId(InteractiveFormConfig config) {
    Optional<InteractiveFormConfig> existing = getByFileId(config.getFileId());
    if (existing.isPresent()) {
      InteractiveFormConfig old = existing.get();
      old.setJsonSchema(config.getJsonSchema());
      old.setUiSchema(config.getUiSchema());
      old.setLastModifiedAt(LocalDateTime.now());
      store.put(old.getId(), old);
    } else {
      save(config);
    }
  }

  @Override
  public void delete(InteractiveFormConfig config) {
    store.remove(config.getId());
  }

  @Override
  public void deleteByFileId(ObjectId fileId) {
    store.values().removeIf(c -> fileId.equals(c.getFileId()));
  }

  @Override
  public void clear() {
    store.clear();
  }
}
