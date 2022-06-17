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
  private String organizationName;
  private UserType privilegeLevel;
  private FileType fileType;
  private String fileId;
  private String contentType;
  private InputStream inputStream;
  private EncryptionController encryptionController;

  public DownloadFileService(
      FileDao fileDao,
      String username,
      String orgName,
      UserType privilegeLevel,
      FileType fileType,
      String fileId,
      EncryptionController encryptionController) {
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
    if (fileType.isPDF()) {
      if (privilegeLevel == UserType.Client
          || privilegeLevel == UserType.Worker
          || privilegeLevel == UserType.Director
          || privilegeLevel == UserType.Admin) {
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
    ObjectId id = new ObjectId(fileId);
    log.info("Attempting to download file with id {}", id);
    if (fileType.isPDF()) {
      Optional<File> fileFromDB = fileDao.get(id);
      if (fileFromDB.isEmpty()) {
        return FileMessage.NO_SUCH_FILE;
      }
      File file = fileFromDB.get();
      if (fileType == FileType.APPLICATION_PDF
          && (privilegeLevel == UserType.Director
              || privilegeLevel == UserType.Admin
              || privilegeLevel == UserType.Worker)) {
        if (file.getOrganizationName().equals(organizationName)) {
          Optional<InputStream> optionalStream = fileDao.getStream(id);
          if (optionalStream.isPresent()) {
            this.inputStream =
                encryptionController.decryptFile(optionalStream.get(), this.username);
            this.contentType = "application/pdf";
            return FileMessage.SUCCESS;
          }
          return FileMessage.NO_SUCH_FILE;
        }
      } else if (fileType == FileType.IDENTIFICATION_PDF
          && (privilegeLevel == UserType.Client || privilegeLevel == UserType.Worker)) {
        if (file.getUsername().equals(username)) {
          Optional<InputStream> optionalStream = fileDao.getStream(id);
          if (optionalStream.isPresent()) {
            this.inputStream =
                encryptionController.decryptFile(optionalStream.get(), this.username);
            this.contentType = "application/pdf";
            return FileMessage.SUCCESS;
          }
          return FileMessage.NO_SUCH_FILE;
        }
      } else if (fileType == FileType.FORM_PDF) {
        if (file.getOrganizationName().equals(organizationName)) {
          Optional<InputStream> optionalStream = fileDao.getStream(id);
          if (optionalStream.isPresent()) {
            this.inputStream =
                encryptionController.decryptFile(optionalStream.get(), this.username);
            this.contentType = "application/pdf";
            return FileMessage.SUCCESS;
          }
          return FileMessage.NO_SUCH_FILE;
        }
      }
    } else if (fileType.isProfilePic()) {
      // profile picture
      Optional<File> fileFromDB = fileDao.get(this.username, FileType.PROFILE_PICTURE);
      if (fileFromDB.isEmpty()) {
        return FileMessage.NO_SUCH_FILE;
      }
      File file = fileFromDB.get();
      this.contentType = "image/" + file.getContentType();
      Optional<InputStream> optionalStream = fileDao.getStream(id);
      if (optionalStream.isPresent()) {
        this.inputStream = optionalStream.get();
        return FileMessage.SUCCESS;
      }
      return FileMessage.NO_SUCH_FILE;
    } else {
      // miscellaneous files
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
