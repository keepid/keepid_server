package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import File.File;
import Form.Form;
import PDF.PDFTypeV2;
import PDF.PdfMessage;
import User.UserType;
import Validation.ValidationUtils;
import java.util.Optional;
import org.bson.types.ObjectId;

public class DeletePDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private String username;
  private String orgName;
  private UserType privilegeLevel;
  private PDFTypeV2 pdfType;
  private String fileId;

  public DeletePDFServiceV2(
      FileDao fileDao,
      FormDao formDao,
      String username,
      String orgName,
      UserType privilegeLevel,
      PDFTypeV2 pdfType,
      String fileId) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.username = username;
    this.orgName = orgName;
    this.privilegeLevel = privilegeLevel;
    this.pdfType = pdfType;
    this.fileId = fileId;
  }

  @Override
  public Message executeAndGetResponse() {
    if (!ValidationUtils.isValidObjectId(fileId) || pdfType == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    return delete();
  }

  public Message delete() {
    ObjectId id = new ObjectId(fileId);
    Optional<File> fileOptional = fileDao.get(id);
    if (fileOptional.isEmpty()) {
      return PdfMessage.NO_SUCH_FILE;
    }
    File file = fileOptional.get();
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
    // NEED TO FIGURE OUT WHAT EXACTLY TO DELETE
    //
    //
    if (pdfType == PDFTypeV2.ANNOTATED_APPLICATION) {
      if (!file.getOrganizationName().equals(orgName)) {
        return PdfMessage.CROSS_ORG_ACTION_DENIED;
      }
      Optional<Form> formOptional = formDao.get(id);
      if (formOptional.isEmpty()) {
        return PdfMessage.MISSING_FORM;
      }
      fileDao.delete(id);
      formDao.delete(id);
      return PdfMessage.SUCCESS;
    }
    if (pdfType == PDFTypeV2.BLANK_APPLICATION) {
      if (!file.getUsername().equals(username)) {
        return PdfMessage.INSUFFICIENT_USER_PRIVILEGE;
      }
      fileDao.delete(id);
      return PdfMessage.SUCCESS;
    }
    if (pdfType == PDFTypeV2.CLIENT_UPLOADED_DOCUMENT) {
      if (!file.getOrganizationName().equals(orgName)) {
        return PdfMessage.CROSS_ORG_ACTION_DENIED;
      }
      fileDao.delete(id);
      return PdfMessage.SUCCESS;
    }
    return PdfMessage.INVALID_PDF_TYPE;
  }
}
