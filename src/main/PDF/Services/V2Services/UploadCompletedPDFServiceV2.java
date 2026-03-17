package PDF.Services.V2Services;

import Activity.UserActivity.ApplicationActivity.SubmitApplicationActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import Database.Form.FormDao;
import File.File;
import File.FileType;
import File.IdCategoryType;
import Form.FieldType;
import Form.Form;
import Form.FormMetadata;
import Form.FormQuestion;
import Form.FormSection;
import Form.FormType;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import Security.EncryptionController;
import Security.FileStorageCryptoPolicy;
import User.UserType;
import Validation.ValidationUtils;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Saves a pre-filled and pre-signed PDF (completed in the browser) as an application.
 * Accepts the PDF file directly instead of running fill+sign server-side.
 */
@Slf4j
public class UploadCompletedPDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private ActivityDao activityDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private String applicationId;
  private JSONObject formAnswers;
  private InputStream pdfFileStream;
  private EncryptionController encryptionController;
  private FileDao fileDaoRef;
  private File filledFile;
  private Form filledForm;

  public UploadCompletedPDFServiceV2(
      FileDao fileDao,
      FormDao formDao,
      ActivityDao activityDao,
      UserParams userParams,
      FileParams fileParams,
      EncryptionController encryptionController) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.activityDao = activityDao;
    this.username = userParams.getUsername();
    this.organizationName = userParams.getOrganizationName();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.applicationId = fileParams.getFileId();
    this.formAnswers = fileParams.getFormAnswers();
    this.pdfFileStream = fileParams.getFileStream();
    this.encryptionController = encryptionController;
    this.fileDaoRef = fileDao;
  }

  @Override
  public Message executeAndGetResponse() {
    Message err = checkConditions();
    if (err != null) {
      return err;
    }
    return saveCompletedPdf();
  }

  private Message checkConditions() {
    if (!ValidationUtils.isValidObjectId(applicationId) || formAnswers == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    if (pdfFileStream == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    if (privilegeLevel == null) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
    }
    if (privilegeLevel == UserType.Developer) {
      return PdfMessage.INSUFFICIENT_PRIVILEGE;
    }
    return null;
  }

  private String getTemplateFilename() {
    Optional<File> templateOpt = fileDaoRef.get(new ObjectId(applicationId));
    return templateOpt.map(File::getFilename).orElse("Application.pdf");
  }

  private FormSection buildFormBodyFromAnswers() {
    List<FormQuestion> questions = new LinkedList<>();
    String baseName = getTemplateFilename();
    if (baseName.endsWith(".pdf")) {
      baseName = baseName.substring(0, baseName.length() - 4);
    }
    String sectionTitle = baseName + " Form";

    for (String key : formAnswers.keySet()) {
      if ("metadata".equals(key)) continue;
      Object val = formAnswers.get(key);
      String answerText = val == null ? "" : String.valueOf(val);
      if (answerText.isEmpty() || "null".equals(answerText)) continue;

      FormQuestion record =
          new FormQuestion(
              new ObjectId(),
              FieldType.TEXT_FIELD,
              key,
              null,
              key,
              "",
              new ArrayList<>(),
              "",
              false,
              3,
              false,
              new ObjectId(),
              "NONE");
      if (val instanceof Boolean) {
        record.setAnswerText(Boolean.toString((Boolean) val));
      } else if (val instanceof JSONArray) {
        record.setAnswerText(val.toString());
      } else {
        record.setAnswerText(answerText);
      }
      questions.add(record);
    }

    return new FormSection(sectionTitle, "", new ArrayList<>(), questions);
  }

  private Message saveCompletedPdf() {
    try {
      InputStream encryptedStream =
          FileStorageCryptoPolicy.prepareForStorage(
              pdfFileStream, FileType.APPLICATION_PDF, username, encryptionController);

      String filename = getTemplateFilename();
      this.filledFile =
          new File(
              username,
              new Date(),
              encryptedStream,
              FileType.APPLICATION_PDF,
              IdCategoryType.NONE,
              filename,
              organizationName,
              true,
              "application/pdf");

      ObjectId filledFileObjectId = filledFile.getId();
      FormSection formBody = buildFormBodyFromAnswers();

      this.filledForm =
          new Form(
              username,
              Optional.of(username),
              LocalDateTime.now(),
              Optional.of(LocalDateTime.now()),
              FormType.APPLICATION,
              false,
              new FormMetadata(
                  filename + " Form",
                  filename + " Form",
                  "",
                  "",
                  new HashSet<>(),
                  LocalDateTime.now(),
                  new ArrayList<>(),
                  0),
              formBody,
              new ObjectId(),
              "");
      this.filledForm.setFileId(filledFileObjectId);

      fileDao.save(filledFile);
      formDao.save(filledForm);
      recordSubmitApplicationActivity();
      return PdfMessage.SUCCESS;
    } catch (GeneralSecurityException e) {
      log.error("Failed to encrypt completed PDF: {}", e.getMessage());
      return PdfMessage.SERVER_ERROR;
    } catch (Exception e) {
      log.error("Failed to save completed PDF: {}", e.getMessage(), e);
      return PdfMessage.SERVER_ERROR;
    }
  }

  private void recordSubmitApplicationActivity() {
    SubmitApplicationActivity activity =
        new SubmitApplicationActivity(
            username, username, applicationId, filledFile.getFilename());
    activityDao.save(activity);
  }
}
