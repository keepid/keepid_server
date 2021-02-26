package User;

import Database.User.UserDao;
import com.google.inject.Inject;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class UserControllerV2 implements CrudHandler {

  private final UserDao userDao;

  @Inject
  public UserControllerV2(UserDao userDao) {
    this.userDao = userDao;
  }

  public void getAll(Context context) {
    log.debug("Getting all users");
    List<User> userList = userDao.getAll();
    context.result(userList.toString());
  }

  public void getOne(Context context, String username) {
    log.debug("Getting user username={}", username);
    Optional<User> optionalUser = userDao.get(username);
    if (optionalUser.isPresent()) {
      context.result(optionalUser.get().toString());
    } else {
      log.error("There was an error getting user username={}", username);
      context.status(403);
    }
  }

  public void create(Context context) {
    User user = context.bodyAsClass(User.class);
    log.debug("Creating new user username={}", user.getUsername());
    userDao.save(user);
  }

  public void update(Context context, String username) {
    log.debug("Updating user username={}", username);
    User user = context.bodyAsClass(User.class);
    userDao.update(user);
  }

  public void delete(Context context, String username) {
    log.debug("Deleting user username={}", username);
    userDao.delete(username);
  }
}
