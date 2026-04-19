package File.Services;

import Config.Message;
import Config.Service;
import File.FileMessage;
import User.UserType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
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
  private static final int DEFAULT_FONT_SIZE = 14;
  private static final int MIN_FONT_SIZE = 8;
  private static final float FIELD_HEIGHT_FONT_RATIO = 0.58f;

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
        setFieldFontSize((PDVariableText) field, DEFAULT_FONT_SIZE);
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
    acroForm.setNeedAppearances(false);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    pdfDocument.save(outputStream);
    pdfDocument.close();

    this.completedForm = new ByteArrayInputStream(outputStream.toByteArray());
    return FileMessage.SUCCESS;
  }

  private static int resolveFontSizeForWidget(PDVariableText field, int targetPt) {
    try {
      List<PDAnnotationWidget> widgets = field.getWidgets();
      if (widgets != null && !widgets.isEmpty()) {
        PDRectangle rect = widgets.get(0).getRectangle();
        if (rect != null) {
          float h = Math.abs(rect.getHeight());
          if (h > 0) {
            int cap = (int) Math.floor(h * FIELD_HEIGHT_FONT_RATIO);
            return Math.max(MIN_FONT_SIZE, Math.min(targetPt, cap));
          }
        }
      }
    } catch (Exception ignored) {
    }
    return targetPt;
  }

  private void setFieldFontSize(PDVariableText field, int fontSize) {
    int resolvedPt = resolveFontSizeForWidget(field, fontSize);
    String daBefore = field.getDefaultAppearance();
    String da;
    if (daBefore != null && !daBefore.isEmpty()) {
      String replaced = daBefore.replaceFirst("(/\\S+\\s+)[\\d.]+\\s+Tf", "$1" + resolvedPt + " Tf");
      da = replaced.equals(daBefore) ? "/Helv " + resolvedPt + " Tf 0 g" : replaced;
    } else {
      da = "/Helv " + resolvedPt + " Tf 0 g";
    }
    field.setDefaultAppearance(da);
  }
}
