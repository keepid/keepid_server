package File.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import File.File;
import File.FileMessage;
import File.FileType;
import User.UserType;
import Validation.ValidationUtils;
import org.bson.types.ObjectId;

import java.util.Optional;

public class DeleteFileService implements Service {
  private FileDao fileDao;
  private String username;
  private String orgName;
  private UserType userType;
  private FileType fileType;
  private String fileId;

  public DeleteFileService(
      FileDao fileDao,
      String username,
      String orgName,
      UserType userType,
      FileType fileType,
      String fileId) {
    this.fileDao = fileDao;
    this.username = username;
    this.orgName = orgName;
    this.userType = userType;
    this.fileType = fileType;
    this.fileId = fileId;
  }

  @Override
  public Message executeAndGetResponse() {
    if (!ValidationUtils.isValidObjectId(fileId) || fileType == null) {
      return FileMessage.INVALID_PARAMETER;
    }
    ObjectId fileID = new ObjectId(fileId);
    return delete(username, orgName, fileType, userType, fileID, fileDao);
  }

  public Message delete(
      String user,
      String organizationName,
      FileType fileType,
      UserType privilegeLevel,
      ObjectId id,
      FileDao fileDao) {
    Optional<File> fileFromDB = fileDao.get(id);
    if (fileFromDB.isEmpty()) {
      return FileMessage.NO_SUCH_FILE;
    }
    File file = fileFromDB.get();
    if (fileType == FileType.APPLICATION_PDF
        && (privilegeLevel == UserType.Admin
            || privilegeLevel == UserType.Director
            || privilegeLevel == UserType.Worker)) {
      if (file.getOrganizationName().equals(organizationName)) {
        fileDao.delete(id);
        return FileMessage.SUCCESS;
      }
    } else if (fileType == FileType.IDENTIFICATION_PDF
        && (privilegeLevel == UserType.Client || privilegeLevel == UserType.Worker)) {
      if (file.getUsername().equals(user)) {
        fileDao.delete(id);
        return FileMessage.SUCCESS;
      }
    } else if (fileType == FileType.FORM_PDF) {
      if (file.getOrganizationName().equals(organizationName)) {
        fileDao.delete(id);
        return FileMessage.SUCCESS;
      }
    } else if (fileType == FileType.MISC) { // need to establish security levels for MISC files
      if (file.getUsername().equals(user)) {
        fileDao.delete(id);
        return FileMessage.SUCCESS;
      }
    } // no deleting of profile pic files (only replacing)
    return FileMessage.INSUFFICIENT_PRIVILEGE;
  }
}
