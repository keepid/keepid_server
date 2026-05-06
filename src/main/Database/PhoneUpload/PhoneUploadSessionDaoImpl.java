package Database.PhoneUpload;

import static com.mongodb.client.model.Filters.eq;

import Config.DeploymentLevel;
import Config.MongoConfig;
import PhoneUpload.PhoneUploadSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;

public class PhoneUploadSessionDaoImpl implements PhoneUploadSessionDao {
  private final MongoCollection<PhoneUploadSession> phoneUploadSessionCollection;

  public PhoneUploadSessionDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    phoneUploadSessionCollection =
        db.getCollection("phone_upload_sessions", PhoneUploadSession.class);
    ensureIndexes();
  }

  private void ensureIndexes() {
    phoneUploadSessionCollection.createIndex(Indexes.ascending("tokenHash"));
    phoneUploadSessionCollection.createIndex(
        Indexes.ascending("expiresAt"), new IndexOptions().expireAfter(0L, java.util.concurrent.TimeUnit.SECONDS));
  }

  @Override
  public Optional<PhoneUploadSession> getByTokenHash(String tokenHash) {
    return Optional.ofNullable(phoneUploadSessionCollection.find(eq("tokenHash", tokenHash)).first());
  }

  @Override
  public Optional<PhoneUploadSession> get(ObjectId id) {
    return Optional.ofNullable(phoneUploadSessionCollection.find(eq("_id", id)).first());
  }

  @Override
  public List<PhoneUploadSession> getAll() {
    return phoneUploadSessionCollection.find().into(new ArrayList<>());
  }

  @Override
  public int size() {
    return (int) phoneUploadSessionCollection.countDocuments();
  }

  @Override
  public void save(PhoneUploadSession phoneUploadSession) {
    phoneUploadSessionCollection.insertOne(phoneUploadSession);
  }

  @Override
  public void update(PhoneUploadSession phoneUploadSession) {
    phoneUploadSessionCollection.replaceOne(eq("_id", phoneUploadSession.getId()), phoneUploadSession);
  }

  @Override
  public void delete(PhoneUploadSession phoneUploadSession) {
    phoneUploadSessionCollection.deleteOne(eq("_id", phoneUploadSession.getId()));
  }

  @Override
  public void clear() {
    phoneUploadSessionCollection.deleteMany(new Document());
  }
}
