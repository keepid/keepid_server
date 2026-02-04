package Database.User;

import Config.DeploymentLevel;
import Config.MongoConfig;
import User.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class UserDaoImpl implements UserDao {
  private final MongoCollection<User> userCollection;

  public UserDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    userCollection = db.getCollection("user", User.class);
  }

  @Override
  public Optional<User> get(String username) {
    return Optional.ofNullable(userCollection.find(eq("username", username)).first());
  }

  @Override
  public Optional<User> getByEmail(String email) {
    return Optional.ofNullable(userCollection.find(eq("email", email)).first());
  }

  @Override
  public Optional<User> get(ObjectId id) {
    return Optional.ofNullable(userCollection.find(eq("_id", id)).first());
  }

  @Override
  public List<User> getAll() {
    return userCollection.find().into(new ArrayList<>());
  }

  @Override
  public List<User> getAllFromOrg(String orgName) {
    return userCollection.find(eq("organization", orgName)).into(new ArrayList<>());
  }

  @Override
  public List<User> getAllFromOrg(ObjectId objectId) {
    return userCollection.find(eq("_id", objectId)).into(new ArrayList<>());
  }

  @Override
  public int size() {
    return (int) userCollection.countDocuments();
  }

  @Override
  public void delete(User user) {
    userCollection.deleteOne(eq("username", user.getUsername()));
  }

  @Override
  public void clear() {
    userCollection.drop();
  }

  @Override
  public void delete(String username) {
    userCollection.deleteOne(eq("username", username));
  }

  @Override
  public void update(User user) {
    userCollection.replaceOne(eq("username", user.getUsername()), user);
  }

  @Override
  public void resetPassword(User user, String password) {
    // note that pwd is already hashed
    userCollection.updateOne(
        eq("username", user.getUsername()),
        new Document("$set", new Document("password", password)));
  }

  @Override
  public void save(User user) {
    userCollection.insertOne(user);
  }

  @Override
  public void deleteField(String username, String fieldPath) {
    userCollection.updateOne(
        eq("username", username),
        new Document("$unset", new Document(fieldPath, "")));
  }

  @Override
  public void updateField(String username, String fieldPath, Object value) {
    // MongoDB $set with dot notation creates nested structures automatically,
    // but we need to ensure the update is applied correctly
    userCollection.updateOne(
        eq("username", username),
        new Document("$set", new Document(fieldPath, value)));
  }
}
