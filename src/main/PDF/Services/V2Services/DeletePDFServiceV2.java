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
    if (!ValidationUtils.isValidObjectId(fileId) || pdfType == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    fileObjectId = new ObjectId(fileId);
    Optional<File> fileOptional = fileDao.get(fileObjectId);
    if (fileOptional.isEmpty()) {
      return PdfMessage.NO_SUCH_FILE;
    }
    file = fileOptional.get();
    // Check Privileges
    if (privilegeLevel == null) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
    }
    if ((pdfType == PDFTypeV2.ANNOTATED_APPLICATION
            && (privilegeLevel == UserType.Client || privilegeLevel == UserType.Developer))
        || (pdfType == PDFTypeV2.CLIENT_UPLOADED_DOCUMENT
            && (privilegeLevel == UserType.Director
                || privilegeLevel == UserType.Admin
                || privilegeLevel == UserType.Developer))) {
      return PdfMessage.INSUFFICIENT_PRIVILEGE;
    }
    return null;
  }

  public Message delete() {
    // NEED TO FIGURE OUT WHAT EXACTLY TO DELETE
    //
    //
    if (pdfType == PDFTypeV2.ANNOTATED_APPLICATION) {
      if (!file.getOrganizationName().equals(organizationName)) {
        return PdfMessage.CROSS_ORG_ACTION_DENIED;
      }
      Optional<Form> formOptional = formDao.get(fileObjectId);
      if (formOptional.isEmpty()) {
        return PdfMessage.MISSING_FORM;
      }
      fileDao.delete(fileObjectId);
      formDao.delete(fileObjectId);
      return PdfMessage.SUCCESS;
    }
    if (pdfType == PDFTypeV2.BLANK_APPLICATION) {
      if (!file.getUsername().equals(username)) {
        return PdfMessage.INSUFFICIENT_USER_PRIVILEGE;
      }
      fileDao.delete(fileObjectId);
      return PdfMessage.SUCCESS;
    }
    if (pdfType == PDFTypeV2.CLIENT_UPLOADED_DOCUMENT) {
      if (!file.getOrganizationName().equals(organizationName)) {
        return PdfMessage.CROSS_ORG_ACTION_DENIED;
      }
      fileDao.delete(fileObjectId);
      return PdfMessage.SUCCESS;
    }
    return PdfMessage.INVALID_PDF_TYPE;
  }
}
