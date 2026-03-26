package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
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
import java.io.*;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

@Slf4j
public class FillPDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private String fileId;
  private JSONObject formAnswers;
  private InputStream signatureStream;
  private InputStream filledFileStream;
  private File templateFile;
  private File filledFile;
  private Form filledForm;
  private FormSection filledFormBody;
  private PDDocument pdfDocument;
  private EncryptionController encryptionController;
  private ByteArrayOutputStream filledFileOutputStream;
  private boolean preview;

  public FillPDFServiceV2(
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
    this.formAnswers = fileParams.getFormAnswers();
    this.signatureStream = fileParams.getSignatureStream();
    this.encryptionController = encryptionController;
    this.preview = fileParams.isPreview();
  }

  // FILE STREAM MUST BE CLOSED EXTERNALLY
  public InputStream getFilledFileStream() {
    return new ByteArrayInputStream(this.filledFileOutputStream.toByteArray());
  }

  public File getFilledFile() {
    return Objects.requireNonNull(filledFile);
  }

  public Form getFilledForm() {
    return Objects.requireNonNull(filledForm);
  }

  @Override
  public Message executeAndGetResponse() {
    Message FillPDFConditionsErrorMessage = checkFillConditions();
    if (FillPDFConditionsErrorMessage != null) {
      return FillPDFConditionsErrorMessage;
    }
    return fill();
  }

  public Message checkFillConditions() {
    if (!ValidationUtils.isValidObjectId(fileId) || formAnswers == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    //    if (signatureStream == null) {
    //      return PdfMessage.SERVER_ERROR;
    //    }
    if (privilegeLevel == null) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
    }
    if (privilegeLevel == UserType.Developer && !preview) {
      return PdfMessage.INSUFFICIENT_PRIVILEGE;
    }
    return null;
  }

  /**
   * Fills PDF fields directly from formAnswers. No Form/templateForm dependency.
   * Iterates formAnswers keys, looks up each field in the PDF AcroForm, and fills it.
   * Also builds filledFormBody (FormQuestion records) for the filledForm that gets saved on upload.
   */
  public void fillFieldsFromAnswers(PDAcroForm acroForm) throws IOException {
    List<FormQuestion> filledFormBodyQuestions = new LinkedList<>();
    String sectionTitle = this.templateFile != null ? this.templateFile.getFilename() + " Form" : "Form";
    String sectionDesc = "";

    for (String key : formAnswers.keySet()) {
      if ("metadata".equals(key)) continue;
      String answerText = String.valueOf(formAnswers.get(key));
      if (answerText == null || answerText.isEmpty() || answerText.equals("null")) continue;

      PDField field = acroForm.getField(key);
      if (field == null) continue;

      FormQuestion record = new FormQuestion(
          new ObjectId(), FieldType.TEXT_FIELD, key, null,
          key, "", new ArrayList<>(), "", false, 3, false,
          new ObjectId(), "NONE");
      record.setAnswerText(answerText);

      try {
        fillPDField(field, answerText, record);
        filledFormBodyQuestions.add(record);
      } catch (Exception e) {
        log.warn("Failed to set PDF field '{}': {}", key, e.getMessage());
      }
    }

    this.filledFormBody =
        new FormSection(sectionTitle, sectionDesc, new ArrayList<>(), filledFormBodyQuestions);
  }

  private void fillPDField(PDField field, String answerText, FormQuestion filledQuestion)
      throws IOException {
    if (field instanceof PDButton) {
      if (field instanceof PDCheckBox) {
        PDCheckBox checkBoxField = (PDCheckBox) field;
        boolean fieldAnswer = "true".equalsIgnoreCase(answerText)
            || "on".equalsIgnoreCase(answerText)
            || "yes".equalsIgnoreCase(answerText)
            || "1".equals(answerText);
        filledQuestion.setAnswerText(Boolean.toString(fieldAnswer));
        if (fieldAnswer) {
          checkBoxField.check();
        } else {
          checkBoxField.unCheck();
        }
      } else if (field instanceof PDPushButton) {
        // Do nothing
      } else if (field instanceof PDRadioButton) {
        PDRadioButton radioButtonField = (PDRadioButton) field;
        if (answerText == null || answerText.isEmpty() || answerText.equals("Off")) {
          return;
        }
        filledQuestion.setAnswerText(answerText);
        radioButtonField.setValue(answerText);
      }
    } else if (field instanceof PDVariableText) {
      if (field instanceof PDChoice) {
        if (field instanceof PDListBox) {
          PDListBox listBoxField = (PDListBox) field;
          if (answerText == null || answerText.isEmpty() || answerText.equals("Off")) {
            return;
          }
          List<String> values = new LinkedList<>();
          for (Object value : new JSONArray(answerText)) {
            String stringValue = (String) value;
            values.add(stringValue);
          }
          filledQuestion.setAnswerText(values.toString());
          listBoxField.setValue(values);
        } else if (field instanceof PDComboBox) {
          PDComboBox comboBoxField = (PDComboBox) field;
          filledQuestion.setAnswerText(answerText);
          comboBoxField.setValue(answerText);
        }
      } else if (field instanceof PDTextField) {
        filledQuestion.setAnswerText(answerText);
        field.setValue(answerText);
      }
    } else if (field instanceof PDSignatureField) {
      // Handled in signPDF
    }
  }

  public Message mergeFileAndFormAnswers(InputStream templateFileStream) {
    try {
      this.pdfDocument = Loader.loadPDF(templateFileStream);
    } catch (IOException e) {
      return PdfMessage.INVALID_PDF;
    }
    this.pdfDocument.setAllSecurityToBeRemoved(true);
    PDAcroForm acroForm = this.pdfDocument.getDocumentCatalog().getAcroForm();
    if (acroForm == null) {
      return PdfMessage.INVALID_PDF;
    }
    try {
      fillFieldsFromAnswers(acroForm);
      if (this.signatureStream != null) {
        signPDF();
      }

      this.filledFileOutputStream = new ByteArrayOutputStream();
      this.pdfDocument.save(this.filledFileOutputStream);
      this.pdfDocument.close();
      this.filledFileStream = new ByteArrayInputStream(this.filledFileOutputStream.toByteArray());
      return null;
    } catch (Exception e) {
      log.error("Error merging file and form questions: {}", e.getMessage(), e);
      return PdfMessage.SERVER_ERROR;
    }
  }

  public void signPDF() throws IOException {
    PDVisibleSignDesigner visibleSignDesigner = new PDVisibleSignDesigner(this.signatureStream);
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
    signature.setName(username);
    signature.setSignDate(Calendar.getInstance());

    for (PDSignatureField signatureField : findSignatureFields()) {
      signatureField.setValue(signature);
    }

    this.pdfDocument.addSignature(signature, signatureOptions);
  }

  public List<PDSignatureField> findSignatureFields() {
    List<PDSignatureField> signatureFields = new LinkedList<>();
    List<PDField> fields = new LinkedList<>();
    fields.addAll(this.pdfDocument.getDocumentCatalog().getAcroForm().getFields());
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

  public Message createNewFileAndForm() {
    InputStream filledFileEncryptedStream;
    try {
      filledFileEncryptedStream =
          FileStorageCryptoPolicy.prepareForStorage(
              this.filledFileStream,
              FileType.APPLICATION_PDF,
              this.username,
              this.encryptionController);
    } catch (GeneralSecurityException | IOException e) {
      log.error("Error encrypting filled file for user '{}': {}", this.username, e.getMessage(), e);
      return PdfMessage.SERVER_ERROR;
    }
    this.filledFile =
        new File(
            this.username,
            new Date(),
            filledFileEncryptedStream,
            FileType.APPLICATION_PDF,
            IdCategoryType.NONE,
            this.templateFile.getFilename(),
            this.organizationName,
            true,
            this.templateFile.getContentType());
    ObjectId filledFileObjectId = this.filledFile.getId();
    this.filledForm =
        new Form(
            this.username,
            Optional.of(this.username),
            LocalDateTime.now(),
            Optional.of(LocalDateTime.now()),
            FormType.APPLICATION,
            false,
            new FormMetadata(
                this.templateFile.getFilename() + " Form",
                this.templateFile.getFilename() + " Form",
                "Pennsylvania",
                "Philadelphia",
                new HashSet<>(),
                LocalDateTime.now(),
                new ArrayList<>(),
                0),
            this.filledFormBody,
            new ObjectId(),
            "");
    this.filledForm.setFileId(filledFileObjectId);
    this.filledForm.setApplicationMetadata(extractMetadataFromAnswers(this.formAnswers));
    return PdfMessage.SUCCESS;
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

  public Message fill() {
    ObjectId fileObjectId = new ObjectId(this.fileId);
    Optional<File> templateFileOptional = this.fileDao.get(fileObjectId);
    if (templateFileOptional.isEmpty()) {
      return PdfMessage.NO_SUCH_FILE;
    }
    this.templateFile = templateFileOptional.get();
    InputStream templateFileStream;
    try {
      InputStream storedTemplateStream = this.fileDao.getStream(fileObjectId).get();
      byte[] templateBytes = storedTemplateStream.readAllBytes();
      templateFileStream =
          FileStorageCryptoPolicy.openForRead(
              templateBytes,
              this.templateFile.getFileType(),
              this.templateFile.getUsername(),
              this.encryptionController);
    } catch (Exception e) {
      log.error("Unable to load/decrypt template file '{}': {}", fileObjectId, e.getMessage(), e);
      return PdfMessage.SERVER_ERROR;
    }
    Message mergeMessage = mergeFileAndFormAnswers(templateFileStream);
    if (mergeMessage != null) {
      return mergeMessage;
    }
    return createNewFileAndForm();
  }
}
