package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class AssignWorkerToUserService implements Service {
  private final UserDao userDao;
  private final String targetUserToAssign;
  private final String currentLoggedInUsername;
  private final List<String> workerUsernamesToAdd;

  public AssignWorkerToUserService(
      UserDao userDao,
      String currentLoggedInUsername,
      String targetUserToAssign,
      List<String> workerUsernamesToAdd) {
    this.userDao = userDao;
    this.targetUserToAssign = targetUserToAssign;
    this.currentLoggedInUsername = currentLoggedInUsername;
    this.workerUsernamesToAdd = workerUsernamesToAdd;
  }

  @Override
  public Message executeAndGetResponse() {
    if (currentLoggedInUsername == null) {
      log.error("Session Token Failure");
      return UserMessage.SESSION_TOKEN_FAILURE;
    }
    Optional<User> maybeTargetUser = userDao.get(targetUserToAssign);
    if (maybeTargetUser.isEmpty()) {
      return UserMessage.USER_NOT_FOUND;
    }
    User targetUser = maybeTargetUser.get();
    targetUser.setAssignedWorkerUsernames(
        getOnlyValidWorkerUsernames(workerUsernamesToAdd, targetUser.getOrganization()));
    userDao.update(targetUser);
    return UserMessage.SUCCESS;
  }

  // only retrieves valid worker usernames in the target organization
  List<String> getOnlyValidWorkerUsernames(List<String> workerUsernamesToAdd, String organization) {
    return workerUsernamesToAdd.stream()
        .map(userDao::get)
        .filter(Optional::isPresent)
        .map(Optional::get) // only return valid users
        .filter(validUser -> validUser.getOrganization().equals(organization))
        .map(validUser -> validUser.getUsername())
        .collect(Collectors.toSet()) // remove duplicates
        .stream()
        .sorted() // sort
        .collect(Collectors.toList());
  }
}
