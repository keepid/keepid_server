package PDF.Services.CrudServices;

import Config.Message;
import Config.Service;
import File.IdCategoryType;
import PDF.PDFType;
import PDF.PdfController;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class UploadPDFService implements Service {
  public static final int CHUNK_SIZE_BYTES = 100000;

  String uploader;
  String organizationName;
  UserType privilegeLevel;
  String filename;
  String fileContentType;
  InputStream fileStream;
  PDFType pdfType;
  MongoDatabase db;
  EncryptionController encryptionController;
  IdCategoryType idCategory;

  public UploadPDFService(
      MongoDatabase db,
      String uploaderUsername,
      String organizationName,
      UserType privilegeLevel,
      PDFType pdfType,
      String filename,
      String fileContentType,
      InputStream fileStream,
      EncryptionController encryptionController,
      IdCategoryType idCategory) {
    this.db = db;
    this.uploader = uploaderUsername;
    this.organizationName = organizationName;
    this.privilegeLevel = privilegeLevel;
    this.pdfType = pdfType;
    this.filename = filename;
    this.fileContentType = fileContentType;
    this.fileStream = fileStream;
    this.encryptionController = encryptionController;
    this.idCategory = idCategory;
  }

  @Override
  public Message executeAndGetResponse() {
    if (pdfType == null) {
      return PdfMessage.INVALID_PDF_TYPE;
    } else if (fileStream == null) {
      return PdfMessage.INVALID_PDF;
    } else if (!fileContentType.equals("application/pdf")
        && !fileContentType.startsWith("image")) {
      return PdfMessage.INVALID_PDF;
    } else {
      if (fileContentType.startsWith("image")) {
        ImageToPDFService imageToPDFService = new ImageToPDFService(fileStream);
        Message response = imageToPDFService.executeAndGetResponse();
        if (response == PdfMessage.INVALID_PDF) return response;

        fileStream = imageToPDFService.getFileStream();
        filename = filename.substring(0, filename.lastIndexOf(".")) + ".pdf";
      }
      if ((pdfType == PDFType.COMPLETED_APPLICATION
              || pdfType == PDFType.IDENTIFICATION_DOCUMENT
              || pdfType == PDFType.BLANK_FORM)
          && (privilegeLevel == UserType.Client
              || privilegeLevel == UserType.Worker
              || privilegeLevel == UserType.Director
              || privilegeLevel == UserType.Admin
              || privilegeLevel == UserType.Developer)) {
        try {
          return mongodbUpload();
        } catch (GeneralSecurityException | IOException e) {
          return PdfMessage.SERVER_ERROR;
        }
      } else {
        return PdfMessage.INSUFFICIENT_PRIVILEGE;
      }
    }
  }

  public Message mongodbUpload() throws GeneralSecurityException, IOException {
    String title = PdfController.getPDFTitle(filename, fileStream, pdfType);
    GridFSBucket gridBucket = GridFSBuckets.create(db, pdfType.toString());
    InputStream inputStream;
    String uploadDate = Instant.now().atZone(ZoneId.systemDefault()).toString();
    uploadDate = uploadDate.replace("T", " ");
    uploadDate = uploadDate.substring(0, uploadDate.indexOf(".")); // Get part before period

    Document metadata =
      new Document("type", "pdf")
        .append("upload_date", uploadDate)
        .append("title", title)
        .append("uploader", uploader)
        .append("organizationName", organizationName);

    if (pdfType == PDFType.BLANK_FORM) {
      inputStream = fileStream;
      metadata = metadata.append("annotated", false);
    } else if (pdfType == PDFType.IDENTIFICATION_DOCUMENT){
      inputStream = encryptionController.encryptFile(fileStream, uploader);
      metadata = metadata.append("idCategory", idCategory.toString());
    } else {
      // pdfType == PDFType.COMPLETED_APPLICATION
      inputStream = encryptionController.encryptFile(fileStream, uploader);
    }

    GridFSUploadOptions options =
      new GridFSUploadOptions()
          .chunkSizeBytes(CHUNK_SIZE_BYTES)
          .metadata(metadata);

    gridBucket.uploadFromStream(filename, inputStream, options);
    inputStream.close();
    return PdfMessage.SUCCESS;
  }
}
