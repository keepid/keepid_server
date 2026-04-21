package File.Services;

import Activity.UserActivity.FileActivity.UploadFileActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import File.File;
import File.FileMessage;
import File.FileType;
import PDF.Services.V2Services.NormalizePdfFieldAppearancesService;
import Security.EncryptionController;
import Security.OrganizationCryptoAad;
import User.UserType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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

@Slf4j
public class UploadFileService implements Service {
  public static final int CHUNK_SIZE_BYTES = 100000;

  File fileToUpload;
  Optional<UserType> privilegeLevel;
  Optional<String> fileIdStr;
  boolean toSign;
  Optional<InputStream> signatureFileStream;
  FileDao fileDao;
  ActivityDao activityDao;
  String usernameOfInvoker;
  Optional<EncryptionController> encryptionController;

  public UploadFileService(
      FileDao fileDao,
      ActivityDao activityDao,
      String usernameOfInvoker,
      File fileToUpload,
      Optional<UserType> privilegeLevel,
      Optional<String> fileIdStr,
      boolean toSign,
      Optional<InputStream> signatureFileStream,
      Optional<EncryptionController> encryptionController) {
    this.fileDao = fileDao;
    this.activityDao = activityDao;
    this.usernameOfInvoker = usernameOfInvoker;
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
      if (!fileContentType.equals("application/pdf") && !fileContentType.startsWith("image")) {
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
          // Normalize BEFORE signing: re-saving a signed PDF would invalidate the /ByteRange
          // digest of the embedded signature. Normalizing the template first means the signature
          // is computed over the already-normalized bytes and survives unchanged.
          if (!normalizePdfFieldAppearancesIfApplicable()) {
            return FileMessage.SERVER_ERROR;
          }
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

  /**
   * Runs a best-effort /DA normalization pass on uploaded AcroForm PDFs so all downstream
   * renderers (pdf.js preview, pdfbox fill, pdf-lib flatten, native PDF viewer on print) agree on
   * field font sizing. The normalize service itself is best-effort and returns original bytes on
   * failure, but reading the upload stream is not: a read failure here must not be swallowed or
   * the caller would encrypt a partially-consumed stream.
   *
   * @return {@code true} on success (including the no-op case for non-PDF uploads), {@code false}
   *     if the upload stream could not be fully read and the caller should abort with SERVER_ERROR.
   */
  private boolean normalizePdfFieldAppearancesIfApplicable() {
    FileType fileType = this.fileToUpload.getFileType();
    if (fileType == null || !fileType.isPDF()) {
      return true;
    }
    String contentType = this.fileToUpload.getContentType();
    if (contentType == null || !contentType.startsWith("application/pdf")) {
      // Image-sourced "PDFs" are converted elsewhere; only normalize when we actually have a PDF.
      return true;
    }
    InputStream original = this.fileToUpload.getFileStream();
    if (original == null) {
      return true;
    }
    byte[] originalBytes;
    try {
      originalBytes = original.readAllBytes();
    } catch (IOException e) {
      // Stream is now partially consumed; we can't fall through to uploadFile() or we'd persist
      // a truncated PDF. Surface the failure so the handler returns SERVER_ERROR.
      log.warn("PDF upload stream read failed during /DA normalization: {}", e.getMessage());
      return false;
    }
    byte[] normalized;
    try {
      normalized = NormalizePdfFieldAppearancesService.normalize(originalBytes);
    } catch (Exception e) {
      log.warn("PDF /DA normalization skipped for upload: {}", e.getMessage());
      normalized = originalBytes;
    }
    this.fileToUpload.setFileStream(new ByteArrayInputStream(normalized));
    return true;
  }

  private void recordUploadFileActivity(boolean isProfilePic) {
    String filename = fileToUpload.getFilename();
    if (isProfilePic) {
      filename = "Profile Picture";
    }
    UploadFileActivity log =
        new UploadFileActivity(
            usernameOfInvoker,
            fileToUpload.getUsername(),
            fileToUpload.getFileType(),
            fileToUpload.getFileId(),
            filename);
    activityDao.save(log);
  }

  public Message uploadFile() throws GeneralSecurityException, IOException {
    if (encryptionController.isEmpty()) {
      return FileMessage.SERVER_ERROR;
    }
    EncryptionController controller = encryptionController.get();
    FileType ft = this.fileToUpload.getFileType();
    String encryptionContext =
        ((ft == FileType.ORG_DOCUMENT || ft == FileType.FORM)
                && this.fileToUpload.getOrganizationId() != null)
            ? OrganizationCryptoAad.fromOrganizationId(this.fileToUpload.getOrganizationId())
            : this.fileToUpload.getUsername();
    this.fileToUpload.setFileStream(
        controller.encryptFile(this.fileToUpload.getFileStream(), encryptionContext));
    this.fileDao.save(fileToUpload);
    recordUploadFileActivity(false);
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
    recordUploadFileActivity(true);
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
