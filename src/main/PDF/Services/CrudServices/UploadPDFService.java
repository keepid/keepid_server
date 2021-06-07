package PDF.Services.CrudServices;

import Config.Message;
import Config.Service;
import PDF.PDFType;
import PDF.PdfController;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.bson.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
      String fileContentType,
      InputStream fileStream,
      EncryptionController encryptionController) {
    this.db = db;
    this.uploader = uploaderUsername;
    this.organizationName = organizationName;
    this.privilegeLevel = privilegeLevel;
    this.pdfType = pdfType;
    this.filename = filename;
    this.fileContentType = fileContentType;
    this.fileStream = fileStream;
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    if (pdfType == null) {
      System.out.println(3);
      return PdfMessage.INVALID_PDF_TYPE;
    } else if (fileStream == null) {
      System.out.println(4);
      return PdfMessage.INVALID_PDF;
    } else if (!fileContentType.equals("application/pdf")
        && !fileContentType.equals("application/octet-stream")) {
      System.out.println(5);
      return PdfMessage.INVALID_PDF;
    } else {
      System.out.println(2);
      if (fileContentType.equals("application/octet-stream")) {
        try {
          fileStream = convertImageToPDF(fileStream);
        } catch (IOException exception) {
          return PdfMessage.INVALID_PDF;
        }
      }
      if ((pdfType == PDFType.APPLICATION
              || pdfType == PDFType.IDENTIFICATION
              || pdfType == PDFType.FORM)
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
    System.out.println(1);
    String title = PdfController.getPDFTitle(filename, fileStream, pdfType);
    InputStream inputStream = encryptionController.encryptFile(fileStream, uploader);
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
                      .append("title", title)
                      .append("uploader", uploader)
                      .append("organizationName", organizationName));
    }
    gridBucket.uploadFromStream(filename, inputStream, options);
    return PdfMessage.SUCCESS;
  }

  public InputStream convertImageToPDF(InputStream fileStream) throws IOException {
    PDDocument document = new PDDocument();
    PDPage page = new PDPage();
    document.addPage(page);
    BufferedImage bimg = ImageIO.read(fileStream);
    PDImageXObject imageXObject = LosslessFactory.createFromImage(document, bimg);
    PDPageContentStream contentStream = new PDPageContentStream(document, page);
    contentStream.drawImage(imageXObject, 0, 0);
    contentStream.close();
    // document.save(System.getProperty("java.io.tmpdir") + File.separator + "pdfImage.pdf");
    // document.close();
    PDStream PDStream = new PDStream(document);
    InputStream inputStream = PDStream.createInputStream();
    return inputStream;
  }
}
