package PDF.Services.CrudServices;

import Config.Message;
import Config.Service;
import PDF.PDFType;
import PDF.PdfMessage;
import User.UserType;
import Validation.ValidationUtils;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;

public class DeletePDFService implements Service {
  MongoDatabase db;
  private String username;
  private String orgName;
  private UserType userType;
  private PDFType pdfType;
  private String fileId;

  public DeletePDFService(
      MongoDatabase db,
      String username,
      String orgName,
      UserType userType,
      PDFType pdfType,
      String fileId) {
    this.db = db;
    this.username = username;
    this.orgName = orgName;
    this.userType = userType;
    this.pdfType = pdfType;
    this.fileId = fileId;
  }

  @Override
  public Message executeAndGetResponse() {
    if (!ValidationUtils.isValidObjectId(fileId) || pdfType == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    if (!ValidationUtils.isValidObjectId(fileId) || pdfType == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    return delete();
  }

  public Message delete() {
    ObjectId id = new ObjectId(fileId);
    GridFSBucket gridBucket = GridFSBuckets.create(db, pdfType.toString());
    GridFSFile grid_out = gridBucket.find(Filters.eq("_id", id)).first();
    if (grid_out == null || grid_out.getMetadata() == null) {
      return PdfMessage.NO_SUCH_FILE;
    }
    if (pdfType == PDFType.COMPLETED_APPLICATION
        && (userType == UserType.Admin
            || userType == UserType.Director
            || userType == UserType.Worker)) {
      if (grid_out.getMetadata().getString("organizationName").equals(orgName)) {
        gridBucket.delete(id);
        return PdfMessage.SUCCESS;
      }
    } else if (pdfType == PDFType.IDENTIFICATION_DOCUMENT
        && (userType == UserType.Client || userType == UserType.Worker)) {
      if (grid_out.getMetadata().getString("uploader").equals(username)) {
        gridBucket.delete(id);
        return PdfMessage.SUCCESS;
      }
    } else if (pdfType == PDFType.BLANK_FORM) {
      if (grid_out.getMetadata().getString("organizationName").equals(orgName)) {
        gridBucket.delete(id);
        return PdfMessage.SUCCESS;
      }
    }
    return PdfMessage.INSUFFICIENT_PRIVILEGE;
  }
}
