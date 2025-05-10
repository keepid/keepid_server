package File.Services;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import Activity.ViewFileActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import File.File;
import File.FileMessage;
import File.FileType;
import User.UserType;
import java.util.Objects;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetFilesInformationService implements Service {
  private FileDao fileDao;
  private ActivityDao activityDao;
  private String username;
  private String orgName;
  private UserType userType;
  private FileType fileType;
  private JSONArray files;
  private boolean annotated;

  public GetFilesInformationService(
      FileDao fileDao,
      ActivityDao activityDao,
      String username,
      String orgName,
      UserType userType,
      FileType fileType,
      boolean annotated) {
    this.fileDao = fileDao;
    this.activityDao = activityDao;
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
                    eq("organizationName", orgName),
                    eq("fileType", FileType.APPLICATION_PDF.toString()));
            return getAllFiles(filter, fileType, fileDao);
          } else if (fileType == FileType.IDENTIFICATION_PDF
              && (userType == UserType.Director
                  || userType == UserType.Admin
                  || userType == UserType.Worker)) {
            filter =
                and(
                    eq("username", username),
                    eq("fileType", FileType.IDENTIFICATION_PDF.toString()));
            return getAllFiles(filter, fileType, fileDao);
          } else if (fileType == FileType.IDENTIFICATION_PDF && (userType == UserType.Client)) {
            filter =
                and(
                    eq("username", username),
                    eq("fileType", FileType.IDENTIFICATION_PDF.toString()));
            return getAllFiles(filter, fileType, fileDao);
          } else if (fileType == FileType.FORM) {
            filter =
                and(
                    eq("organizationName", orgName),
                    eq("annotated", annotated),
                    eq("fileType", FileType.FORM.toString()));
            return getAllFiles(filter, fileType, fileDao);
          } else {
            return FileMessage.INSUFFICIENT_PRIVILEGE;
          }
        } else if (!fileType.isProfilePic()) {
          // miscellaneous files
          filter = and(eq("username", username), eq("fileType", FileType.MISC.toString()));
          return getAllFiles(filter, fileType, fileDao);
        }
        return FileMessage.INVALID_FILE_TYPE;
      } catch (Exception e) {
        return FileMessage.INVALID_PARAMETER;
      }
    }
  }

  public JSONArray getFilesJSON() {
    Objects.requireNonNull(files);
    return files;
  }

  private void recordViewFileActivity(ObjectId id) {
    ViewFileActivity log =
        new ViewFileActivity(
            username, username, // Again, target vs invoker
            fileType, id); // Again, which id?
    activityDao.save(log);
  }

  public Message getAllFiles(Bson filter, FileType fileType, FileDao fileDao) {
    JSONArray files = new JSONArray();
    ObjectId id = null;
    for (File file_out : fileDao.getAll(filter)) {
      assert file_out != null;
      // Chooses first object for id
      if (id == null) {
        id = file_out.getId();
      }
      String uploaderUsername = file_out.getUsername();
      JSONObject fileMetadata =
          new JSONObject()
              .put("uploader", uploaderUsername)
              .put("id", file_out.getId().toString())
              .put("uploadDate", file_out.getUploadedAt().toString());
      if (fileType.isPDF()) {
        fileMetadata.put("organizationName", file_out.getOrganizationName());
        if (fileType == FileType.FORM) {
          String title = file_out.getFilename();
          if (title != null) {
            fileMetadata.put("filename", title);
          } else {
            fileMetadata.put("filename", file_out.getFilename());
          }
          fileMetadata.put("annotated", file_out.isAnnotated());

        } else if (fileType == FileType.APPLICATION_PDF) {
          fileMetadata.put("filename", file_out.getFilename());
        } else if (fileType == FileType.IDENTIFICATION_PDF) {
          fileMetadata.put("filename", file_out.getFilename());
          fileMetadata.put("idCategory", file_out.getIdCategory());
        }
      } else if (fileType == FileType.MISC) {
        fileMetadata.put("filename", file_out.getFilename());
      }
      files.put(fileMetadata);
    }
    this.files = files;
    recordViewFileActivity(id);
    return FileMessage.SUCCESS;
  }
}
