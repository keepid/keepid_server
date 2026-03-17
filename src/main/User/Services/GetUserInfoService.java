package User.Services;

import static com.google.common.base.Preconditions.checkState;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
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
    } catch (org.json.JSONException e) {
      log.error("JSON Error when reading request body ... {}", e.getMessage());
    }
    return Optional.empty();
  }

  public JSONObject getUserFields() {
    checkState(user != null, "user must exist");
    return user.serialize();
  }

  /**
   * Returns a flattened map of all user fields including nested structures.
   * Keys use dot notation for nested objects (e.g., "currentName.first", "personalAddress.city")
   * and index notation for arrays (e.g., "phoneBook.0.phoneNumber").
   */
  public Map<String, String> getFlattenedFieldMap() {
    checkState(user != null, "user must exist");
    Map<String, String> flattened = new HashMap<>();
    JSONObject userJSON = user.serialize();
    flattenJSON(userJSON, "", flattened);
    return flattened;
  }

  private void flattenJSON(JSONObject json, String prefix, Map<String, String> result) {
    if (json == null) return;
    String[] names = JSONObject.getNames(json);
    if (names == null) return;

    for (String key : names) {
      if (shouldSkipKey(key)) continue;

      String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
      Object value = json.opt(key);
      processValue(value, fullKey, result);
    }
  }

  private boolean shouldSkipKey(String key) {
    return "password".equals(key);
  }

  private void processValue(Object value, String fullKey, Map<String, String> result) {
    if (value == null || JSONObject.NULL.equals(value)) return;

    if (value instanceof JSONObject) {
      flattenJSON((JSONObject) value, fullKey, result);
    } else if (value instanceof org.json.JSONArray) {
      processArray((org.json.JSONArray) value, fullKey, result);
    } else {
      result.put(fullKey, String.valueOf(value));
    }
  }

  private void processArray(org.json.JSONArray array, String fullKey, Map<String, String> result) {
    for (int i = 0; i < array.length(); i++) {
      Object item = array.opt(i);
      if (item instanceof JSONObject) {
        flattenJSON((JSONObject) item, fullKey + "." + i, result);
      } else if (item != null && !JSONObject.NULL.equals(item)) {
        result.put(fullKey + "." + i, String.valueOf(item));
      }
    }
  }
}
