package File.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import File.File;
import File.FileMessage;
import User.UserType;
import Validation.ValidationUtils;
import java.util.Optional;
import org.bson.types.ObjectId;

public class RenameFileService implements Service {
  private final FileDao fileDao;
  private final String fileId;
  private final String newFilename;
  private final String orgName;
  private final UserType userType;

  public RenameFileService(
      FileDao fileDao,
      String fileId,
      String newFilename,
      String orgName,
      UserType userType) {
    this.fileDao = fileDao;
    this.fileId = fileId;
    this.newFilename = newFilename;
    this.orgName = orgName;
    this.userType = userType;
  }

  @Override
  public Message executeAndGetResponse() {
    if (!ValidationUtils.isValidObjectId(fileId)) {
      return FileMessage.INVALID_PARAMETER;
    }
    if (newFilename == null || newFilename.trim().isEmpty()) {
      return FileMessage.INVALID_PARAMETER;
    }

    if (userType != UserType.Worker
        && userType != UserType.Admin
        && userType != UserType.Director) {
      return FileMessage.INSUFFICIENT_PRIVILEGE;
    }

    ObjectId objectId = new ObjectId(fileId);
    Optional<File> maybeFile = fileDao.get(objectId);
    if (maybeFile.isEmpty()) {
      return FileMessage.NO_SUCH_FILE;
    }

    File file = maybeFile.get();

    if (!file.getOrganizationName().equals(orgName)) {
      return FileMessage.INSUFFICIENT_PRIVILEGE;
    }

    file.setFilename(newFilename.trim());
    fileDao.update(file);
    return FileMessage.SUCCESS;
  }
}
