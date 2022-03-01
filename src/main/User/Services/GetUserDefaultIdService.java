package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

@Slf4j
public class GetUserDefaultIdService implements Service{
    private final UserDao userDao;
    private final String username;
    private final String documentType;
    private String id;
    private User user;

    public GetUserDefaultIdService(UserDao userDao, String username, String documentType) {
        this.userDao = userDao;
        this.username = username;
        this.documentType = documentType;
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

        id = user.getDefaultIds().get(documentType);

        return UserMessage.SUCCESS;
    }

    public String getId(){
        return id;
    }
    public User getUser(){
        return user;
    }
}
