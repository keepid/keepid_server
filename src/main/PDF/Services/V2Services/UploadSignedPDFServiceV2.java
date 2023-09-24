package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import File.File;
import Form.Form;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.UserType;
import Validation.ValidationUtils;
import java.io.InputStream;
import org.bson.types.ObjectId;
import org.json.JSONObject;

public class UploadSignedPDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private String fileId;
  private JSONObject formAnswers;
  private InputStream signatureStream;
  private EncryptionController encryptionController;
  private UserParams userParams;
  private FileParams fileParams;
  private File filledFile;
  private Form filledForm;

  public UploadSignedPDFServiceV2(
      FileDao fileDao,
      FormDao formDao,
      UserParams userParams,
      FileParams fileParams,
      EncryptionController encryptionController) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.userParams = userParams;
    this.username = userParams.getUsername();
    this.organizationName = userParams.getOrganizationName();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.fileParams = fileParams;
    this.fileId = fileParams.getFileId();
    this.formAnswers = fileParams.getFormAnswers();
    this.signatureStream = fileParams.getSignatureStream();
    this.encryptionController = encryptionController;
  }

  public ObjectId getFilledFileObjectId() {
    return this.filledFile.getId();
  }

  @Override
  public Message executeAndGetResponse() {
    Message uploadConditionsErrorMessage = checkUploadConditions();
    if (uploadConditionsErrorMessage != null) {
      return uploadConditionsErrorMessage;
    }
    FillPDFServiceV2 fillPDFServiceV2 =
        new FillPDFServiceV2(fileDao, formDao, userParams, fileParams, encryptionController);
    Message fillResponse = fillPDFServiceV2.executeAndGetResponse();
    if (fillResponse != PdfMessage.SUCCESS) {
      return fillResponse;
    }
    this.filledFile = fillPDFServiceV2.getFilledFile();
    this.filledForm = fillPDFServiceV2.getFilledForm();
    if (this.filledFile == null || this.filledForm == null) {
      return PdfMessage.SERVER_ERROR;
    }
    return upload();
  }

  public Message checkUploadConditions() {
    if (!ValidationUtils.isValidObjectId(fileId) || formAnswers == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    if (signatureStream == null) {
      return PdfMessage.SERVER_ERROR;
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
    this.fileDao.save(this.filledFile);
    this.formDao.save(this.filledForm);
    return PdfMessage.SUCCESS;
  }
}
