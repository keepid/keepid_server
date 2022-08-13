package File.Services;

import Config.Message;
import Config.Service;
import File.FileMessage;
import User.UserType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class FillPDFFileService implements Service {
  UserType privilegeLevel;
  InputStream fileStream;
  JSONObject formAnswers;
  InputStream completedForm;

  public FillPDFFileService(
      UserType privilegeLevel, InputStream fileStream, JSONObject formAnswers) {
    this.privilegeLevel = privilegeLevel;
    this.fileStream = fileStream;
    this.formAnswers = formAnswers;
  }

  @Override
  public Message executeAndGetResponse() {
    if (fileStream == null) {
      return FileMessage.INVALID_FILE;
    } else {
      if (privilegeLevel == UserType.Client
          || privilegeLevel == UserType.Worker
          || privilegeLevel == UserType.Director
          || privilegeLevel == UserType.Admin) {
        try {
          return fillFields(fileStream, formAnswers);
        } catch (IOException e) {
          return FileMessage.SERVER_ERROR;
        }
      } else {
        return FileMessage.INSUFFICIENT_PRIVILEGE;
      }
    }
  }

  public InputStream getCompletedForm() {
    Objects.requireNonNull(completedForm);
    return completedForm;
  }

  public Message fillFields(InputStream inputStream, JSONObject formAnswers)
      throws IllegalArgumentException, IOException {
    if (inputStream == null || formAnswers == null) {
      return FileMessage.INVALID_FILE;
    }
    PDDocument pdfDocument = Loader.loadPDF(inputStream);
    pdfDocument.setAllSecurityToBeRemoved(true);
    PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm();
    if (acroForm == null) {
      return FileMessage.INVALID_FILE;
    }

    for (String fieldName : formAnswers.keySet()) {
      PDField field = acroForm.getField(fieldName);
      if (field instanceof PDButton) {
        if (field instanceof PDCheckBox) {
          PDCheckBox checkBoxField = (PDCheckBox) field;
          boolean formAnswer = formAnswers.getBoolean(fieldName);
          if (formAnswer) {
            checkBoxField.check();
          } else {
            checkBoxField.unCheck();
          }
        } else if (field instanceof PDPushButton) {
          // Do nothing. Maybe in the future make it clickable
        } else if (field instanceof PDRadioButton) {

          PDRadioButton radioButtonField = (PDRadioButton) field;
          String formAnswer = formAnswers.getString(fieldName);
          radioButtonField.setValue(formAnswer);
        }
      } else if (field instanceof PDVariableText) {
        if (field instanceof PDChoice) {
          if (field instanceof PDListBox) {
            PDListBox listBoxField = (PDListBox) field;
            List<String> values = new LinkedList<>();

            // Test that this throws an error when invalid values are passed
            for (Object value : formAnswers.getJSONArray(fieldName)) {
              String stringValue = (String) value;
              values.add(stringValue);
            }
            listBoxField.setValue(values);
          } else if (field instanceof PDComboBox) {
            PDComboBox listBoxField = (PDComboBox) field;
            String formAnswer = formAnswers.getString(fieldName);
            listBoxField.setValue(formAnswer);
          }
        } else if (field instanceof PDTextField) {
          String value = formAnswers.getString(fieldName);
          field.setValue(value);
        }
      } else if (field instanceof PDSignatureField) {
        // Do nothing, implemented separately in pdfSignedUpload
      }
    }
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    pdfDocument.save(outputStream);
    pdfDocument.close();

    this.completedForm = new ByteArrayInputStream(outputStream.toByteArray());
    return FileMessage.SUCCESS;
  }
}
