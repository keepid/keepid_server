package File.Services;

import Config.Message;
import Config.Service;
import File.FileMessage;
import File.FileType;
import User.UserType;
import Validation.ValidationUtils;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;

public class DeleteFileService implements Service {
  MongoDatabase db;
  private String username;
  private String orgName;
  private UserType userType;
  private FileType fileType;
  private String fileId;

  public DeleteFileService(
      MongoDatabase db,
      String username,
      String orgName,
      UserType userType,
      FileType fileType,
      String fileId) {
    this.db = db;
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
    return delete(username, orgName, fileType, userType, fileID, db);
  }

  public Message delete(
      String user,
      String organizationName,
      FileType fileType,
      UserType privilegeLevel,
      ObjectId id,
      MongoDatabase db) {
    GridFSBucket gridBucket = GridFSBuckets.create(db, "files");
    GridFSFile grid_out = gridBucket.find(Filters.eq("_id", id)).first();
    if (grid_out == null || grid_out.getMetadata() == null) {
      return FileMessage.NO_SUCH_FILE;
    }
    if (fileType == FileType.APPLICATION_PDF
        && (privilegeLevel == UserType.Admin
            || privilegeLevel == UserType.Director
            || privilegeLevel == UserType.Worker)) {
      if (grid_out.getMetadata().getString("organizationName").equals(organizationName)) {
        gridBucket.delete(id);
        return FileMessage.SUCCESS;
      }
    } else if (fileType == FileType.IDENTIFICATION_PDF
        && (privilegeLevel == UserType.Client || privilegeLevel == UserType.Worker)) {
      if (grid_out.getMetadata().getString("uploader").equals(user)) {
        gridBucket.delete(id);
        return FileMessage.SUCCESS;
      }
    } else if (fileType == FileType.FORM_PDF) {
      if (grid_out.getMetadata().getString("organizationName").equals(organizationName)) {
        gridBucket.delete(id);
        return FileMessage.SUCCESS;
      }
    } else if (fileType == FileType.MISC) { // need to establish security levels for MISC files
      if (grid_out.getMetadata().getString("uploader").equals(user)) {
        gridBucket.delete(id);
        return FileMessage.SUCCESS;
      }
    } // no deleting of profile pic files (only replacing)
    return FileMessage.INSUFFICIENT_PRIVILEGE;
  }
}
