package UserV2.Services;

import Config.Message;
import Config.Service;
import Database.UserV2.UserDao;
import UserV2.User;
import UserV2.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

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

  public static User getUserFromRequest(UserDao userDao, String req) {
    log.info("Starting check for user...");
    String username;
    User user = null;
    try {
      JSONObject reqJson = new JSONObject(req);
      if (reqJson.has("targetUser")) {
        username = reqJson.getString("targetUser");
        Optional<User> optionalUser = userDao.get(username);
        if (optionalUser.isPresent()) user = optionalUser.get();
      }
    } catch (JSONException e) {
      log.error("JSON Error when reading request body ... {}", e.getMessage());
    }
    log.info("User check completed...");
    return user;
  }

  public JSONObject getUserFields() {
    checkState(user != null, "user must exist");
    return user.serialize();
  }
}
