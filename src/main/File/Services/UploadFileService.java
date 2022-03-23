package File.Services;

import Config.Message;
import Config.Service;
import File.FileMessage;
import File.FileType;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.UserMessage;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class UploadFileService implements Service {
  public static final int CHUNK_SIZE_BYTES = 100000;

  String uploader;
  String organizationName;
  UserType privilegeLevel;
  String filename;
  String title;
  String fileContentType;
  String fileIdStr;
  boolean annotated;
  boolean toSign;
  InputStream fileStream;
  InputStream signatureFileStream;
  FileType fileType;
  MongoDatabase db;
  EncryptionController encryptionController;

  public UploadFileService(
      MongoDatabase db,
      String uploaderUsername,
      String organizationName,
      UserType privilegeLevel,
      FileType fileType,
      String filename,
      String title,
      String fileContentType,
      String fileIdStr,
      boolean annotated,
      boolean toSign,
      InputStream fileStream,
      InputStream signatureFileStream,
      EncryptionController encryptionController) {
    this.db = db;
    this.uploader = uploaderUsername;
    this.organizationName = organizationName;
    this.privilegeLevel = privilegeLevel;
    this.fileType = fileType;
    this.filename = filename;
    this.annotated = annotated;
    this.toSign = toSign;
    this.title = title;
    this.fileContentType = fileContentType;
    this.fileIdStr = fileIdStr;
    this.fileStream = fileStream;
    this.signatureFileStream = signatureFileStream;
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    if (fileType == null) {
      return FileMessage.INVALID_FILE_TYPE;
    } else if (fileStream == null) {
      return FileMessage.INVALID_FILE;
    } else if (fileType.isPDF()) {
      if (!fileContentType.equals("application/pdf")
          && !fileContentType.equals("application/octet-stream")) {
        return FileMessage.INVALID_FILE_TYPE;
      }
      if (privilegeLevel == UserType.Client
          || privilegeLevel == UserType.Worker
          || privilegeLevel == UserType.Director
          || privilegeLevel == UserType.Admin
          || privilegeLevel == UserType.Developer) {
        try {
          if (this.toSign) {
            this.fileStream = signFile();
          }
          return mongodbUploadPDF();
        } catch (GeneralSecurityException | IOException e) {
          return FileMessage.SERVER_ERROR;
        }
      } else {
        log.info("Privilege level: {}", privilegeLevel.toString());
        return FileMessage.INSUFFICIENT_PRIVILEGE;
      }
    } else if (fileType.isProfilePic()) {
      try {
        return mongodbUploadPFP();
      } catch (GeneralSecurityException | IOException e) {
        return FileMessage.SERVER_ERROR;
      }
    } else {
      try {
        return mongodbUploadMisc();
      } catch (GeneralSecurityException | IOException e) {
        return FileMessage.SERVER_ERROR;
      }
    }
  }

  public Message mongodbUploadPDF() throws GeneralSecurityException, IOException {
    GridFSBucket gridBucket = GridFSBuckets.create(db, "files");
    fileStream = encryptionController.encryptFile(fileStream, uploader);
    if (fileType == FileType.FORM_PDF && this.annotated) {
      ObjectId fileID = new ObjectId(this.fileIdStr);
      GridFSFile grid_out = gridBucket.find(eq("_id", fileID)).first();
      if (grid_out == null || grid_out.getMetadata() == null) {
        return FileMessage.NO_SUCH_FILE;
      }
      if (grid_out.getMetadata().getString("organizationName").equals(organizationName)) {
        gridBucket.delete(fileID);
      }
    }

    Document metadata =
        new Document("type", fileType.toString())
            .append("upload_date", String.valueOf(LocalDate.now()))
            .append("title", title)
            .append("uploader", uploader)
            .append("organizationName", organizationName);
    if (fileType == FileType.FORM_PDF) {
      metadata.append("annotated", this.annotated);
    }
    GridFSUploadOptions options =
        new GridFSUploadOptions().chunkSizeBytes(CHUNK_SIZE_BYTES).metadata(metadata);
    gridBucket.uploadFromStream(filename, fileStream, options);
    return PdfMessage.SUCCESS;
  }

  public Message mongodbUploadPFP() throws GeneralSecurityException, IOException {
    Bson filter = Filters.eq("metadata.uploader", this.uploader);
    GridFSBucket gridBucket = GridFSBuckets.create(db, "files");
    GridFSFile grid_out = gridBucket.find(filter).first();
    if (grid_out != null) {
      gridBucket.delete(grid_out.getObjectId());
    }
    String[] temp = filename.split("\\.");
    String contentType = temp[temp.length - 1];
    if (!contentType.equals("png")) {
      contentType = "jpeg";
    }
    GridFSUploadOptions options =
        new GridFSUploadOptions()
            .chunkSizeBytes(CHUNK_SIZE_BYTES)
            .metadata(
                new Document("type", fileType.toString())
                    .append("upload_date", String.valueOf(LocalDate.now()))
                    .append("contentType", contentType)
                    .append("uploader", uploader));
    gridBucket.uploadFromStream(filename, fileStream, options);
    log.info(uploader + " has successfully uploaded a profile picture with name  " + filename);
    return UserMessage.SUCCESS.withMessage("Profile Picture uploaded Successfully");
  }

  public Message mongodbUploadMisc() throws GeneralSecurityException, IOException {
    GridFSBucket gridBucket = GridFSBuckets.create(db, "files");
    GridFSUploadOptions options =
        new GridFSUploadOptions()
            .chunkSizeBytes(CHUNK_SIZE_BYTES)
            .metadata(
                new Document("type", fileType.toString())
                    .append("contentType", fileContentType)
                    .append("upload_date", String.valueOf(LocalDate.now()))
                    .append("uploader", uploader));
    gridBucket.uploadFromStream(filename, fileStream, options);
    return FileMessage.SUCCESS;
  }

  private InputStream signFile() throws IOException {
    PDDocument pdfDocument = PDDocument.load(this.fileStream);

    PDVisibleSignDesigner visibleSignDesigner = new PDVisibleSignDesigner(this.signatureFileStream);
    visibleSignDesigner.zoom(0);
    PDVisibleSigProperties visibleSigProperties =
        new PDVisibleSigProperties()
            .visualSignEnabled(true)
            .setPdVisibleSignature(visibleSignDesigner);
    visibleSigProperties.buildSignature();

    SignatureOptions signatureOptions = new SignatureOptions();
    signatureOptions.setVisualSignature(visibleSigProperties.getVisibleSignature());

    PDSignature signature = new PDSignature();
    signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
    signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
    signature.setName(this.uploader);
    signature.setSignDate(Calendar.getInstance());

    for (PDSignatureField signatureField : findSignatureFields(pdfDocument)) {
      signatureField.setValue(signature);
    }

    pdfDocument.addSignature(signature, signatureOptions);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    pdfDocument.save(outputStream);
    pdfDocument.close();

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  // Make it so that it can handle different signers
  public static List<PDSignatureField> findSignatureFields(PDDocument pdfDocument) {
    List<PDSignatureField> signatureFields = new LinkedList<>();
    List<PDField> fields = new LinkedList<>();
    fields.addAll(pdfDocument.getDocumentCatalog().getAcroForm().getFields());
    while (!fields.isEmpty()) {
      PDField field = fields.get(0);
      if (field instanceof PDNonTerminalField) {
        List<PDField> childrenFields = ((PDNonTerminalField) field).getChildren();
        fields.addAll(childrenFields);
      } else {
        if (field instanceof PDSignatureField) {
          signatureFields.add((PDSignatureField) field);
        }
      }
      // Remove field just gotten so we do not get it again
      fields.remove(0);
    }
    return signatureFields;
  }
}
