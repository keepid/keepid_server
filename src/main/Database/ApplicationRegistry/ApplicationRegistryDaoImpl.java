package Database.ApplicationRegistry;

import static com.mongodb.client.model.Filters.*;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Form.ApplicationRegistryEntry;
import Form.ApplicationRegistryEntry.OrgMapping;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

public class ApplicationRegistryDaoImpl implements ApplicationRegistryDao {
  private final MongoCollection<ApplicationRegistryEntry> collection;

  public ApplicationRegistryDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    collection = db.getCollection("application_registry", ApplicationRegistryEntry.class);
    ensureIndexes();
  }

  private void ensureIndexes() {
    collection.createIndex(
        Indexes.compoundIndex(
            Indexes.ascending("idCategoryType"),
            Indexes.ascending("state"),
            Indexes.ascending("applicationSubtype"),
            Indexes.ascending("pidlSubtype")),
        new IndexOptions().unique(true));
  }

  @Override
  public List<ApplicationRegistryEntry> getAll() {
    return collection.find().into(new ArrayList<>());
  }

  @Override
  public Optional<ApplicationRegistryEntry> get(ObjectId id) {
    return Optional.ofNullable(collection.find(eq("_id", id)).first());
  }

  @Override
  public Optional<ApplicationRegistryEntry> find(
      String idCategoryType, String state, String applicationSubtype, String pidlSubtype) {
    Bson filter;
    if (pidlSubtype == null || pidlSubtype.isEmpty()) {
      filter =
          and(
              eq("idCategoryType", idCategoryType),
              eq("state", state),
              eq("applicationSubtype", applicationSubtype),
              or(eq("pidlSubtype", null), eq("pidlSubtype", "")));
    } else {
      filter =
          and(
              eq("idCategoryType", idCategoryType),
              eq("state", state),
              eq("applicationSubtype", applicationSubtype),
              eq("pidlSubtype", pidlSubtype));
    }
    return Optional.ofNullable(collection.find(filter).first());
  }

  @Override
  public Optional<ApplicationRegistryEntry> findByLookupKey(String lookupKey) {
    return Optional.ofNullable(collection.find(eq("lookupKey", lookupKey)).first());
  }

  @Override
  public void save(ApplicationRegistryEntry entry) {
    collection.insertOne(entry);
  }

  @Override
  public void update(ApplicationRegistryEntry entry) {
    entry.setLastModifiedAt(LocalDateTime.now());
    collection.replaceOne(eq("_id", entry.getId()), entry);
  }

  @Override
  public void addOrgMapping(ObjectId registryId, OrgMapping mapping) {
    collection.updateOne(
        eq("_id", registryId),
        Updates.combine(
            Updates.push("orgMappings", mapping),
            Updates.set("lastModifiedAt", LocalDateTime.now())));
  }

  @Override
  public void removeOrgMapping(ObjectId registryId, String orgName) {
    collection.updateOne(
        eq("_id", registryId),
        Updates.combine(
            Updates.pull("orgMappings", new Document("orgName", orgName)),
            Updates.set("lastModifiedAt", LocalDateTime.now())));
  }

  @Override
  public void delete(ObjectId registryId) {
    collection.deleteOne(eq("_id", registryId));
  }

  @Override
  public void delete(ApplicationRegistryEntry entry) {
    collection.deleteOne(eq("_id", entry.getId()));
  }

  @Override
  public int size() {
    return (int) collection.countDocuments();
  }

  @Override
  public void clear() {
    collection.deleteMany(new Document());
  }
}
