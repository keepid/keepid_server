package File.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import File.File;
import File.FileMessage;
import File.FileType;
import Security.EncryptionController;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

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
  FileDao fileDao;
  EncryptionController encryptionController;

  public UploadFileService(
      MongoDatabase db,
      FileDao fileDao,
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
    this.fileDao = fileDao;
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
          return uploadFile();
        } catch (GeneralSecurityException | IOException e) {
          return FileMessage.SERVER_ERROR;
        }
      } else {
        log.info("Privilege level: {}", privilegeLevel.toString());
        return FileMessage.INSUFFICIENT_PRIVILEGE;
      }
    } else if (fileType.isProfilePic()) {
      try {
        return uploadPFP();
      } catch (GeneralSecurityException | IOException e) {
        return FileMessage.SERVER_ERROR;
      }
    } else {
      try {
        return uploadFile();
      } catch (GeneralSecurityException | IOException e) {
        return FileMessage.SERVER_ERROR;
      }
    }
  }

  public Message uploadFile() throws GeneralSecurityException, IOException {
    Date uploadDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    fileStream = encryptionController.encryptFile(fileStream, uploader);
    return this.fileDao.save(
        this.uploader,
        this.fileStream,
        this.fileType,
        uploadDate,
        this.organizationName,
        this.annotated,
        this.filename,
        this.fileContentType);
  }

  public Message uploadPFP() throws GeneralSecurityException, IOException {
    Optional<File> optFile = fileDao.get(this.uploader, FileType.PROFILE_PICTURE);
    if (optFile.isPresent()) {
      fileDao.delete(optFile.get());
    }
    String[] temp = filename.split("\\.");
    String contentType = temp[temp.length - 1];
    if (!contentType.equals("png")) {
      contentType = "jpeg";
    }
    Date uploadDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    return this.fileDao.save(
        this.uploader,
        this.fileStream,
        this.fileType,
        uploadDate,
        this.organizationName,
        this.annotated,
        this.filename,
        contentType);
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
