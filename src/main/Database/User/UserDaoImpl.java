package Database.User;

import Config.MongoTestConfig;
import User.User;
import com.google.inject.Inject;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class UserDaoImpl implements UserDao {
  private final MongoCollection<User> userCollection;

  @Inject
  public UserDaoImpl(MongoTestConfig mongoTestConfig) {
    this.userCollection = mongoTestConfig.getDatabase().getCollection("user", User.class);
  }

  @Override
  public Optional<User> get(String username) {
    return Optional.ofNullable(userCollection.find(eq("username", username)).first());
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
  public void deleteAllFromOrg(String orgName) {
    userCollection.deleteMany(eq("organization", orgName));
  }

  @Override
  public void resetPassword(User user, String password) {
    userCollection.updateOne(
        eq("username", user.getUsername()),
        new Document("$set", new Document("password", password)));
  }

  @Override
  public void save(User user) {
    userCollection.insertOne(user);
  }
}
