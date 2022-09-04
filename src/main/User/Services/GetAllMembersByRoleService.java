package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class GetAllMembersByRoleService implements Service {
  private final UserDao userDao;
  private final String orgName;
  private final UserType privilegeLevel;
  private Set<User> usersWithSpecificRole;

  public GetAllMembersByRoleService(UserDao userDao, String orgName, UserType privilegeLevel) {
    this.userDao = userDao;
    this.orgName = orgName;
    this.privilegeLevel = privilegeLevel;
    this.usersWithSpecificRole = new HashSet<>();
  }

  @Override
  public Message executeAndGetResponse() {
    if (privilegeLevel == null || orgName == null) {
      log.error("Session Token Failure");
      return UserMessage.SESSION_TOKEN_FAILURE;
    }
    this.usersWithSpecificRole =
        userDao.getAllFromOrg(orgName).stream()
            .filter(user -> user.getUserType() == privilegeLevel)
            .collect(Collectors.toSet());
    return UserMessage.SUCCESS;
  }

  public Set<JSONObject> getUsersWithSpecificRole() {
    return usersWithSpecificRole.stream().map(user -> user.serialize()).collect(Collectors.toSet());
  }
}
