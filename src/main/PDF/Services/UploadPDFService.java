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
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDate;

public class UploadPDFService implements Service {
  public static final int CHUNK_SIZE_BYTES = 100000;

  String uploader;
  String organizationName;
  UserType privilegeLevel;
  String filename;
  String title;
  String fileContentType;
  InputStream fileStream;
  PDFType pdfType;
  MongoDatabase db;
  EncryptionController encryptionController;

  public UploadPDFService(
      MongoDatabase db,
      String uploaderUsername,
      String organizationName,
      UserType privilegeLevel,
      PDFType pdfType,
      String filename,
      String title,
      String fileContentType,
      InputStream fileStream,
      EncryptionController encryptionController) {
    this.db = db;
    this.uploader = uploaderUsername;
    this.organizationName = organizationName;
    this.privilegeLevel = privilegeLevel;
    this.pdfType = pdfType;
    this.filename = filename;
    this.title = title;
    this.fileContentType = fileContentType;
    this.fileStream = fileStream;
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    if (pdfType == null) {
      return PdfMessage.INVALID_PDF_TYPE;
    } else if (fileStream == null) {
      return PdfMessage.INVALID_PDF;
    } else if (!fileContentType.equals("application/pdf")
        && !fileContentType.equals("application/octet-stream")) {
      return PdfMessage.INVALID_PDF;
    } else {
      if ((pdfType == PDFType.APPLICATION
              || pdfType == PDFType.IDENTIFICATION
              || pdfType == PDFType.FORM)
          && (privilegeLevel == UserType.Client
              || privilegeLevel == UserType.Worker
              || privilegeLevel == UserType.Director
              || privilegeLevel == UserType.Admin
              || privilegeLevel == UserType.Developer)) {
        try {
          return mongodbUpload(
              uploader, organizationName, filename, title, fileStream, pdfType, db);
        } catch (GeneralSecurityException | IOException e) {
          return PdfMessage.SERVER_ERROR;
        }
      } else {
        return PdfMessage.INSUFFICIENT_PRIVILEGE;
      }
    }
  }

  public Message mongodbUpload(
      String uploader,
      String organizationName,
      String filename,
      String title,
      InputStream inputStream,
      PDFType pdfType,
      MongoDatabase db)
      throws GeneralSecurityException, IOException {
    inputStream = encryptionController.encryptFile(inputStream, uploader);
    GridFSBucket gridBucket = GridFSBuckets.create(db, pdfType.toString());
    GridFSUploadOptions options;
    if (pdfType == PDFType.FORM) {
      options =
          new GridFSUploadOptions()
              .chunkSizeBytes(CHUNK_SIZE_BYTES)
              .metadata(
                  new Document("type", "pdf")
                      .append("upload_date", String.valueOf(LocalDate.now()))
                      .append("title", title)
                      .append("annotated", false)
                      .append("uploader", uploader)
                      .append("organizationName", organizationName));

    } else {
      options =
          new GridFSUploadOptions()
              .chunkSizeBytes(CHUNK_SIZE_BYTES)
              .metadata(
                  new Document("type", "pdf")
                      .append("upload_date", String.valueOf(LocalDate.now()))
                      .append("uploader", uploader)
                      .append("organizationName", organizationName));
    }
    gridBucket.uploadFromStream(filename, inputStream, options);
    return PdfMessage.SUCCESS;
  }
}
