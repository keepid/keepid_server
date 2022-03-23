package File.Services;

import Config.Message;
import Config.Service;
import File.FileMessage;
import File.FileType;
import Security.EncryptionController;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Objects;

@Slf4j
public class DownloadFileService implements Service {
  MongoDatabase db;
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
      String username,
      String orgName,
      UserType privilegeLevel,
      FileType fileType,
      String fileId,
      EncryptionController encryptionController) {
    this.db = db;
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
    GridFSBucket gridBucket = GridFSBuckets.create(db, "files");
    log.info("Attempting to download file with id {}", id);
    if (fileType.isPDF()) {
      GridFSFile grid_out = gridBucket.find(Filters.eq("_id", id)).first();
      if (grid_out == null || grid_out.getMetadata() == null) {
        return FileMessage.NO_SUCH_FILE;
      }
      if (fileType == FileType.APPLICATION_PDF
          && (privilegeLevel == UserType.Director
              || privilegeLevel == UserType.Admin
              || privilegeLevel == UserType.Worker)) {
        if (grid_out.getMetadata().getString("organizationName").equals(organizationName)) {
          this.inputStream =
              encryptionController.decryptFile(gridBucket.openDownloadStream(id), user);
          this.contentType = "application/pdf";
          return FileMessage.SUCCESS;
        }
      } else if (fileType == FileType.IDENTIFICATION_PDF
          && (privilegeLevel == UserType.Client || privilegeLevel == UserType.Worker)) {
        if (grid_out.getMetadata().getString("uploader").equals(user)) {
          this.inputStream =
              encryptionController.decryptFile(gridBucket.openDownloadStream(id), user);
          this.contentType = "application/pdf";
          return FileMessage.SUCCESS;
        }
      } else if (fileType == FileType.FORM_PDF) {
        if (grid_out.getMetadata().getString("organizationName").equals(organizationName)) {
          this.inputStream =
              encryptionController.decryptFile(gridBucket.openDownloadStream(id), user);
          this.contentType = "application/pdf";
          return FileMessage.SUCCESS;
        }
      }
    } else if (fileType.isProfilePic()) {
      // profile picture
      Bson filter =
          Filters.and(
              Filters.eq("metadata.uploader", user),
              Filters.eq("metadata.type", FileType.PROFILE_PICTURE.toString()));
      GridFSFile grid_out = gridBucket.find(filter).limit(1).first();
      if (grid_out == null || grid_out.getMetadata() == null) {
        return FileMessage.NO_SUCH_FILE; // or can send user not found
      }
      this.contentType = "image/" + grid_out.getMetadata().getString("contentType");
      this.inputStream = gridBucket.openDownloadStream(grid_out.getObjectId());
      return FileMessage.SUCCESS;
    } else {
      // miscellaneous files
      GridFSFile grid_out = gridBucket.find(Filters.eq("_id", id)).first();
      if (grid_out == null || grid_out.getMetadata() == null) {
        return FileMessage.NO_SUCH_FILE; // or can send user not found
      }
      this.contentType = "application/octet-stream";
      this.inputStream = gridBucket.openDownloadStream(grid_out.getObjectId());
      return FileMessage.SUCCESS;
    }
    return FileMessage.NO_SUCH_FILE;
  }
}
