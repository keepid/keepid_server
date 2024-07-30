package Database.User;

import Database.Dao;
import User.User;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;

public interface UserDao extends Dao<User> {

  List<User> getAll();

  List<User> getAllFromOrg(String orgName);

  List<User> getAllFromOrg(ObjectId id);

  Optional<User> get(String username);

  void delete(String username);

  void update(User user);

  void resetPassword(User user, String newpassword);
}
