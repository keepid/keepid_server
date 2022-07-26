package File.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import File.File;
import File.FileMessage;
import File.FileType;
import Security.EncryptionController;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
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
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class UploadFileService implements Service {
  public static final int CHUNK_SIZE_BYTES = 100000;

  File fileToUpload;
  Optional<UserType> privilegeLevel;
  Optional<String> fileIdStr;
  boolean toSign;
  Optional<InputStream> signatureFileStream;
  FileDao fileDao;
  Optional<EncryptionController> encryptionController;

  public UploadFileService(
      FileDao fileDao,
      File fileToUpload,
      Optional<UserType> privilegeLevel,
      Optional<String> fileIdStr,
      boolean toSign,
      Optional<InputStream> signatureFileStream,
      Optional<EncryptionController> encryptionController) {
    this.fileDao = fileDao;
    this.fileToUpload = fileToUpload;
    this.privilegeLevel = privilegeLevel;
    this.fileIdStr = fileIdStr;
    this.toSign = toSign;
    this.signatureFileStream = signatureFileStream;
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    FileType fileType = this.fileToUpload.getFileType();
    InputStream fileStream = this.fileToUpload.getFileStream();
    String fileContentType = this.fileToUpload.getContentType();
    if (fileType == null) {
      return FileMessage.INVALID_FILE_TYPE;
    } else if (fileStream == null) {
      return FileMessage.INVALID_FILE;
    } else if (fileType.isPDF()) {
      if (!fileContentType.equals("application/pdf")
          && !fileContentType.equals("application/octet-stream")) {
        return FileMessage.INVALID_FILE_TYPE;
      }
      if (privilegeLevel.isEmpty()) {
        return FileMessage.INSUFFICIENT_PRIVILEGE;
      }
      UserType privilegeLevelType = privilegeLevel.get();
      if (privilegeLevelType == UserType.Client
          || privilegeLevelType == UserType.Worker
          || privilegeLevelType == UserType.Director
          || privilegeLevelType == UserType.Admin
          || privilegeLevelType == UserType.Developer) {
        try {
          if (this.toSign) {
            if (signatureFileStream.isPresent()) this.fileToUpload.setFileStream(signFile());
            else return FileMessage.INVALID_PARAMETER;
          }
          return uploadFile();
        } catch (GeneralSecurityException | IOException e) {
          return FileMessage.SERVER_ERROR;
        }
      } else {
        log.info("Privilege level: {}", privilegeLevelType.toString());
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
    if (encryptionController.isEmpty()) {
      return FileMessage.SERVER_ERROR;
    }
    EncryptionController controller = encryptionController.get();
    this.fileToUpload.setFileStream(
        controller.encryptFile(this.fileToUpload.getFileStream(), this.fileToUpload.getUsername()));
    this.fileDao.save(fileToUpload);
    return FileMessage.SUCCESS;
  }

  public Message uploadPFP() throws GeneralSecurityException, IOException {
    Optional<File> optFile = fileDao.get(this.fileToUpload.getUsername(), FileType.PROFILE_PICTURE);
    System.out.println("Uploading PFP real quick!");
    if (optFile.isPresent()) {
      fileDao.delete(optFile.get());
    }
    String[] temp = fileToUpload.getFilename().split("\\.");
    String contentType = temp[temp.length - 1];
    if (!contentType.equals("png")) {
      contentType = "jpeg";
    }
    this.fileToUpload.setContentType(contentType);
    this.fileDao.save(fileToUpload);
    return FileMessage.SUCCESS;
  }

  private InputStream signFile() throws IOException {
    PDDocument pdfDocument = Loader.loadPDF(this.fileToUpload.getFileStream());

    InputStream signatureStream = signatureFileStream.get();
    PDVisibleSignDesigner visibleSignDesigner = new PDVisibleSignDesigner(signatureStream);
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
    signature.setName(this.fileToUpload.getUsername());
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
