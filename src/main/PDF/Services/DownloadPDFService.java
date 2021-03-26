package PDF.Services;

import Config.Message;
import Config.Service;
import PDF.PDFType;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Objects;

public class DownloadPDFService implements Service {
  MongoDatabase db;
  private String username;
  private String orgName;
  private UserType privilegeLevel;
  private PDFType pdfType;
  private String fileId;
  private InputStream inputStream;
  private EncryptionController encryptionController;

  public DownloadPDFService(
      MongoDatabase db,
      String username,
      String orgName,
      UserType privilegeLevel,
      String fileId,
      PDFType pdfType,
      EncryptionController encryptionController) {
    this.db = db;
    this.username = username;
    this.orgName = orgName;
    this.privilegeLevel = privilegeLevel;
    this.pdfType = pdfType;
    this.fileId = fileId;
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    if (pdfType == null) {
      return PdfMessage.INVALID_PDF;
    }
    if (privilegeLevel == UserType.Client
        || privilegeLevel == UserType.Worker
        || privilegeLevel == UserType.Director
        || privilegeLevel == UserType.Admin) {
      try {
        return download();
      } catch (Exception e) {
        return PdfMessage.ENCRYPTION_ERROR;
      }

    } else {
      return PdfMessage.INSUFFICIENT_PRIVILEGE;
    }
  }

  public InputStream getInputStream() {
    Objects.requireNonNull(inputStream);
    return inputStream;
  }

  public Message download() throws GeneralSecurityException, IOException {
    ObjectId id = new ObjectId(fileId);
    GridFSBucket gridBucket = GridFSBuckets.create(db, pdfType.toString());
    GridFSFile grid_out = gridBucket.find(Filters.eq("_id", id)).first();
    if (grid_out == null || grid_out.getMetadata() == null) {
      return PdfMessage.NO_SUCH_FILE;
    }
    if (pdfType == PDFType.APPLICATION
        && (privilegeLevel == UserType.Director
            || privilegeLevel == UserType.Admin
            || privilegeLevel == UserType.Worker)) {
      if (grid_out.getMetadata().getString("organizationName").equals(orgName)) {
        this.inputStream =
            encryptionController.decryptFile(gridBucket.openDownloadStream(id), username);
        return PdfMessage.SUCCESS;
      }
    } else if (pdfType == PDFType.IDENTIFICATION
        && (privilegeLevel == UserType.Client || privilegeLevel == UserType.Worker)) {
      if (grid_out.getMetadata().getString("uploader").equals(username)) {
        this.inputStream =
            encryptionController.decryptFile(gridBucket.openDownloadStream(id), username);
        return PdfMessage.SUCCESS;
      }
    } else if (pdfType == PDFType.FORM) {
      if (grid_out.getMetadata().getString("organizationName").equals(orgName)) {
        this.inputStream = gridBucket.openDownloadStream(id);
        return PdfMessage.SUCCESS;
      }
    }
    return PdfMessage.INVALID_PDF_TYPE;
  }
}
