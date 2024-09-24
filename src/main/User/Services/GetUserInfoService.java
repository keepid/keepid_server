package User.Services;

import static com.google.common.base.Preconditions.checkState;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;

@Slf4j
public class GetUserInfoService implements Service {
  private final UserDao userDao;
  private final String username;
  private User user;

  public GetUserInfoService(UserDao userDao, String username) {
    this.userDao = userDao;
    this.username = username;
  }

  @Override
  public Message executeAndGetResponse() {
    if (username == null) {
      return UserMessage.SESSION_TOKEN_FAILURE;
    }
    Optional<User> optionalUser = userDao.get(username);
    if (optionalUser.isEmpty()) {
      log.error("Session Token Failure");
      return UserMessage.USER_NOT_FOUND;
    } else {
      this.user = optionalUser.get();
      log.info("Successfully got user info");
      return UserMessage.SUCCESS;
    }
  }

  public static Optional<User> getUserFromRequest(UserDao userDao, String req) {
    log.info("Starting check for user...");
    String username;
    try {
      JSONObject reqJson = new JSONObject(req);
      if (reqJson.has("targetUser")) {
        username = reqJson.getString("targetUser");
        Optional<User> optionalUser = userDao.get(username);
        return optionalUser;
      }
    } catch (JSONException e) {
      log.error("JSON Error when reading request body ... {}", e.getMessage());
    }
    return Optional.empty();
  }

  public JSONObject getUserFields() {
    checkState(user != null, "user must exist");
    return user.serialize();
  }
}
