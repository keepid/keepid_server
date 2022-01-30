package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SetUserDefaultIdService implements Service {
    private UserDao userDao;
    private String username;
    private String category;
    private String id;
    private User user;

    public SetUserDefaultIdService(UserDao userDao, String username, String category, String id) {
        this.userDao = userDao;
        this.username = username;
        this.category = category;
        this.id = id;
    }

    @Override
    public Message executeAndGetResponse() {
        // checking input types
        Optional<User> optionalUser = userDao.get(username);
        if (optionalUser.isEmpty()) {
            log.info(username + " not found");
            return UserMessage.USER_NOT_FOUND;
        }
        user = optionalUser.get();

        user.setDefaultId(category, id);

        return UserMessage.SUCCESS;
    }
}