package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import File.File;
import File.FileType;
import File.IdCategoryType;
import Form.Form;
import Form.FormQuestion;
import Form.FormSection;
import Form.FormType;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.UserType;
import Validation.ValidationUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
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
  private EncryptionController encryptionController;

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

  public InputStream getFilledFileStream() {
    return Objects.requireNonNull(filledFileStream);
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

  public void setPDFFieldsFromFormQuestions(List<FormQuestion> formQuestions, PDAcroForm acroForm)
      throws IOException {
    List<FormQuestion> filledFormBodyQuestions = new LinkedList<>();
    for (FormQuestion formQuestion : formQuestions) {
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
        // Do nothing, implemented separately in pdfSignedUpload
        continue;
      }
      filledFormBodyQuestions.add(filledFormNewQuestion);
    }
    this.filledFormBody =
        new FormSection(
            this.templateForm.getBody().getTitle(),
            this.templateForm.getBody().getDescription(),
            null,
            filledFormBodyQuestions);
  }

  public Message mergeFileAndForm(
      InputStream templateFileStream, List<FormQuestion> formQuestions) {
    PDDocument pdfDocument;
    try {
      pdfDocument = Loader.loadPDF(templateFileStream);
    } catch (IOException e) {
      return PdfMessage.INVALID_PDF;
    }
    pdfDocument.setAllSecurityToBeRemoved(true);
    PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm();
    if (acroForm == null) {
      return PdfMessage.INVALID_PDF;
    }
    try {
      setPDFFieldsFromFormQuestions(formQuestions, acroForm);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      pdfDocument.save(outputStream);
      pdfDocument.close();
      this.filledFileStream = new ByteArrayInputStream(outputStream.toByteArray());
      return null;
    } catch (Exception e) {
      return PdfMessage.SERVER_ERROR;
    }
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
    this.filledForm =
        new Form(
            this.username,
            Optional.of(this.username),
            LocalDateTime.now(),
            Optional.of(LocalDateTime.now()),
            FormType.APPLICATION,
            false,
            null,
            this.filledFormBody,
            null,
            "");
    return PdfMessage.SUCCESS;
  }

  public Message fill() {
    ObjectId fileObjectId = new ObjectId(this.fileId);
    Optional<File> templateFileOptional = this.fileDao.get(fileObjectId);
    if (templateFileOptional.isEmpty()) {
      return PdfMessage.NO_SUCH_FILE;
    }
    this.templateFile = templateFileOptional.get();
    InputStream templateFileStream = null;
    try {
      templateFileStream =
          this.encryptionController.decryptFile(
              this.fileDao.getStream(fileObjectId).get(), this.username);
    } catch (Exception e) {
      return PdfMessage.SERVER_ERROR;
    }
    Optional<Form> templateFormOptional = this.formDao.getByFileId(fileObjectId);
    if (templateFormOptional.isEmpty()) {
      return PdfMessage.MISSING_FORM;
    }
    this.templateForm = templateFormOptional.get();
    List<FormQuestion> formQuestions = templateForm.getAllQuestionsFromForm();
    Message mergeMessage = mergeFileAndForm(templateFileStream, formQuestions);
    if (mergeMessage != null) {
      return mergeMessage;
    }
    return createNewFileAndForm();
  }
}
