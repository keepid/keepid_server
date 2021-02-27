package Database.VaccineRecord;

import Config.DeploymentLevel;
import Config.MongoConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;

public class VaccineRecordDaoImpl implements VaccineRecordDao {
  private final MongoCollection<VaccineRecord> vaccineRecordCollection;

  public VaccineRecordDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    vaccineRecordCollection = db.getCollection("vaccineRecord", VaccineRecord.class);
  }

  @Override
  public Optional<VaccineRecord> get(ObjectId id) {
    return Optional.ofNullable(vaccineRecordCollection.find(eq("_id", id)).first());
  }

  @Override
  public List<VaccineRecord> getAll() {
    return vaccineRecordCollection.find().into(new ArrayList<>());
  }

  @Override
  public List<VaccineRecord> getAllBetween(long start, long end) {
    long today = LocalDate.now().toEpochDay();
    long week = today + 8 * 24 * 60 * 60 * 1000;
    return vaccineRecordCollection
        .find(or(gte("dateOfNextDose", today), lte("dateOfNextDose", week)))
        .into(new ArrayList<>());
  }

  @Override
  public List<VaccineRecord> getAllFromOrg(String orgName) {
    return vaccineRecordCollection.find(eq("orgName", orgName)).into(new ArrayList<>());
  }

  @Override
  public int size() {
    return (int) vaccineRecordCollection.countDocuments();
  }

  @Override
  public void delete(VaccineRecord record) {
    vaccineRecordCollection.deleteOne(eq("_id", record.getId()));
  }

  @Override
  public void clear() {
    vaccineRecordCollection.drop();
  }

  @Override
  public void update(VaccineRecord record) {
    vaccineRecordCollection.replaceOne(eq("_id", record.getId()), record);
  }

  @Override
  public void save(VaccineRecord record) {
    vaccineRecordCollection.insertOne(record);
  }
}
