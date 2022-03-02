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
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.bson.Document;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
      return PdfMessage.INVALID_PDF_TYPE;
    } else if (fileStream == null) {
      return PdfMessage.INVALID_PDF;
    } else if (!fileContentType.equals("application/pdf")
        && !fileContentType.equals("application/octet-stream")
        && !fileContentType.startsWith("image")) {
      return PdfMessage.INVALID_PDF;
    } else {
      if (fileContentType.startsWith("image")) {
        try {
          fileStream = convertImageToPDF(fileStream);
          filename = filename.substring(0, filename.lastIndexOf(".")) + ".pdf";
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
    String title = PdfController.getPDFTitle(filename, fileStream, pdfType);
    GridFSBucket gridBucket = GridFSBuckets.create(db, pdfType.toString());
    GridFSUploadOptions options;
    InputStream inputStream;

    if (pdfType == PDFType.FORM) {
      inputStream = fileStream;
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
      inputStream = encryptionController.encryptFile(fileStream, uploader);
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
    final int PDFHEIGHT = 792;
    final int PDFWIDTH = 612;

    PDDocument document = new PDDocument();
    PDPage page = new PDPage();
    document.addPage(page);
    BufferedImage bimg = ImageIO.read(fileStream);
    System.out.println(bimg.getWidth() + ": " + bimg.getHeight());
    float imageWidth = bimg.getWidth();
    float imageHeight = bimg.getHeight();

    if (imageWidth > imageHeight) {
      bimg = rotateImage(bimg);
      float tmp = imageWidth;
      imageWidth = imageHeight;
      imageHeight = tmp;
    }

    float finalWidth, finalHeight;
    if (imageWidth / PDFWIDTH > imageHeight / PDFHEIGHT) {
      finalWidth = PDFWIDTH;
      finalHeight = imageHeight * PDFWIDTH / imageWidth;
    } else {
      finalHeight = PDFHEIGHT;
      finalWidth = imageWidth * PDFHEIGHT / imageHeight;
    }

    //    Matrix transformationMatrix;
    //    if (imageHeight >= imageWidth) {
    //      // Normal Orientation
    //      transformationMatrix = new Matrix(PDFWIDTH / imageWidth, 0, 0, PDFHEIGHT / imageHeight,
    // 0, 0);
    //    } else {
    //      // Need to rotate
    //      transformationMatrix =
    //          new Matrix(0, PDFHEIGHT / imageWidth, -1 * PDFWIDTH / imageHeight, 0, 0, 0);
    //      transformationMatrix = new Matrix(1f, 0f, 1f, 0f, 0f, 0f);
    //    }

    PDImageXObject imageXObject = LosslessFactory.createFromImage(document, bimg);
    PDPageContentStream contentStream = new PDPageContentStream(document, page);
    contentStream.drawImage(imageXObject, 0, 0, finalWidth, finalHeight);
    contentStream.close();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    document.save(out);
    document.close();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());
    return inputStream;
  }

  public BufferedImage rotateImage(BufferedImage bimg) {
    // https://blog.idrsolutions.com/2019/05/image-rotation-in-java/
    final double rads = Math.toRadians(90);
    final double sin = Math.abs(Math.sin(rads));
    final double cos = Math.abs(Math.cos(rads));
    final int w = (int) Math.floor(bimg.getWidth() * cos + bimg.getHeight() * sin);
    final int h = (int) Math.floor(bimg.getHeight() * cos + bimg.getWidth() * sin);
    final BufferedImage rotatedImage = new BufferedImage(w, h, bimg.getType());
    final AffineTransform at = new AffineTransform();
    at.translate(w / 2, h / 2);
    at.rotate(rads, 0, 0);
    at.translate(-bimg.getWidth() / 2, -bimg.getHeight() / 2);
    final AffineTransformOp rotateOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
    rotateOp.filter(bimg, rotatedImage);
    return rotatedImage;
  }
}
