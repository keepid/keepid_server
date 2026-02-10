package User.Services;

import Activity.UserActivity.AuthenticationActivity.LogInActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.User.UserDao;
import Issue.IssueController;
import Security.SecurityUtils;
import User.IpObject;
import User.User;
import User.UserMessage;
import User.UserType;
import Validation.ValidationUtils;
import io.ipinfo.api.IPInfo;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponse;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class LoginService implements Service {
  public final String IP_INFO_TOKEN = Objects.requireNonNull(System.getenv("IPINFO_TOKEN"));
  private UserDao userDao;
  private ActivityDao activityDao;
  private final String username;
  private final String password;
  private User user;
  private final String ip;
  private final String userAgent;

  public LoginService(
      UserDao userDao,
      ActivityDao activityDao,
      String username,
      String password,
      String ip,
      String userAgent) {
    this.userDao = userDao;
    this.activityDao = activityDao;
    this.username = username;
    this.password = password;
    this.ip = ip;
    this.userAgent = userAgent;
  }

  // the execute function will handle all business logic
  public Message executeAndGetResponse() {
    // validation
    if (!ValidationUtils.isValidUsername(this.username)
        || !ValidationUtils.isValidPassword(this.password)) {
      log.info("Invalid username and/or password");
      return UserMessage.AUTH_FAILURE;
    }
    // get user

    Optional<User> optionalUser = userDao.get(this.username);
    if (optionalUser.isEmpty()) {
      return UserMessage.AUTH_FAILURE;
    }
    user = optionalUser.get();
    // verify password
    if (!verifyPassword(this.password, user.getPassword())) {
      return UserMessage.AUTH_FAILURE;
    }
    recordActivityLogin(user, activityDao); // record login activity
    recordToLoginHistory(user, ip, userAgent, IP_INFO_TOKEN, userDao); // get ip location
    log.info("Login Successful!");
    return UserMessage.AUTH_SUCCESS;
  }

  public static void recordActivityLogin(User user, ActivityDao activityDao) {
    LogInActivity log = new LogInActivity(user.getUsername(), user.getTwoFactorOn());
    activityDao.save(log);
  }

  public static void recordToLoginHistory(User user, String ip, String userAgent,
                                          String IP_INFO_TOKEN, UserDao userDao) {
    List<IpObject> loginList = user.getLogInHistory();
    if (loginList == null) {
      loginList = new ArrayList<IpObject>(1000);
    }
    if (loginList.size() >= 1000) {
      loginList.remove(0);
    }
    log.info("Trying to add login to login history");

    IpObject thisLogin = new IpObject();
    ZonedDateTime currentTime = ZonedDateTime.now();
    String formattedDate =
        currentTime.format(DateTimeFormatter.ofPattern("MM/dd/YYYY, HH:mm")) + " Local Time";
    boolean isMobile = userAgent.contains("Mobi");
    String device = isMobile ? "Mobile" : "Computer";

    thisLogin.setDate(formattedDate);
    thisLogin.setIp(ip);
    thisLogin.setDevice(device);

    IPInfo ipInfo = IPInfo.builder().setToken(IP_INFO_TOKEN).build();
    try {
      IPResponse response = ipInfo.lookupIP(ip);
      thisLogin.setLocation(
          response.getPostal() + ", " + response.getCity() + "," + response.getRegion());
    } catch (RateLimitedException ex) {
      log.error("Failed to retrieve login location due to limited rates for IPInfo.com");
      thisLogin.setLocation("Unknown");
      JSONObject body = new JSONObject();
      body.put(
          "text",
          "You are receiving this because we have arrived at maximum amount of IP "
              + "lookups we are allowed for our free plan.");
      Unirest.post(IssueController.issueReportActualURL).body(body.toString()).asEmpty();
    }
    loginList.add(thisLogin);
    user.setLogInHistory(loginList);
    userDao.update(user);
    log.info("Added login to login history");
  }

  public boolean verifyPassword(String inputPassword, String userHash) {
    SecurityUtils.PassHashEnum verifyPasswordStatus =
        SecurityUtils.verifyPassword(inputPassword, userHash);
    switch (verifyPasswordStatus) {
      case SUCCESS:
        return true;
      case ERROR:
        {
          log.error("Failed to hash password");
          return false;
        }
      case FAILURE:
        {
          log.info("Incorrect password");
          return false;
        }
    }
    return false;
  }

  public UserType getUserRole() {
    Objects.requireNonNull(user);
    return user.getUserType();
  }

  public String getOrganization() {
    Objects.requireNonNull(user);
    return user.getOrganization();
  }

  public String getUsername() {
    Objects.requireNonNull(user);
    return user.getUsername();
  }

  public String getFirstName() {
    Objects.requireNonNull(user);
    return user.getFirstName();
  }

  public String getLastName() {
    Objects.requireNonNull(user);
    return user.getLastName();
  }

  public String getFullName() {
    Objects.requireNonNull(user);
    return user.getFirstName() + " " + user.getLastName();
  }

}
