package Database.PhoneUpload;

import Config.DeploymentLevel;
import PhoneUpload.PhoneUploadSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bson.types.ObjectId;

public class PhoneUploadSessionDaoTestImpl implements PhoneUploadSessionDao {
  private final Map<ObjectId, PhoneUploadSession> phoneUploadSessionMap;

  public PhoneUploadSessionDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException("Should not run in memory test database in production or staging");
    }
    phoneUploadSessionMap = new LinkedHashMap<>();
  }

  @Override
  public Optional<PhoneUploadSession> getByTokenHash(String tokenHash) {
    return phoneUploadSessionMap.values().stream()
        .filter(session -> tokenHash != null && tokenHash.equals(session.getTokenHash()))
        .findFirst();
  }

  @Override
  public Optional<PhoneUploadSession> get(ObjectId id) {
    return Optional.ofNullable(phoneUploadSessionMap.get(id));
  }

  @Override
  public List<PhoneUploadSession> getAll() {
    return new ArrayList<>(phoneUploadSessionMap.values());
  }

  @Override
  public int size() {
    return phoneUploadSessionMap.size();
  }

  @Override
  public void save(PhoneUploadSession phoneUploadSession) {
    if (phoneUploadSession.getId() == null) {
      phoneUploadSession.setId(new ObjectId());
    }
    phoneUploadSessionMap.put(phoneUploadSession.getId(), phoneUploadSession);
  }

  @Override
  public void update(PhoneUploadSession phoneUploadSession) {
    phoneUploadSessionMap.put(phoneUploadSession.getId(), phoneUploadSession);
  }

  @Override
  public void delete(PhoneUploadSession phoneUploadSession) {
    phoneUploadSessionMap.remove(phoneUploadSession.getId());
  }

  @Override
  public void clear() {
    phoneUploadSessionMap.clear();
  }
}
