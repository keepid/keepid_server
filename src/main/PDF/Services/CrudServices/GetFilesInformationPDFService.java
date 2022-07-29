package PDF.Services.CrudServices;

import Config.Message;
import Config.Service;
import PDF.PDFType;
import PDF.PdfMessage;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class GetFilesInformationPDFService implements Service {
  MongoDatabase db;
  private String username;
  private String orgName;
  private UserType userType;
  private PDFType pdfType;
  private JSONArray files;
  private boolean annotated;

  public GetFilesInformationPDFService(
      MongoDatabase db,
      String username,
      String orgName,
      UserType userType,
      PDFType pdfType,
      boolean annotated) {
    this.db = db;
    this.username = username;
    this.orgName = orgName;
    this.userType = userType;
    this.pdfType = pdfType;
    this.annotated = annotated;
  }

  @Override
  public Message executeAndGetResponse() {
    if (pdfType == null) {
      return PdfMessage.INVALID_PDF_TYPE;
    } else {
      return getAllFiles();
    }
  }

  public JSONArray getFiles() {
    Objects.requireNonNull(files);
    return files;
  }

  public Message getAllFiles() {
    try {
      Bson filter;
      if (pdfType == PDFType.COMPLETED_APPLICATION
          && (userType == UserType.Director
              || userType == UserType.Admin
              || userType == UserType.Worker)) {
        filter = Filters.eq("metadata.organizationName", orgName);
        return mongodbGetAllFiles(filter);
      } else if (pdfType == PDFType.COMPLETED_APPLICATION
          && (userType == UserType.Client)) {
        filter = and(eq("metadata.uploader", username));
        return mongodbGetAllFiles(filter);
      } else if (pdfType == PDFType.IDENTIFICATION_DOCUMENT && (userType == UserType.Client)) {
        filter = Filters.eq("metadata.uploader", username);
        return mongodbGetAllFiles(filter);
      } else if (pdfType == PDFType.BLANK_FORM) {
        if (userType == UserType.Developer) {
          // Getting forms that are not annotated yet
          filter = eq("metadata.annotated", annotated);
        } else {
          filter =
              and(eq("metadata.organizationName", orgName), eq("metadata.annotated", annotated));
        }
        return mongodbGetAllFiles(filter);
      } else {
        return PdfMessage.INSUFFICIENT_PRIVILEGE;
      }
    } catch (Exception e) {
      return PdfMessage.INVALID_PARAMETER;
    }
  }

  public Message mongodbGetAllFiles(Bson filter) {
    JSONArray files = new JSONArray();
    GridFSBucket gridBucket = GridFSBuckets.create(db, pdfType.toString());
    for (GridFSFile grid_out : gridBucket.find(filter)) {
      assert grid_out.getMetadata() != null;
      String uploaderUsername = grid_out.getMetadata().getString("uploader");
      JSONObject fileMetadata =
          new JSONObject()
              .put("uploader", uploaderUsername)
              .put("organizationName", grid_out.getMetadata().getString("organizationName"))
              .put("id", grid_out.getId().asObjectId().getValue().toString())
              .put("uploadDate", grid_out.getMetadata().getString("upload_date"))
              .put("annotated", annotated);
      fileMetadata.put("filename", grid_out.getMetadata().getString("title"));
      if (pdfType.equals(PDFType.BLANK_FORM)) {
        fileMetadata.put("annotated", grid_out.getMetadata().getBoolean("annotated"));
      }
      files.put(fileMetadata);
    }
    this.files = files;
    return PdfMessage.SUCCESS;
  }
}
