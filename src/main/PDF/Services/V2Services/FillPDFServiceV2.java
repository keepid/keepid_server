package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import File.File;
import File.FileType;
import File.IdCategoryType;
import Form.Form;
import Form.FormMetadata;
import Form.FormQuestion;
import Form.FormSection;
import Form.FormType;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.UserType;
import Validation.ValidationUtils;
import java.io.*;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
  private Form templateForm;
  private File filledFile;
  private Form filledForm;
  private FormSection filledFormBody;
  private PDDocument pdfDocument;
  private EncryptionController encryptionController;
  private ByteArrayOutputStream filledFileOutputStream;

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
    if (privilegeLevel == UserType.Developer) {
      return PdfMessage.INSUFFICIENT_PRIVILEGE;
    }
    return null;
  }

  public void setPDFFieldsFromFormQuestions(List<FormQuestion> formQuestions, PDAcroForm acroForm)
      throws IOException {
    List<FormQuestion> filledFormBodyQuestions = new LinkedList<>();
    for (FormQuestion formQuestion : formQuestions) {
      String fieldName = formQuestion.getQuestionName();
      if (!formAnswers.has(fieldName)) {
        continue;
      }
      String formAnswerText = String.valueOf(formAnswers.get(fieldName));
      formQuestion.setAnswerText(formAnswerText);
      FormQuestion filledFormNewQuestion = formQuestion.copyOfFormQuestion();
      PDField field = acroForm.getField(formQuestion.getQuestionName());
      if (field instanceof PDButton) {
        if (field instanceof PDCheckBox) {
          PDCheckBox checkBoxField = (PDCheckBox) field;
          boolean fieldAnswer = Boolean.parseBoolean(formQuestion.getAnswerText());
          filledFormNewQuestion.setAnswerText(Boolean.toString(fieldAnswer));
          if (fieldAnswer) {
            checkBoxField.check();
          } else {
            checkBoxField.unCheck();
          }
        } else if (field instanceof PDPushButton) {
          // Do nothing. Maybe in the future make it clickable
          continue;
        } else if (field instanceof PDRadioButton) {
          PDRadioButton radioButtonField = (PDRadioButton) field;
          String fieldAnswer = formQuestion.getAnswerText();
          filledFormNewQuestion.setAnswerText(fieldAnswer);
          radioButtonField.setValue(fieldAnswer);
        }
      } else if (field instanceof PDVariableText) {
        if (field instanceof PDChoice) {
          if (field instanceof PDListBox) {
            PDListBox listBoxField = (PDListBox) field;
            List<String> values = new LinkedList<>();
            for (Object value : new JSONArray(formQuestion.getAnswerText())) {
              String stringValue = (String) value;
              values.add(stringValue);
            }
            filledFormNewQuestion.setAnswerText(values.toString());
            listBoxField.setValue(values);
          } else if (field instanceof PDComboBox) {
            PDComboBox listBoxField = (PDComboBox) field;
            String formAnswer = formQuestion.getAnswerText();
            filledFormNewQuestion.setAnswerText(formAnswer);
            listBoxField.setValue(formAnswer);
          }
        } else if (field instanceof PDTextField) {
          String value = formQuestion.getAnswerText();
          filledFormNewQuestion.setAnswerText(value);
          field.setValue(value);
        }
      } else if (field instanceof PDSignatureField) {
        // Handled in signPDF
        continue;
      }
      filledFormBodyQuestions.add(filledFormNewQuestion);
    }

    // Set Current Date
    PDField currentDateField = acroForm.getField("currentDate");
    if (currentDateField != null) {
      LocalDate currentDate = LocalDate.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
      String formattedCurrentDate = currentDate.format(formatter);
      currentDateField.setValue(formattedCurrentDate);
    }

    this.filledFormBody =
        new FormSection(
            this.templateForm.getBody().getTitle(),
            this.templateForm.getBody().getDescription(),
            new ArrayList<>(),
            filledFormBodyQuestions);
  }

  public Message mergeFileAndFormQuestions(
      InputStream templateFileStream, List<FormQuestion> formQuestions) {
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
      setPDFFieldsFromFormQuestions(formQuestions, acroForm);
      if (this.signatureStream != null) {
        signPDF();
      }

      this.filledFileOutputStream = new ByteArrayOutputStream();
      this.pdfDocument.save(this.filledFileOutputStream);
      this.pdfDocument.close();
      this.filledFileStream = new ByteArrayInputStream(this.filledFileOutputStream.toByteArray());
      return null;
    } catch (Exception e) {
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
          this.encryptionController.encryptFile(this.filledFileStream, this.username);
    } catch (GeneralSecurityException | IOException e) {
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
    return PdfMessage.SUCCESS;
  }

  public List<FormQuestion> getAllQuestionsFromForm(FormSection formSection) {
    List<FormQuestion> formQuestions = new LinkedList<FormQuestion>();
    getAllQuestionsFromFormRecursion(formSection, formQuestions);
    return formQuestions;
  }

  public void getAllQuestionsFromFormRecursion(
      FormSection formSection, List<FormQuestion> formQuestions) {
    formQuestions.addAll(formSection.getQuestions());
    if (formSection.getSubsections().size() == 0) {
      return;
    }
    for (FormSection formSubsection : formSection.getSubsections()) {
      getAllQuestionsFromFormRecursion(formSubsection, formQuestions);
    }
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
      templateFileStream = this.fileDao.getStream(fileObjectId).get();
    } catch (Exception e) {
      return PdfMessage.NO_SUCH_FILE;
    }
    Optional<Form> templateFormOptional = this.formDao.getByFileId(fileObjectId);
    if (templateFormOptional.isEmpty()) {
      return PdfMessage.MISSING_FORM;
    }
    this.templateForm = templateFormOptional.get();
    List<FormQuestion> formQuestions = getAllQuestionsFromForm(templateForm.getBody());
    Message mergeMessage = mergeFileAndFormQuestions(templateFileStream, formQuestions);
    if (mergeMessage != null) {
      return mergeMessage;
    }
    return createNewFileAndForm();
  }
}
