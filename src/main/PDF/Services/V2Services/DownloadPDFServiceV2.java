package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import File.File;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.UserType;
import Validation.ValidationUtils;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.bson.types.ObjectId;

public class DownloadPDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private String fileId;
  private PDFTypeV2 pdfType;
  private InputStream downloadedInputStream;
  private EncryptionController encryptionController;

  public DownloadPDFServiceV2(
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
    this.fileId = fileParams.getFileId();
    this.pdfType = fileParams.getPdfType();
    this.encryptionController = encryptionController;
  }

  public InputStream getDownloadedInputStream() {
    return Objects.requireNonNull(this.downloadedInputStream);
  }

  @Override
  public Message executeAndGetResponse() {
    Message downloadConditionsErrorMessage = checkDownloadConditions();
    if (downloadConditionsErrorMessage != null) {
      return downloadConditionsErrorMessage;
    }
    return download();
  }

  public Message checkDownloadConditions() {
    if (!ValidationUtils.isValidObjectId(this.fileId) || this.pdfType == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    if (this.privilegeLevel == null) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
    }
    return null;
  }

  public Message download() {
    ObjectId fileObjectId = new ObjectId(this.fileId);
    Optional<File> fileOptional = this.fileDao.get(fileObjectId);
    if (fileOptional.isEmpty()) {
      return PdfMessage.NO_SUCH_FILE;
    }
    try {
      if (pdfType == PDFTypeV2.BLANK_APPLICATION) {
        this.downloadedInputStream = this.fileDao.getStream(fileObjectId).get();
      } else { // Client documents and annotated applications are encrypted
        this.downloadedInputStream =
            this.encryptionController.decryptFile(
                this.fileDao.getStream(fileObjectId).get(), this.username);
      }
      return PdfMessage.SUCCESS;
    } catch (Exception e) {
      return PdfMessage.SERVER_ERROR;
    }
  }
}
