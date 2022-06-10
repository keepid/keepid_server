package File.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import File.File;
import File.FileMessage;
import File.FileType;
import Security.EncryptionController;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class DownloadFileService implements Service {
  MongoDatabase db;
  private FileDao fileDao;
  private String username;
  private String orgName;
  private UserType privilegeLevel;
  private FileType fileType;
  private String fileId;
  private String contentType;
  private InputStream inputStream;
  private EncryptionController encryptionController;

  public DownloadFileService(
      MongoDatabase db,
      FileDao fileDao,
      String username,
      String orgName,
      UserType privilegeLevel,
      FileType fileType,
      String fileId,
      EncryptionController encryptionController) {
    this.db = db;
    this.fileDao = fileDao;
    this.username = username;
    this.orgName = orgName;
    this.privilegeLevel = privilegeLevel;
    this.fileType = fileType;
    this.fileId = fileId;
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    ObjectId fileID = new ObjectId(fileId);
    if (fileType == null) {
      return FileMessage.INVALID_FILE_TYPE;
    }
    if (fileType.isPDF()) {
      if (privilegeLevel == UserType.Client
          || privilegeLevel == UserType.Worker
          || privilegeLevel == UserType.Director
          || privilegeLevel == UserType.Admin) {
        try {
          return download(username, orgName, privilegeLevel, fileID, fileType, db);
        } catch (Exception e) {
          return FileMessage.ENCRYPTION_ERROR;
        }
      } else {
        return FileMessage.INSUFFICIENT_PRIVILEGE;
      }
    } else {
      try {
        return download(username, orgName, privilegeLevel, fileID, fileType, db);
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

  public Message download(
      String user,
      String organizationName,
      UserType privilegeLevel,
      ObjectId id,
      FileType fileType,
      MongoDatabase db)
      throws GeneralSecurityException, IOException {
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
          this.inputStream = encryptionController.decryptFile(file.getFileStreamFromDB(db), user);
          this.contentType = "application/pdf";
          return FileMessage.SUCCESS;
        }
      } else if (fileType == FileType.IDENTIFICATION_PDF
          && (privilegeLevel == UserType.Client || privilegeLevel == UserType.Worker)) {
        if (file.getUsername().equals(user)) {
          this.inputStream = encryptionController.decryptFile(file.getFileStreamFromDB(db), user);
          this.contentType = "application/pdf";
          return FileMessage.SUCCESS;
        }
      } else if (fileType == FileType.FORM_PDF) {
        if (file.getOrganizationName().equals(organizationName)) {
          this.inputStream = encryptionController.decryptFile(file.getFileStreamFromDB(db), user);
          this.contentType = "application/pdf";
          return FileMessage.SUCCESS;
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
      this.inputStream = file.getFileStreamFromDB(db);
      return FileMessage.SUCCESS;
    } else {
      // miscellaneous files
      Optional<File> fileFromDB = fileDao.get(id);
      if (fileFromDB.isEmpty()) {
        return FileMessage.NO_SUCH_FILE;
      }
      File file = fileFromDB.get();
      this.contentType = file.getContentType();
      this.inputStream = file.getFileStreamFromDB(db);
      return FileMessage.SUCCESS;
    }
    return FileMessage.NO_SUCH_FILE;
  }
}
