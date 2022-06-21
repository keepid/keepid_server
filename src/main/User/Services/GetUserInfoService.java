package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.Optional;

@Slf4j
public class GetUserInfoService implements Service {
  private UserDao userDao;
  private String username;
  private User user;

  public GetUserInfoService(UserDao userDao, String username) {
    this.userDao = userDao;
    this.username = username;
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

  @Override
  public Message executeAndGetResponse() {
    Objects.requireNonNull(username);
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

  public JSONObject getUserFields() {
    Objects.requireNonNull(user);
    JSONObject userObject = new JSONObject();
    userObject.put("userRole", this.user.getUserType());
    userObject.put("organization", user.getOrganization());
    userObject.put("firstName", user.getFirstName());
    userObject.put("lastName", user.getLastName());
    userObject.put("birthDate", user.getBirthDate());
    userObject.put("address", user.getAddress());
    userObject.put("city", user.getCity());
    userObject.put("state", user.getState());
    userObject.put("zipcode", user.getZipcode());
    userObject.put("email", user.getEmail());
    userObject.put("phone", user.getPhone());
    userObject.put("twoFactorOn", user.getTwoFactorOn());
    userObject.put("username", user.getUsername());
    return userObject;
  }
}
