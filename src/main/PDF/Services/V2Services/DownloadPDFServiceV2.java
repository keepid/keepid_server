package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import File.File;
import Form.Form;
import Form.FormQuestion;
import PDF.PDFTypeV2;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.bson.types.ObjectId;
import org.json.JSONArray;

public class DownloadPDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private String fileId;
  private PDFTypeV2 pdfType;
  private InputStream downloadedInputStream;
  private EncryptionController encryptionController;

  public DownloadPDFServiceV2(
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
    this.pdfType = fileParams.getPdfType();
    this.encryptionController = encryptionController;
  }

  public InputStream getDownloadedInputStream() {
    return Objects.requireNonNull(this.downloadedInputStream);
  }

  @Override
  public Message executeAndGetResponse() {
    Message downloadConditionsErrorMessage = checkDownloadConditions();
    if (downloadConditionsErrorMessage != null) {
      return downloadConditionsErrorMessage;
    }
    return download();
  }

  public Message checkDownloadConditions() {
    if (!ValidationUtils.isValidObjectId(this.fileId) || this.pdfType == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    if (this.privilegeLevel == null) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
    }
    return null;
  }

  public void setPDFFieldsFromFormQuestions(List<FormQuestion> formQuestions, PDAcroForm acroForm)
      throws IOException {
    for (FormQuestion formQuestion : formQuestions) {
      PDField field = acroForm.getField(formQuestion.getQuestionName());
      if (field instanceof PDButton) {
        if (field instanceof PDCheckBox) {
          PDCheckBox checkBoxField = (PDCheckBox) field;
          boolean fieldAnswer = Boolean.parseBoolean(formQuestion.getAnswerText());
          if (fieldAnswer) {
            checkBoxField.check();
          } else {
            checkBoxField.unCheck();
          }
        } else if (field instanceof PDPushButton) {
          // Do nothing. Maybe in the future make it clickable
        } else if (field instanceof PDRadioButton) {
          PDRadioButton radioButtonField = (PDRadioButton) field;
          String fieldAnswer = formQuestion.getAnswerText();
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
            listBoxField.setValue(values);
          } else if (field instanceof PDComboBox) {
            PDComboBox listBoxField = (PDComboBox) field;
            String formAnswer = formQuestion.getAnswerText();
            listBoxField.setValue(formAnswer);
          }
        } else if (field instanceof PDTextField) {
          String value = formQuestion.getAnswerText();
          field.setValue(value);
        }
      } else if (field instanceof PDSignatureField) {
        // Do nothing, implemented separately in pdfSignedUpload
      }
    }
  }

  public Message mergeFileAndForm(InputStream fileStream, List<FormQuestion> formQuestions) {
    PDDocument pdfDocument;
    try {
      pdfDocument = Loader.loadPDF(fileStream);
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
      this.downloadedInputStream = new ByteArrayInputStream(outputStream.toByteArray());
      return PdfMessage.SUCCESS;
    } catch (Exception e) {
      return PdfMessage.SERVER_ERROR;
    }
  }

  public Message download() {
    ObjectId fileObjectId = new ObjectId(this.fileId);
    Optional<File> fileOptional = this.fileDao.get(fileObjectId);
    if (fileOptional.isEmpty()) {
      return PdfMessage.NO_SUCH_FILE;
    }
    if (this.pdfType == PDFTypeV2.BLANK_APPLICATION
        || this.pdfType == PDFTypeV2.CLIENT_UPLOADED_DOCUMENT) {
      try {
        this.downloadedInputStream =
            this.encryptionController.decryptFile(
                this.fileDao.getStream(fileObjectId).get(), this.username);
      } catch (Exception e) {
        return PdfMessage.SERVER_ERROR;
      }
      return PdfMessage.SUCCESS;
    }
    // PDFTypeV2.ANNOTATED_APPLICATION
    Optional<Form> formOptional = this.formDao.getByFileId(fileObjectId);
    if (formOptional.isEmpty()) {
      return PdfMessage.MISSING_FORM;
    }
    Form form = formOptional.get();
    List<FormQuestion> formQuestions = form.getAllQuestionsFromForm();
    InputStream fileStream = null;
    try {
      fileStream =
          this.encryptionController.decryptFile(
              this.fileDao.getStream(fileObjectId).get(), this.username);
    } catch (Exception e) {
      return PdfMessage.SERVER_ERROR;
    }
    return mergeFileAndForm(fileStream, formQuestions);
  }
}
