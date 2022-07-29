package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import lombok.extern.slf4j.Slf4j;
import User.Services.DocumentType;

import java.util.Optional;

    @Slf4j
public class SetUserDefaultIdService implements Service {
    private UserDao userDao;
    private String username;
    private DocumentType documentType;
    private String id;
    private User user;

    public SetUserDefaultIdService(UserDao userDao, String username, DocumentType documentType, String id) {
        this.userDao = userDao;
        this.username = username;
        this.documentType = documentType;
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

        user.setDefaultId(documentType, id);
        userDao.update(user);

        return UserMessage.SUCCESS;
    }

    public String getDocumentTypeId(DocumentType documentType) {
        return user.getDefaultIds().get(DocumentType.stringFromDocumentType(documentType));
    }

    public User getUser(){
        return user;
    }

}