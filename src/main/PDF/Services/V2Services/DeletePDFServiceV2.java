package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import File.File;
import Form.Form;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import User.UserType;
import Validation.ValidationUtils;
import java.util.Optional;
import org.bson.types.ObjectId;

public class DeletePDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private PDFTypeV2 pdfType;
  private String fileId;
  private File file;
  private ObjectId fileObjectId;

  public DeletePDFServiceV2(
      FileDao fileDao, FormDao formDao, UserParams userParams, FileParams fileParams) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.username = userParams.getUsername();
    this.organizationName = userParams.getOrganizationName();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.pdfType = fileParams.getPdfType();
    this.fileId = fileParams.getFileId();
  }

  @Override
  public Message executeAndGetResponse() {
    Message deleteConditionsErrorMessage = checkDeleteConditions();
    if (deleteConditionsErrorMessage != null) {
      return deleteConditionsErrorMessage;
    }
    return delete();
  }

  public Message checkDeleteConditions() {
    if (!ValidationUtils.isValidObjectId(this.fileId) || this.pdfType == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    this.fileObjectId = new ObjectId(this.fileId);
    Optional<File> fileOptional = this.fileDao.get(this.fileObjectId);
    if (fileOptional.isEmpty()) {
      return PdfMessage.NO_SUCH_FILE;
    }
    this.file = fileOptional.get();
    // Check Privileges
    if (this.privilegeLevel == null) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
    }
    if ((this.pdfType == PDFTypeV2.ANNOTATED_APPLICATION
            && (this.privilegeLevel == UserType.Client
                || this.privilegeLevel == UserType.Developer))
        || (this.pdfType == PDFTypeV2.CLIENT_UPLOADED_DOCUMENT
            && (this.privilegeLevel == UserType.Director
                || this.privilegeLevel == UserType.Admin
                || this.privilegeLevel == UserType.Worker
                || this.privilegeLevel == UserType.Developer))) {
      return PdfMessage.INSUFFICIENT_PRIVILEGE;
    }
    return null;
  }

  public Message delete() {
    // NEED TO FIGURE OUT WHAT EXACTLY TO DELETE
    //
    //
    if (this.pdfType == PDFTypeV2.CLIENT_UPLOADED_DOCUMENT) {
      if (!this.file.getOrganizationName().equals(this.organizationName)) {
        return PdfMessage.CROSS_ORG_ACTION_DENIED;
      }
      this.fileDao.delete(this.fileObjectId);
      return PdfMessage.SUCCESS;
    }
    Optional<Form> formOptional = this.formDao.getByFileId(this.fileObjectId);
    if (formOptional.isEmpty()) {
      return PdfMessage.MISSING_FORM;
    }
    ObjectId formObjectId = formOptional.get().getId();
    if (this.pdfType == PDFTypeV2.ANNOTATED_APPLICATION) {
      if (!this.file.getOrganizationName().equals(this.organizationName)) {
        return PdfMessage.CROSS_ORG_ACTION_DENIED;
      }
      this.fileDao.delete(this.fileObjectId);
      this.formDao.delete(formObjectId);
      return PdfMessage.SUCCESS;
    }
    if (this.pdfType == PDFTypeV2.BLANK_APPLICATION) { // THIS PDFTYPE IS NOT USED IN THE FRONTEND
      if (!this.file
          .getUsername()
          .equals(this.username)) { // Only the person who uploaded the blank app can delete
        return PdfMessage.INSUFFICIENT_USER_PRIVILEGE;
      }
      this.fileDao.delete(this.fileObjectId);
      this.formDao.delete(formObjectId);
      return PdfMessage.SUCCESS;
    }
    return PdfMessage.INVALID_PDF_TYPE;
  }
}
