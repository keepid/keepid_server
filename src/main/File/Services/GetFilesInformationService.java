package File.Services;

import Config.Message;
import Config.Service;
import File.FileMessage;
import File.FileType;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class GetFilesInformationService implements Service {
  MongoDatabase db;
  private String username;
  private String orgName;
  private UserType userType;
  private FileType fileType;
  private JSONArray files;
  private boolean annotated;

  public GetFilesInformationService(
      MongoDatabase db,
      String username,
      String orgName,
      UserType userType,
      FileType fileType,
      boolean annotated) {
    this.db = db;
    this.username = username;
    this.orgName = orgName;
    this.userType = userType;
    this.fileType = fileType;
    this.annotated = annotated;
  }

  @Override
  public Message executeAndGetResponse() {
    if (fileType == null) {
      return FileMessage.INVALID_FILE_TYPE;
    } else {
      try {
        Bson filter = null;
        if (fileType.isPDF()) {
          if (fileType == FileType.APPLICATION_PDF
              && (userType == UserType.Director
                  || userType == UserType.Admin
                  || userType == UserType.Worker)) {
            filter =
                and(
                    eq("metadata.organizationName", orgName),
                    eq("metadata.type", FileType.APPLICATION_PDF.toString()));
            return mongoDBGetAllFiles(filter, fileType, db);
          } else if (fileType == FileType.IDENTIFICATION_PDF && (userType == UserType.Client)) {
            filter =
                and(
                    eq("metadata.uploader", username),
                    eq("metadata.type", FileType.IDENTIFICATION_PDF.toString()));
            return mongoDBGetAllFiles(filter, fileType, db);
          } else if (fileType == FileType.FORM_PDF) {
            filter =
                and(
                    eq("metadata.organizationName", orgName),
                    eq("metadata.annotated", annotated),
                    eq("metadata.type", FileType.FORM_PDF.toString()));
            return mongoDBGetAllFiles(filter, fileType, db);
          } else {
            return FileMessage.INSUFFICIENT_PRIVILEGE;
          }
        } else if (!fileType.isProfilePic()) {
          // miscellaneous files
          filter =
              and(eq("metadata.uploader", username), eq("metadata.type", FileType.MISC.toString()));
          return mongoDBGetAllFiles(filter, fileType, db);
        }
        return FileMessage.INVALID_FILE_TYPE;
      } catch (Exception e) {
        return FileMessage.INVALID_PARAMETER;
      }
    }
  }

  public JSONArray getFiles() {
    Objects.requireNonNull(files);
    return files;
  }

  public Message mongoDBGetAllFiles(Bson filter, FileType fileType, MongoDatabase db) {
    JSONArray files = new JSONArray();
    GridFSBucket gridBucket = GridFSBuckets.create(db, "files");
    for (GridFSFile grid_out : gridBucket.find(filter)) {
      assert grid_out.getMetadata() != null;
      String uploaderUsername = grid_out.getMetadata().getString("uploader");
      JSONObject fileMetadata =
          new JSONObject()
              .put("uploader", uploaderUsername)
              .put("id", grid_out.getId().asObjectId().getValue().toString())
              .put("uploadDate", grid_out.getUploadDate().toString());
      if (fileType.isPDF()) {
        fileMetadata.put("organizationName", grid_out.getMetadata().getString("organizationName"));
        fileMetadata.put("annotated", annotated);
        if (fileType == FileType.FORM_PDF) {
          String title = grid_out.getMetadata().getString("title");
          if (title != null) {
            fileMetadata.put("filename", title);
          } else {
            fileMetadata.put("filename", grid_out.getFilename());
          }
          fileMetadata.put("annotated", grid_out.getMetadata().getBoolean("annotated"));

        } else if (fileType == FileType.APPLICATION_PDF
            || fileType == FileType.IDENTIFICATION_PDF) {
          fileMetadata.put("filename", grid_out.getFilename());
        }
      } else if (fileType == FileType.MISC) {
        fileMetadata.put("filename", grid_out.getFilename());
      }
      files.put(fileMetadata);
    }
    this.files = files;
    return FileMessage.SUCCESS;
  }
}
