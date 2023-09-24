package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.UserType;
import java.io.InputStream;

public class UploadSignedPDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private PDFTypeV2 pdfType;
  private String fileName;
  private String fileContentType;
  private InputStream fileStream;
  private InputStream signatureStream;
  private EncryptionController encryptionController;

  public UploadSignedPDFServiceV2(
      FileDao fileDao,
      FormDao formDao,
      UserParams userParams,
      FileParams fileParams,
      EncryptionController encryptionController) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.username = userParams.getUsername();
    this.organizationName = userParams.getOrganizationName();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.pdfType = fileParams.getPdfType();
    this.fileName = fileParams.getFileName();
    this.fileContentType = fileParams.getFileContentType();
    this.fileStream = fileParams.getFileStream();
    this.signatureStream = fileParams.getSignatureStream();
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    Message uploadConditionsErrorMessage = checkUploadConditions();
    if (uploadConditionsErrorMessage != null) {
      return uploadConditionsErrorMessage;
    }
    return upload();
  }

  public Message checkUploadConditions() {
    if (pdfType == null) {
      return PdfMessage.INVALID_PDF_TYPE;
    }
    if (fileStream == null
        || !fileContentType.equals("application/pdf")
        || signatureStream == null) {
      return PdfMessage.INVALID_PDF;
    }
    if (privilegeLevel == null) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
    }
    if (privilegeLevel == UserType.Developer) {
      return PdfMessage.INSUFFICIENT_PRIVILEGE;
    }
    return null;
  }

  public Message upload() {
    // FINISH SIGN AND UPLOAD
    return null;
  }
}
