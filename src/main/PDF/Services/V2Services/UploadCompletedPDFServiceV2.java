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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

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
  private ObjectId organizationId;
  private UserType privilegeLevel;
  private String applicationId;
  private JSONObject formAnswers;
  private InputStream pdfFileStream;
  private EncryptionController encryptionController;
  private FileDao fileDaoRef;
  private File filledFile;
  private Form filledForm;
  private boolean replacingExistingApplication;
  private ObjectId existingApplicationObjectId;
  private ObjectId existingPacketObjectId;
  private ObjectId persistedApplicationObjectId;

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
    this.organizationId = userParams.getOrganizationId();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.applicationId = fileParams.getFileId();
    this.formAnswers = fileParams.getFormAnswers();
    this.pdfFileStream = fileParams.getFileStream();
    this.encryptionController = encryptionController;
    this.fileDaoRef = fileDao;
    this.replacingExistingApplication = false;
    this.existingApplicationObjectId = null;
    this.existingPacketObjectId = null;
    this.persistedApplicationObjectId = null;
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

    Optional<File> existingFileOpt = fileDaoRef.get(new ObjectId(applicationId));
    if (existingFileOpt.isPresent()) {
      File existingFile = existingFileOpt.get();
      if (this.organizationId == null) {
        this.organizationId = existingFile.getOrganizationId();
      }
      if (existingFile.getFileType() == FileType.APPLICATION_PDF) {
        if (!Objects.equals(existingFile.getUsername(), username)
            || !Objects.equals(existingFile.getOrganizationName(), organizationName)) {
          return PdfMessage.INSUFFICIENT_PRIVILEGE;
        }
        replacingExistingApplication = true;
        existingApplicationObjectId = existingFile.getId();
        existingPacketObjectId = existingFile.getPacketId();
      }
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
      InputStream completedPdfStream = pdfFileStream;
      InputStream encryptedStream =
          FileStorageCryptoPolicy.prepareForStorage(
              completedPdfStream, FileType.APPLICATION_PDF, username, encryptionController);

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
      this.filledFile.setOrganizationId(organizationId);

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

      Map<String, String> mergedMetadata = new HashMap<>();
      Optional<Form> templateOpt = formDao.getByFileId(new ObjectId(applicationId));
      if (templateOpt.isPresent()) {
        Form templateForm = templateOpt.get();
        Map<String, String> tmplMeta = templateForm.getApplicationMetadata();
        if (tmplMeta != null) {
          mergedMetadata.putAll(tmplMeta);
        }
        String templateTitle =
            templateForm.getMetadata() != null ? templateForm.getMetadata().getTitle() : null;
        if (templateTitle != null && !templateTitle.isBlank()) {
          mergedMetadata.putIfAbsent("applicationTitle", templateTitle.trim());
          mergedMetadata.putIfAbsent("applicationDisplayName", templateTitle.trim());
        }
      }
      mergedMetadata.putAll(extractMetadataFromAnswers(this.formAnswers));
      this.filledForm.setApplicationMetadata(mergedMetadata);

      if (replacingExistingApplication && existingApplicationObjectId != null) {
        Optional<File> existingApplicationFile = fileDao.get(existingApplicationObjectId);
        this.filledFile.setId(existingApplicationObjectId);
        this.filledFile.setPacketId(existingPacketObjectId);
        this.filledForm.setFileId(existingApplicationObjectId);
        if (existingApplicationFile.isPresent()) {
          // Keep original upload timestamp when replacing an existing application.
          this.filledFile.setUploadedAt(existingApplicationFile.get().getUploadedAt());
        }

        Optional<Form> existingForm = formDao.getByFileId(existingApplicationObjectId);
        if (existingForm.isPresent()) {
          // Preserve original form upload timestamp for edited applications.
          this.filledForm.setUploadedAt(existingForm.get().getUploadedAt());
          this.filledForm.setId(existingForm.get().getId());
        }

        fileDao.update(filledFile);
        this.persistedApplicationObjectId = existingApplicationObjectId;
        if (existingForm.isPresent()) {
          formDao.update(filledForm);
        } else {
          formDao.save(filledForm);
        }
      } else {
        fileDao.save(filledFile);
        this.persistedApplicationObjectId = filledFile.getId();
        formDao.save(filledForm);
      }
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

  private InputStream flattenInteractiveFields(InputStream sourcePdfStream) {
    byte[] pdfBytes;
    try {
      pdfBytes = sourcePdfStream.readAllBytes();
    } catch (Exception e) {
      log.warn("Could not read completed PDF for flattening, storing original stream: {}", e.getMessage());
      return sourcePdfStream;
    }

    try (PDDocument document = Loader.loadPDF(new ByteArrayInputStream(pdfBytes));
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      PDAcroForm form = document.getDocumentCatalog().getAcroForm();
      if (form != null) {
        form.flatten();
      }
      document.save(output);
      return new ByteArrayInputStream(output.toByteArray());
    } catch (Exception e) {
      log.warn("Could not flatten completed PDF, storing original bytes: {}", e.getMessage());
      return new ByteArrayInputStream(pdfBytes);
    }
  }

  private static Map<String, String> extractMetadataFromAnswers(JSONObject formAnswers) {
    Map<String, String> metadata = new HashMap<>();
    if (formAnswers != null && formAnswers.has("metadata")) {
      Object raw = formAnswers.get("metadata");
      if (raw instanceof JSONObject) {
        JSONObject metaObj = (JSONObject) raw;
        for (String key : metaObj.keySet()) {
          Object val = metaObj.get(key);
          if (val != null && !JSONObject.NULL.equals(val)) {
            metadata.put(key, String.valueOf(val));
          }
        }
      }
    }
    return metadata;
  }

  private void recordSubmitApplicationActivity() {
    String activityApplicationId =
        persistedApplicationObjectId != null
            ? persistedApplicationObjectId.toString()
            : applicationId;
    SubmitApplicationActivity activity =
        new SubmitApplicationActivity(
            username, username, activityApplicationId, filledFile.getFilename());
    activityDao.save(activity);
  }

  public String getPersistedApplicationId() {
    if (persistedApplicationObjectId == null) {
      return null;
    }
    return persistedApplicationObjectId.toString();
  }
}
