package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class DeleteUserProfileFieldService implements Service {
    private final UserDao userDao;
    private final String username;
    private final String fieldPath;

    private static final Set<String> REQUIRED_FIELDS = new HashSet<>(Arrays.asList(
            "username",
            "email",
            "organization",
            "userType",
            "privilegeLevel",
            "password",
            "id",
            "_id",
            "creationDate",
            "currentName"
    ));

    public DeleteUserProfileFieldService(UserDao userDao, String username, String fieldPath) {
        this.userDao = userDao;
        this.username = username;
        this.fieldPath = fieldPath;
    }

    @Override
    public Message executeAndGetResponse() {
        Optional<User> optionalUser = userDao.get(username);
        if (optionalUser.isEmpty()) {
            log.error("User not found: " + username);
            return UserMessage.USER_NOT_FOUND;
        }

        if (fieldPath == null || fieldPath.trim().isEmpty()) {
            log.error("Invalid field path: empty or null");
            return UserMessage.INVALID_PARAMETER;
        }

        String rootField = fieldPath.split("\\.")[0];
        if (REQUIRED_FIELDS.contains(rootField)) {
            log.error("Cannot delete required field: " + rootField);
            return UserMessage.INVALID_PARAMETER;
        }

        try {
            userDao.deleteField(username, fieldPath);
            log.info("Successfully deleted field '{}' for user: {}", fieldPath, username);
            return UserMessage.SUCCESS;
        } catch (Exception e) {
            log.error("Error deleting field '{}' for user {}: {}", fieldPath, username, e.getMessage(), e);
            return UserMessage.AUTH_FAILURE;
        }
    }
}
