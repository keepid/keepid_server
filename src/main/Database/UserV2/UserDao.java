package Database.UserV2;

import Database.Dao;
import UserV2.User;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public interface UserDao extends Dao<User> {

  List<User> getAllFromOrg(String orgName);

  List<User> getAllFromOrg(ObjectId id);

  Optional<User> get(String username);

  void delete(String username);

  void update(User user);

  void resetPassword(User user, String newpassword);
}
