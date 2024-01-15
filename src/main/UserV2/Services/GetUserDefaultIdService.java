package UserV2.Services;

import Config.Message;
import Config.Service;
import Database.UserV2.UserDao;
import UserV2.User;
import UserV2.UserMessage;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class GetUserDefaultIdService implements Service{
    private final UserDao userDao;
    private final String username;
    private final DocumentType documentType;
    private String id;
    private User user;

    public GetUserDefaultIdService(UserDao userDao, String username, DocumentType documentType) {
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

        Map<String, String> defaultIds = user.getDefaultIds();

        return UserMessage.SUCCESS;
    }

    public String getId(DocumentType documentType) {
        return user.getDefaultIds().get(DocumentType.stringFromDocumentType(documentType));
    }
    public User getUser(){
        return user;
    }
}
