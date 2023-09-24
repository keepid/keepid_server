package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.User.UserDao;
import File.File;
import File.FileType;
import File.IdCategoryType;
import Form.FieldType;
import Form.Form;
import Form.FormMetadata;
import Form.FormQuestion;
import Form.FormSection;
import Form.FormType;
import PDF.PdfAnnotationError;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.Services.GetUserInfoService;
import User.UserMessage;
import User.UserType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.bson.types.ObjectId;
import org.json.JSONObject;

public class UploadAnnotatedPDFServiceV2 implements Service {
  public static final int DEFAULT_FIELD_NUM_LINES = 3;
  public static final String successStatus = "Success";

  private FileDao fileDao;
  private FormDao formDao;
  private UserDao userDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private String fileName;
  private String fileContentType;
  private InputStream fileStream;
  private EncryptionController encryptionController;
  private List<FormQuestion> formQuestions;
  private JSONObject userInfo;
  private ObjectId uploadedFileId;
  private String fileOrganizationName;

  public UploadAnnotatedPDFServiceV2(
      FileDao fileDao,
      FormDao formDao,
      UserDao userDao,
      UserParams userParams,
      FileParams fileParams,
      EncryptionController encryptionController) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.userDao = userDao;
    this.username = userParams.getUsername();
    this.organizationName = userParams.getOrganizationName();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.fileName = fileParams.getFileName();
    this.fileContentType = fileParams.getFileContentType();
    this.fileStream = fileParams.getFileStream();
    this.fileOrganizationName = fileParams.getFileOrgName();
    this.encryptionController = encryptionController;
  }

  public ObjectId getUploadedFileId() {
    return this.uploadedFileId;
  }

  @Override
  public Message executeAndGetResponse() {
    Message uploadConditionsErrorMessage = checkUploadConditions();
    if (uploadConditionsErrorMessage != null) {
      return uploadConditionsErrorMessage;
    }
    GetUserInfoService getUserInfoService = new GetUserInfoService(userDao, username);
    Message getUserInfoServiceResponse = getUserInfoService.executeAndGetResponse();
    if (getUserInfoServiceResponse != UserMessage.SUCCESS) {
      return getUserInfoServiceResponse;
    }
    this.userInfo = getUserInfoService.getUserFields();
    return upload();
  }

  public Message checkUploadConditions() {
    if (this.fileStream == null || !this.fileContentType.equals("application/pdf")) {
      return PdfMessage.INVALID_PDF;
    }
    if (this.privilegeLevel != UserType.Developer) {
      return PdfMessage.INSUFFICIENT_PRIVILEGE;
    }
    return null;
  }

  public void generateFormQuestionFromFields(List<PDField> fields) throws Exception {
    for (PDField field : fields) {
      if (!(field instanceof PDNonTerminalField)) {
        FormQuestion generatedFormQuestion = generateFormQuestionFromTerminalField(field);
        if (generatedFormQuestion != null) {
          this.formQuestions.add(generatedFormQuestion);
        }
      } else {
        generateFormQuestionFromFields(((PDNonTerminalField) field).getChildren());
      }
    }
  }

  public FormQuestion getCheckBoxFormQuestion(PDCheckBox field) {
    ObjectId id = new ObjectId();
    FieldType type = FieldType.CHECKBOX;
    String questionName = field.getFullyQualifiedName();
    String questionText = questionName;
    String answerText = "";
    List<String> options = new LinkedList<>();
    options.add(field.getOnValue());
    String defaultValue = Boolean.FALSE.toString();
    boolean required = field.isRequired();
    int numLines = DEFAULT_FIELD_NUM_LINES;
    boolean matched = false;
    // New ObjectId is used, this value is not read unless needed
    // in which case this value will be replaced with the correct ObjectId
    ObjectId conditionalOnField = new ObjectId();
    String conditionalType = "NONE";
    return new FormQuestion(
        id,
        type,
        questionName,
        questionText,
        answerText,
        options,
        defaultValue,
        required,
        numLines,
        matched,
        conditionalOnField,
        conditionalType);
  }

  public FormQuestion getRadioButtonFormQuestion(PDRadioButton field) {
    ObjectId id = new ObjectId();
    FieldType type = FieldType.RADIO_BUTTON;
    String questionName = field.getFullyQualifiedName();
    String questionText = questionName;
    String answerText = "";
    List<String> options = new LinkedList<>(field.getOnValues());
    String defaultValue = "Off";
    boolean required = field.isRequired();
    int numLines = options.size();
    boolean matched = false;
    ObjectId conditionalOnField = new ObjectId();
    String conditionalType = "NONE";
    return new FormQuestion(
        id,
        type,
        questionName,
        questionText,
        answerText,
        options,
        defaultValue,
        required,
        numLines,
        matched,
        conditionalOnField,
        conditionalType);
  }

  public FormQuestion getChoiceFieldFormQuestion(PDChoice field) {
    ObjectId id = new ObjectId();
    FieldType type = (field instanceof PDComboBox) ? FieldType.COMBOBOX : FieldType.LISTBOX;
    String questionName = field.getFullyQualifiedName();
    String questionText =
        field.isMultiSelect()
            ? questionName + " (you can select multiple options with CTRL)"
            : questionName;
    String answerText = "";
    List<String> options = new LinkedList<>(field.getOptions());
    String defaultValue = "Off";
    boolean required = field.isRequired();
    int numLines = options.size();
    boolean matched = false;
    ObjectId conditionalOnField = new ObjectId();
    String conditionalType = "NONE";
    return new FormQuestion(
        id,
        type,
        questionName,
        questionText,
        answerText,
        options,
        defaultValue,
        required,
        numLines,
        matched,
        conditionalOnField,
        conditionalType);
  }

  public FormQuestion getTextFieldFormQuestion(PDTextField field) {
    ObjectId id = new ObjectId();
    FieldType type;
    String questionName = field.getFullyQualifiedName();
    String questionText = null;
    if (field.isReadOnly()) {
      type = FieldType.READ_ONLY_FIELD;
      String fieldValue = field.getValue();
      if (fieldValue != null && !fieldValue.equals("")) {
        questionText = questionName + ": " + fieldValue;
      }
    } else if (field.isMultiline()) {
      type = FieldType.MULTILINE_TEXT_FIELD;
      questionText = questionName;
    } else {
      type = FieldType.TEXT_FIELD;
      questionText = questionName;
    }
    String answerText = "";
    List<String> options = new LinkedList<>();
    String defaultValue = "";
    boolean required = field.isRequired();
    int numLines = DEFAULT_FIELD_NUM_LINES;
    boolean matched = false;
    ObjectId conditionalOnField = new ObjectId();
    String conditionalType = "NONE";
    return new FormQuestion(
        id,
        type,
        questionName,
        questionText,
        answerText,
        options,
        defaultValue,
        required,
        numLines,
        matched,
        conditionalOnField,
        conditionalType);
  }

  // Conditional fields may be deprecated from previous PdfController
  public void setMatchedAndConditionalFields(FormQuestion formQuestion) throws Exception {
    String questionName = formQuestion.getQuestionName();
    String[] splitQuestionName = questionName.split(":");
    if (splitQuestionName.length != 1
        && splitQuestionName.length != 2
        && splitQuestionName.length != 3) {
      throw new Exception("Invalid number of colons in questionName");
    }

    formQuestion.setQuestionText(splitQuestionName[0]);
    if (splitQuestionName.length == 1) {
      return;
    }
    String fieldTypeIndicatorString = splitQuestionName[1];
    if (fieldTypeIndicatorString.startsWith("+")) {
      // Positively linked field
      formQuestion.setConditionalType("POSITIVE");
      formQuestion.setConditionalOnField(new ObjectId(fieldTypeIndicatorString.substring(1)));
    } else if (fieldTypeIndicatorString.startsWith("-")) {
      // Negatively linked field
      formQuestion.setConditionalType("NEGATIVE");
      formQuestion.setConditionalOnField(new ObjectId(fieldTypeIndicatorString.substring(1)));
    } else if (fieldTypeIndicatorString.equals("anyDate")) {
      // Make it a date field that can be selected by the client
      formQuestion.setType(FieldType.DATE_FIELD);
    } else if (fieldTypeIndicatorString.equals("currentDate")) {
      // Make a date field with the current date that cannot be changed (value set on frontend)
      formQuestion.setType(FieldType.DATE_FIELD);
      formQuestion.setMatched(true);
    } else if (fieldTypeIndicatorString.equals("signature")) {
      // Signatures not handled in first round of form completion
      formQuestion.setType(FieldType.SIGNATURE);
    } else if (this.userInfo.has(fieldTypeIndicatorString)) {
      // Field has a matched database variable, so make that the autofilled value
      formQuestion.setMatched(true);
      formQuestion.setDefaultValue((String) this.userInfo.get(fieldTypeIndicatorString));
    } else {
      throw new Exception("FieldTypeIndicatorString invalid");
    }
  }

  public FormQuestion generateFormQuestionFromTerminalField(PDField field) throws Exception {
    FormQuestion generatedFormQuestion = null;
    if (field instanceof PDButton) {
      if (field instanceof PDCheckBox) {
        generatedFormQuestion = getCheckBoxFormQuestion((PDCheckBox) field);
      } else if (field instanceof PDPushButton) {
        // Do nothing for a push button, as we don't need to support them right now
        return null;
      } else if (field instanceof PDRadioButton) {
        generatedFormQuestion = getRadioButtonFormQuestion((PDRadioButton) field);
      }
    } else if (field instanceof PDVariableText) {
      if (field instanceof PDChoice) {
        generatedFormQuestion = getChoiceFieldFormQuestion((PDChoice) field);
      } else if (field instanceof PDTextField) {
        generatedFormQuestion = getTextFieldFormQuestion((PDTextField) field);
      }
    } else if (field instanceof PDSignatureField) {
      // Do nothing, as signatures are dealt with in findSignatureFields
      return null;
    } else {
      throw new Exception("Failed to generate FormQuestion");
    }

    try {
      setMatchedAndConditionalFields(generatedFormQuestion);
    } catch (Exception e) {
      throw new Exception("Failed to generate matched and conditional fields: " + e.getMessage());
    }
    return generatedFormQuestion;
  }

  public Message upload() {
    PDDocument pdfDocument;
    // Create copies of inputstream as Loader.loadPDF closes the parameter input stream
    ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream();
    try {
      fileStream.transferTo(fileOutputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      pdfDocument = Loader.loadPDF(new ByteArrayInputStream(fileOutputStream.toByteArray()));
    } catch (IOException e) {
      return PdfMessage.INVALID_PDF;
    }
    pdfDocument.setAllSecurityToBeRemoved(true);
    PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm();
    if (acroForm == null) {
      return PdfMessage.INVALID_PDF;
    }
    File file =
        new File(
            this.username,
            new Date(),
            new ByteArrayInputStream(fileOutputStream.toByteArray()),
            FileType.FORM,
            IdCategoryType.NONE,
            this.fileName,
            this.fileOrganizationName,
            true,
            this.fileContentType);
    ObjectId fileId = file.getId();
    this.formQuestions = new LinkedList<FormQuestion>();
    try {
      generateFormQuestionFromFields(acroForm.getFields());
    } catch (Exception e) {
      return new PdfAnnotationError(e.getMessage());
    }
    FormSection body = new FormSection("Form Body", "Form Body", new LinkedList<>(), formQuestions);
    Form form =
        new Form(
            this.username,
            Optional.of(this.username),
            LocalDateTime.now(),
            Optional.of(LocalDateTime.now()),
            FormType.FORM,
            true,
            new FormMetadata(
                this.fileName + " Form",
                this.fileName + " Form",
                "Pennsylvania",
                "Philadelphia",
                new HashSet<>(),
                LocalDateTime.now(),
                new ArrayList<>(),
                0),
            body,
            new ObjectId(),
            "");
    form.setFileId(fileId);
    fileDao.save(file);
    formDao.save(form);
    this.uploadedFileId = file.getId();
    return PdfMessage.SUCCESS;
  }
}
