package Database.InteractiveFormConfig;

import static com.mongodb.client.model.Filters.eq;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Form.InteractiveFormConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;

public class InteractiveFormConfigDaoImpl implements InteractiveFormConfigDao {
  private final MongoCollection<InteractiveFormConfig> collection;

  public InteractiveFormConfigDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    collection =
        db.getCollection("interactive_form_configs", InteractiveFormConfig.class);
    ensureIndexes();
  }

  private void ensureIndexes() {
    collection.createIndex(
        Indexes.ascending("fileId"), new IndexOptions().unique(true));
  }

  @Override
  public Optional<InteractiveFormConfig> get(ObjectId id) {
    return Optional.ofNullable(collection.find(eq("_id", id)).first());
  }

  @Override
  public Optional<InteractiveFormConfig> getByFileId(ObjectId fileId) {
    return Optional.ofNullable(collection.find(eq("fileId", fileId)).first());
  }

  @Override
  public List<InteractiveFormConfig> getAll() {
    return collection.find().into(new ArrayList<>());
  }

  @Override
  public int size() {
    return (int) collection.countDocuments();
  }

  @Override
  public void save(InteractiveFormConfig config) {
    collection.insertOne(config);
  }

  @Override
  public void update(InteractiveFormConfig config) {
    config.setLastModifiedAt(LocalDateTime.now());
    collection.replaceOne(eq("_id", config.getId()), config);
  }

  @Override
  public void upsertByFileId(InteractiveFormConfig config) {
    config.setLastModifiedAt(LocalDateTime.now());
    collection.replaceOne(
        eq("fileId", config.getFileId()),
        config,
        new ReplaceOptions().upsert(true));
  }

  @Override
  public void delete(InteractiveFormConfig config) {
    collection.deleteOne(eq("_id", config.getId()));
  }

  @Override
  public void deleteByFileId(ObjectId fileId) {
    collection.deleteOne(eq("fileId", fileId));
  }

  @Override
  public void clear() {
    collection.deleteMany(new Document());
  }
}
