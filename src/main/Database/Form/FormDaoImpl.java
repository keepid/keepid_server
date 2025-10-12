package Database.Form;

import static com.mongodb.client.model.Filters.*;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Form.Form;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;

public class FormDaoImpl implements FormDao {
  private MongoCollection<Form> formCollection;

  public FormDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    formCollection = db.getCollection("form", Form.class);
  }

  @Override
  public List<Form> getAll() {
    return formCollection.find().into(new ArrayList<>());
  }

  @Override
  public int size() {
    return (int) formCollection.countDocuments();
  }

  @Override
  public void clear() {
    formCollection.drop();
  }

  @Override
  public void delete(Form form) {
    formCollection.deleteOne(eq("fileId", form.getFileId()));
  }

  @Override
  public void update(Form form) {
    formCollection.replaceOne(eq("fileId", form.getFileId()), form);
  }

  @Override
  public void save(Form form) {
    formCollection.insertOne(form);
  }

  @Override
  public Optional<Form> get(ObjectId id) {
    return Optional.ofNullable(formCollection.find(eq("_id", id)).first());
  }

  @Override
  public Optional<Form> getByFileId(ObjectId fileId) {
    return Optional.ofNullable(formCollection.find(eq("fileId", fileId)).first());
  }

  @Override
  public List<Form> get(String username) {
    return formCollection.find(eq("username", username)).into(new ArrayList<>());
  }

  @Override
  public List<Form> getWeeklyApplications() {
    LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
    Date oneWeekAgoDate = Date.from(oneWeekAgo.atZone(ZoneId.systemDefault()).toInstant());

    return formCollection
        .find(and(eq("formType", "APPLICATION"), gte("uploadedAt", oneWeekAgoDate)))
        .into(new ArrayList<>());
  }

  @Override
  public void delete(ObjectId id) {
    formCollection.deleteOne(eq("_id", id));
  }
}
