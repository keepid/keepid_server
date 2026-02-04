package User.Services;

import static com.google.common.base.Preconditions.checkState;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
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

  /**
   * Returns a flattened map of all user fields including nested
   * optionalInformation.
   * Keys use dot notation for nested objects (e.g.,
   * "optionalInformation.person.firstName")
   * and index notation for arrays (e.g.,
   * "optionalInformation.familyInfo.parents.0.firstName").
   * Null fields are excluded from the map.
   *
   * @return Map of field paths to string values
   */
  public Map<String, String> getFlattenedFieldMap() {
    checkState(user != null, "user must exist");
    Map<String, String> flattened = new HashMap<>();

    // Use JSON serialization for reliable conversion
    JSONObject userJSON = user.serialize();

    // Flatten the JSON recursively
    flattenJSON(userJSON, "", flattened);

    return flattened;
  }

  /**
   * Recursively flattens a JSONObject structure.
   *
   * @param json   The JSONObject to flatten
   * @param prefix The current path prefix (e.g., "optionalInformation.person")
   * @param result The result map to populate
   */
  private void flattenJSON(JSONObject json, String prefix, Map<String, String> result) {
    if (json == null) {
      return;
    }

    String[] names = JSONObject.getNames(json);
    if (names == null) {
      return;
    }

    for (String key : names) {
      if (shouldSkipKey(key, prefix)) {
        continue;
      }

      String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
      Object value = json.opt(key);

      processValue(value, fullKey, result);
    }
  }

  private boolean shouldSkipKey(String key, String prefix) {
    // Skip password for security
    if ("password".equals(key)) {
      return true;
    }

    // Skip firstName/lastName from optionalInformation.person - they come from root level
    boolean isPersonFirstName = "firstName".equals(key) && prefix.contains("optionalInformation.person");
    boolean isPersonLastName = "lastName".equals(key) && prefix.contains("optionalInformation.person");
    return isPersonFirstName || isPersonLastName;
  }

  private void processValue(Object value, String fullKey, Map<String, String> result) {
    if (value == null || JSONObject.NULL.equals(value)) {
      // Skip null values
      return;
    }

    if (value instanceof JSONObject) {
      flattenJSON((JSONObject) value, fullKey, result);
    } else if (value instanceof org.json.JSONArray) {
      processArray((org.json.JSONArray) value, fullKey, result);
    } else {
      // Primitive or simple object - convert to string
      result.put(fullKey, convertToString(value));
    }
  }

  private void processArray(org.json.JSONArray array, String fullKey, Map<String, String> result) {
    for (int i = 0; i < array.length(); i++) {
      Object item = array.opt(i);
      if (item instanceof JSONObject) {
        flattenJSON((JSONObject) item, fullKey + "." + i, result);
      } else if (item != null && !JSONObject.NULL.equals(item)) {
        result.put(fullKey + "." + i, convertToString(item));
      }
    }
  }

  /**
   * Converts an object to its string representation.
   * Handles special cases like Date, Enum, Boolean, etc.
   */
  private String convertToString(Object value) {
    if (value == null || JSONObject.NULL.equals(value)) {
      return null;
    }

    if (value instanceof Boolean) {
      return String.valueOf(value);
    } else if (value instanceof Number) {
      return String.valueOf(value);
    } else {
      return String.valueOf(value);
    }
  }
}
