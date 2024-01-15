package UserV2.Services;

import Config.Message;
import Config.Service;
import Database.UserV2.UserDao;
import UserV2.User;
import UserV2.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class CheckUsernameExistsService implements Service {
  UserDao userDao;
  String username;

  public CheckUsernameExistsService(UserDao userDao, String username) {
    this.userDao = userDao;
    this.username = username;
  }

  @Override
  public Message executeAndGetResponse() {
    Optional<User> user = userDao.get(username);
    if (user.isEmpty()) {
      log.info("Username not taken.");
      return UserMessage.SUCCESS;
    } else {
      log.error("Username already exists.");
      return UserMessage.USERNAME_ALREADY_EXISTS;
    }
  }
}
