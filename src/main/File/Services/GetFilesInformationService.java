package File.Services;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.or;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import File.File;
import File.FileMessage;
import File.FileType;
import Form.Form;
import User.UserType;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetFilesInformationService implements Service {
  private FileDao fileDao;
  private String username;
  private String orgName;
  private ObjectId organizationId;
  private UserType userType;
  private FileType fileType;
  private JSONArray files;
  private boolean annotated;
  private FormDao formDao;

  public GetFilesInformationService(
      FileDao fileDao,
      String username,
      String orgName,
      ObjectId organizationId,
      UserType userType,
      FileType fileType,
      boolean annotated,
      FormDao formDao) {
    this.fileDao = fileDao;
    this.username = username;
    this.orgName = orgName;
    this.organizationId = organizationId;
    this.userType = userType;
    this.fileType = fileType;
    this.annotated = annotated;
    this.formDao = formDao;
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
                organizationId != null
                    ? and(
                        eq("organizationId", organizationId),
                        eq("fileType", FileType.APPLICATION_PDF.toString()))
                    : and(
                        eq("organizationName", orgName),
                        eq("fileType", FileType.APPLICATION_PDF.toString()));
            return getAllFiles(filter, fileType, fileDao);
          } else if (fileType == FileType.APPLICATION_PDF
              && userType == UserType.Client) {
            filter =
                and(
                    eq("username", username),
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
                organizationId != null
                    ? and(
                        eq("organizationId", organizationId),
                        eq("annotated", annotated),
                        eq("fileType", FileType.FORM.toString()))
                    : and(
                        eq("organizationName", orgName),
                        eq("annotated", annotated),
                        eq("fileType", FileType.FORM.toString()));
            return getAllFiles(filter, fileType, fileDao);
          } else if (fileType == FileType.ORG_DOCUMENT) {
            Bson applicationScopeFilter =
                or(eq("applicationScopedAttachment", false), exists("applicationScopedAttachment", false));
            filter =
                organizationId != null
                    ? and(
                        eq("organizationId", organizationId),
                        eq("fileType", FileType.ORG_DOCUMENT.toString()),
                        applicationScopeFilter)
                    : and(
                        eq("organizationName", orgName),
                        eq("fileType", FileType.ORG_DOCUMENT.toString()),
                        applicationScopeFilter);
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

  public Message getAllFiles(Bson filter, FileType fileType, FileDao fileDao) {
    JSONArray files = new JSONArray();
    for (File file_out : fileDao.getAll(filter)) {
      assert file_out != null;
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
          String applicationDisplayName = resolveApplicationDisplayName(file_out);
          if (applicationDisplayName != null && !applicationDisplayName.isBlank()) {
            fileMetadata.put("applicationDisplayName", applicationDisplayName);
          }
          if (file_out.getPacketId() != null) {
            fileMetadata.put("packetId", file_out.getPacketId().toString());
          }
        } else if (fileType == FileType.ORG_DOCUMENT) {
          fileMetadata.put("filename", file_out.getFilename());
        } else if (fileType == FileType.IDENTIFICATION_PDF) {
          fileMetadata.put("filename", file_out.getFilename());
          fileMetadata.put("idCategory", file_out.getIdCategory());
          if (file_out.getIdCategory() != null) {
            fileMetadata.put("idCategoryDisplay", file_out.getIdCategory().toString());
          }
          fileMetadata.put("customIdCategory", file_out.getCustomIdCategory());
        }
      } else if (fileType == FileType.MISC) {
        fileMetadata.put("filename", file_out.getFilename());
      }
      files.put(fileMetadata);
    }
    this.files = files;
    return FileMessage.SUCCESS;
  }

  private String resolveApplicationDisplayName(File file) {
    if (file == null || this.formDao == null) {
      return null;
    }
    Optional<Form> formOpt = formDao.getByFileId(file.getId());
    if (formOpt.isEmpty()) {
      return null;
    }
    Form form = formOpt.get();
    Map<String, String> metadata = form.getApplicationMetadata();
    if (metadata == null || metadata.isEmpty()) {
      String metadataTitle = form.getMetadata() != null ? form.getMetadata().getTitle() : null;
      if (metadataTitle == null || metadataTitle.isBlank()) {
        return null;
      }
      String trimmed = metadataTitle.trim();
      if (trimmed.toLowerCase().endsWith(" form")) {
        trimmed = trimmed.substring(0, trimmed.length() - 5).trim();
      }
      return trimmed.isBlank() ? null : trimmed;
    }
    String[] keysToTry =
        new String[] {"applicationDisplayName", "applicationName", "applicationTitle", "title"};
    for (String key : keysToTry) {
      String value = metadata.get(key);
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }
}
