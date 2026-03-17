package PDF.Services.V2Services;

import Activity.UserActivity.ApplicationActivity.SubmitApplicationActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.User.UserDao;
import File.File;
import Form.Form;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.Services.UpdateProfileFromFormService;
import User.UserMessage;
import User.UserType;
import Validation.ValidationUtils;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONObject;

@Slf4j
public class UploadSignedPDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private UserDao userDao;
  ActivityDao activityDao;
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
      ActivityDao activityDao,
      UserParams userParams,
      FileParams fileParams,
      EncryptionController encryptionController) {
    this(fileDao, formDao, activityDao, null, userParams, fileParams, encryptionController);
  }

  public UploadSignedPDFServiceV2(
      FileDao fileDao,
      FormDao formDao,
      ActivityDao activityDao,
      UserDao userDao,
      UserParams userParams,
      FileParams fileParams,
      EncryptionController encryptionController) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.activityDao = activityDao;
    this.userDao = userDao;
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
    updateClientProfileFromFormAnswers();
    recordSubmitApplicationActivity();
    return PdfMessage.SUCCESS;
  }

  private void updateClientProfileFromFormAnswers() {
    if (this.userDao == null) {
      return;
    }
    UpdateProfileFromFormService updateProfileFromFormService =
        new UpdateProfileFromFormService(this.userDao, this.username, this.formAnswers);
    Message updateResponse = updateProfileFromFormService.executeAndGetResponse();
    if (updateResponse != UserMessage.SUCCESS) {
      // Do not block application submission on profile backfill errors.
      // Profile update is a best-effort post-submit enhancement.
      log.warn(
          "UpdateProfileFromFormService failed for user '{}': {}",
          this.username,
          updateResponse);
    }
  }

  private void recordSubmitApplicationActivity() {
    SubmitApplicationActivity activity =
        new SubmitApplicationActivity(username, username, fileId, filledFile.getFilename());
    activityDao.save(activity);
  }
}
