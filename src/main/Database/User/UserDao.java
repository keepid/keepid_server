package Database.User;

import Database.Dao;
import User.User;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public interface UserDao extends Dao<User> {

  List<User> getAllFromOrg(String orgName);

  List<User> getAllFromOrg(ObjectId id);

  Optional<User> get(String username);

  Optional<User> getByEmail(String email);

  void delete(String username);

  void update(User user);

  void resetPassword(User user, String newpassword);

  /**
   * @param username  The username of the user
   * @param fieldPath The dot-notation path to the field to delete
   */
  void deleteField(String username, String fieldPath);

  /**
   * @param username  The username of the user
   * @param fieldPath The dot-notation path to the field to update
   * @param value     The value to set (can be any type that MongoDB supports)
   */
  void updateField(String username, String fieldPath, Object value);
}
