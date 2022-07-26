package File.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import File.File;
import File.FileMessage;
import File.FileType;
import Security.EncryptionController;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class DownloadFileService implements Service {
  private FileDao fileDao;
  private String username;
  private Optional<String> organizationName;
  private Optional<UserType> privilegeLevel;
  private FileType fileType;
  private Optional<String> fileId;
  private String contentType;
  private InputStream inputStream;
  private Optional<EncryptionController> encryptionController;

  public DownloadFileService(
      FileDao fileDao,
      String username,
      Optional<String> orgName,
      Optional<UserType> privilegeLevel,
      FileType fileType,
      Optional<String> fileId,
      Optional<EncryptionController> encryptionController) {
    this.fileDao = fileDao;
    this.username = username;
    this.organizationName = orgName;
    this.privilegeLevel = privilegeLevel;
    this.fileType = fileType;
    this.fileId = fileId;
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    if (fileType == null) {
      return FileMessage.INVALID_FILE_TYPE;
    }
    //quick fix to make sure everyone can access profile pictures. edit later
    if (fileType.isProfilePic()) this.privilegeLevel = Optional.of(UserType.Worker);
    if (privilegeLevel.isEmpty()) {
      return FileMessage.INSUFFICIENT_PRIVILEGE;
    }
    UserType privilegeLevelType = privilegeLevel.get();
    if (fileType.isPDF()) {
      if (privilegeLevelType == UserType.Client
          || privilegeLevelType == UserType.Worker
          || privilegeLevelType == UserType.Director
          || privilegeLevelType == UserType.Admin) {
        try {
          return download();
        } catch (Exception e) {
          return FileMessage.ENCRYPTION_ERROR;
        }
      } else {
        return FileMessage.INSUFFICIENT_PRIVILEGE;
      }
    } else {
      try {
        return download();
      } catch (Exception e) {
        log.info(e.toString());
        return FileMessage.ENCRYPTION_ERROR;
      }
    }
  }

  public InputStream getInputStream() {
    Objects.requireNonNull(inputStream);
    return inputStream;
  }

  public String getContentType() {
    return contentType;
  }

  public Message download() throws GeneralSecurityException, IOException {
    if (fileType.isPDF()) {
      if (fileId.isEmpty() || organizationName.isEmpty()) {
        return FileMessage.INVALID_PARAMETER;
      }
      if (encryptionController.isEmpty()) {
        return FileMessage.SERVER_ERROR;
      }
      ObjectId id = new ObjectId(fileId.get());
      log.info("Attempting to download file with id {}", id);
      Optional<File> fileFromDB = fileDao.get(id);
      if (fileFromDB.isEmpty()) {
        return FileMessage.NO_SUCH_FILE;
      }
      File file = fileFromDB.get();
      UserType privilegeLevelType = privilegeLevel.get();
      if (fileType == FileType.APPLICATION_PDF
          && (privilegeLevelType == UserType.Director
              || privilegeLevelType == UserType.Admin
              || privilegeLevelType == UserType.Worker)) {
        if (file.getOrganizationName().equals(organizationName.get())) {
          Optional<InputStream> optionalStream = fileDao.getStream(id);
          if (optionalStream.isPresent()) {
            this.inputStream =
                encryptionController.get().decryptFile(optionalStream.get(), this.username);
            this.contentType = "application/pdf";
            return FileMessage.SUCCESS;
          }
          return FileMessage.NO_SUCH_FILE;
        }
      } else if (fileType == FileType.IDENTIFICATION_PDF
          && (privilegeLevelType == UserType.Client || privilegeLevelType == UserType.Worker)) {
        if (file.getUsername().equals(username)) {
          Optional<InputStream> optionalStream = fileDao.getStream(id);
          if (optionalStream.isPresent()) {
            this.inputStream =
                encryptionController.get().decryptFile(optionalStream.get(), this.username);
            this.contentType = "application/pdf";
            return FileMessage.SUCCESS;
          }
          return FileMessage.NO_SUCH_FILE;
        }
      } else if (fileType == FileType.FORM_PDF) {
        if (file.getOrganizationName().equals(organizationName.get())) {
          Optional<InputStream> optionalStream = fileDao.getStream(id);
          if (optionalStream.isPresent()) {
            this.inputStream =
                encryptionController.get().decryptFile(optionalStream.get(), this.username);
            this.contentType = "application/pdf";
            return FileMessage.SUCCESS;
          }
          return FileMessage.NO_SUCH_FILE;
        }
      }
    } else if (fileType.isProfilePic()) {
      // profile picture only requires username
      Optional<File> fileFromDB = fileDao.get(this.username, fileType);
      if (fileFromDB.isEmpty()) {
        return FileMessage.NO_SUCH_FILE;
      }
      File file = fileFromDB.get();
      this.contentType = "image/" + file.getContentType();
      Optional<InputStream> optionalStream = fileDao.getStream(file.getId());
      if (optionalStream.isPresent()) {
        this.inputStream = optionalStream.get();
        return FileMessage.SUCCESS;
      }
      return FileMessage.NO_SUCH_FILE;
    } else {
      // miscellaneous files
      if (fileId.isEmpty()) {
        return FileMessage.INVALID_PARAMETER;
      }
      ObjectId id = new ObjectId(fileId.get());
      Optional<File> fileFromDB = fileDao.get(id);
      if (fileFromDB.isEmpty()) {
        return FileMessage.NO_SUCH_FILE;
      }
      File file = fileFromDB.get();
      this.contentType = file.getContentType();
      Optional<InputStream> optionalStream = fileDao.getStream(id);
      if (optionalStream.isPresent()) {
        this.inputStream = optionalStream.get();
        return FileMessage.SUCCESS;
      }
      return FileMessage.NO_SUCH_FILE;
    }
    return FileMessage.NO_SUCH_FILE;
  }
}
