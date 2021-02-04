package PDF.Services;

import Config.Message;
import Config.Service;
import PDF.PdfMessage;
import User.Services.GetUserInfoService;
import User.UserMessage;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class GetQuestionsPDFService implements Service {
  public static final int DEFAULT_FIELD_NUM_LINES = 3;

  UserType privilegeLevel;
  String username;
  JSONObject userInfo;
  InputStream fileStream;
  MongoDatabase db;
  Logger logger;
  JSONObject applicationInformation;

  public GetQuestionsPDFService(
      MongoDatabase db,
      Logger logger,
      UserType privilegeLevel,
      String username,
      InputStream fileStream) {
    this.db = db;
    this.logger = logger;
    this.privilegeLevel = privilegeLevel;
    this.username = username;
    this.fileStream = fileStream;
  }

  @Override
  public Message executeAndGetResponse() {
    // First, get the user's profile so we can autofill the field questions
    GetUserInfoService userInfoService = new GetUserInfoService(db, logger, username);
    Message userInfoServiceResponse = userInfoService.executeAndGetResponse();
    if (userInfoServiceResponse != UserMessage.SUCCESS) {
      return PdfMessage.SERVER_ERROR;
    } else {
      this.userInfo = userInfoService.getUserFields();
      if (fileStream == null) {
        return PdfMessage.INVALID_PDF;
      } else {
        if (privilegeLevel == UserType.Client
            || privilegeLevel == UserType.Worker
            || privilegeLevel == UserType.Director
            || privilegeLevel == UserType.Admin) {
          try {
            return getFieldInformation(fileStream);
          } catch (IOException e) {
            return PdfMessage.SERVER_ERROR;
          }
        } else {
          return PdfMessage.INSUFFICIENT_PRIVILEGE;
        }
      }
    }
  }

  public JSONObject getApplicationInformation() {
    Objects.requireNonNull(applicationInformation);
    return applicationInformation;
  }

  public Message getFieldInformation(InputStream inputStream) throws IOException {
    PDDocument pdfDocument = PDDocument.load(inputStream);
    pdfDocument.setAllSecurityToBeRemoved(true);
    JSONObject responseJSON = new JSONObject();
    Map<PDRectangle, JSONObject> fieldsJSON = new HashMap<>();

    PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm();
    if (acroForm == null) {
      pdfDocument.close();
      return PdfMessage.INVALID_PDF;
    }

    // Report the Metadata
    PDDocumentInformation documentInformation = pdfDocument.getDocumentInformation();
    // TODO: Make this the filename the client side receives from metadata
    //  and pass it to this method
    responseJSON.put("title", documentInformation.getTitle());
    responseJSON.put("description", documentInformation.getSubject());

    // Make a copy of the fields
    List<PDField> fields = new LinkedList<>();
    fields.addAll(acroForm.getFields());

    while (!fields.isEmpty()) {
      PDField field = fields.remove(0);
      PDRectangle fieldRectangle = getFieldRectangle(field);
      if (field instanceof PDNonTerminalField) {
        // If the field has children, continue recursing
        List<PDField> childrenFields = ((PDNonTerminalField) field).getChildren();
        fields.addAll(childrenFields);
      } else {
        // If the field is a leaf, then get the relevant data and return it
        if (field instanceof PDButton) {
          if (field instanceof PDCheckBox) {
            fieldsJSON.put(fieldRectangle, getCheckBox((PDCheckBox) field));
          } else if (field instanceof PDPushButton) {
            // Do not do anything for a push button, as we don't need to support them right now
          } else if (field instanceof PDRadioButton) {
            fieldsJSON.put(fieldRectangle, getRadioButton((PDRadioButton) field));
          }
        } else if (field instanceof PDVariableText) {
          if (field instanceof PDChoice) {
            fieldsJSON.put(fieldRectangle, getChoiceField((PDChoice) field));
          } else if (field instanceof PDTextField) {
            fieldsJSON.put(fieldRectangle, getTextField((PDTextField) field));
          }
        } else if (field instanceof PDSignatureField) {
          // Do nothing, as signatures are dealt with in findSignatureFields
        }
      }
    }
    orderFields(fieldsJSON);
    responseJSON.put("fields", fieldsJSON);
    this.applicationInformation = responseJSON;

    pdfDocument.close();
    return PdfMessage.SUCCESS;
  }

  private JSONObject getTextField(PDTextField field) {
    String fieldName = field.getFullyQualifiedName();
    String fieldType;
    String fieldQuestion;
    if (field.isReadOnly()) {
      fieldType = "ReadOnlyField";
      fieldQuestion = field.getPartialName();
      String fieldValue = field.getValue();
      if (fieldValue != null && !fieldValue.equals("")) {
        fieldQuestion += ": " + fieldValue;
      }
    } else if (field.isMultiline()) {
      fieldType = "MultilineTextField";
      fieldQuestion = "Please Enter Your: " + field.getPartialName();
    } else {
      fieldType = "TextField";
      fieldQuestion = "Please Enter Your: " + field.getPartialName();
    }
    JSONArray fieldValueOptions = new JSONArray();
    String fieldDefaultValue = "";
    Boolean fieldIsRequired = field.isRequired();
    int numLines = DEFAULT_FIELD_NUM_LINES;
    return createFieldJSONEntry(
        fieldName,
        fieldType,
        fieldValueOptions,
        fieldDefaultValue,
        fieldIsRequired,
        numLines,
        fieldQuestion);
  }

  private JSONObject getCheckBox(PDCheckBox field) {
    String fieldName = field.getFullyQualifiedName();
    String fieldType = "CheckBox";
    JSONArray fieldValueOptions = new JSONArray();
    fieldValueOptions.put(field.getOnValue());
    Boolean fieldDefaultValue = Boolean.FALSE;
    Boolean fieldIsRequired = field.isRequired();
    int numLines = DEFAULT_FIELD_NUM_LINES;
    String fieldQuestion = "Please Select: " + field.getPartialName();
    return createFieldJSONEntry(
        fieldName,
        fieldType,
        fieldValueOptions,
        fieldDefaultValue,
        fieldIsRequired,
        numLines,
        fieldQuestion);
  }

  private JSONObject getRadioButton(PDRadioButton field) {
    String fieldName = field.getFullyQualifiedName();
    String fieldType = "RadioButton";
    JSONArray fieldValueOptions = new JSONArray();
    for (String choice : field.getOnValues()) {
      fieldValueOptions.put(choice);
    }
    String fieldDefaultValue = "Off";
    Boolean fieldIsRequired = field.isRequired();
    int numLines = 2 + fieldValueOptions.length();
    String fieldQuestion = "Please Select One Option for: " + field.getPartialName();
    return createFieldJSONEntry(
        fieldName,
        fieldType,
        fieldValueOptions,
        fieldDefaultValue,
        fieldIsRequired,
        numLines,
        fieldQuestion);
  }

  private JSONObject getChoiceField(PDChoice field) {
    String fieldName = field.getFullyQualifiedName();
    String fieldType, fieldQuestion;
    if (field instanceof PDComboBox) {
      fieldType = "ComboBox";
    } else {
      fieldType = "ListBox";
    }
    JSONArray fieldValueOptions = new JSONArray();
    for (String choice : field.getOptions()) {
      fieldValueOptions.put(choice);
    }
    String fieldDefaultValue = "Off";
    Boolean fieldIsRequired = field.isRequired();
    int numLines = fieldValueOptions.length() + 2;
    if (field.isMultiSelect()) {
      fieldQuestion =
          "Please Select Option(s) for: "
              + field.getPartialName()
              + " (you can select multiple options with CTRL)";
    } else {
      fieldQuestion = "Please Select an Option for: " + field.getPartialName();
    }
    return createFieldJSONEntry(
        fieldName,
        fieldType,
        fieldValueOptions,
        fieldDefaultValue,
        fieldIsRequired,
        numLines,
        fieldQuestion);
  }

  private JSONObject createFieldJSONEntry(
      String fieldName,
      String fieldType,
      JSONArray fieldValueOptions,
      Object fieldDefaultValue,
      boolean fieldIsRequired,
      int fieldNumLines,
      String fieldQuestion) {

    boolean fieldIsMatched = false;

    // TODO: Generalize to multiple field types - only textFields can be autofilled right now
    // Parse field matching directives in the field name
    String[] splitFieldName = fieldName.split(":");
    if (splitFieldName.length == 2) {
      // Annotation for matched field
      String fieldNameBase = splitFieldName[0];
      String fieldDirective = splitFieldName[1];

      // TODO: Make a better way of changing the question fieldName (as current method is clumsy)
      fieldQuestion = fieldQuestion.replaceFirst(fieldName, fieldNameBase);

      if (fieldDirective.equals("anyDate")) {
        // Make it a date field that can be selected by the client
        fieldType = "DateField";
      } else if (fieldDirective.equals("currentDate")) {
        // Make it a date field with the current date that cannot be changed (value set on frontend)
        fieldType = "DateField";
        fieldIsMatched = true;
      } else if (fieldDirective.equals("signature")) {
        // Signatures not handled in first round of form completion
        fieldType = "SignatureField";
      } else if (this.userInfo.has(fieldDirective)) {
        // Field has a matched database variable, so make that the autofilled value
        fieldIsMatched = true;
        fieldDefaultValue = this.userInfo.getString(fieldDirective);
      } else {
        // Match not found - treat as normal field
        logger.error("Error in Annotation for Field: " + fieldName + " - Directive not Understood");
      }
    } else if (splitFieldName.length > 2) {
      // Error in annotation, invalid format - treat as normal field
      logger.error("Error in Annotation for Field: " + fieldName + " - Invalid Format");
    }

    JSONObject fieldJSON = new JSONObject();
    fieldJSON.put("fieldName", fieldName);
    fieldJSON.put("fieldType", fieldType);
    fieldJSON.put("fieldValueOptions", fieldValueOptions);
    fieldJSON.put("fieldDefaultValue", fieldDefaultValue);
    fieldJSON.put("fieldIsRequired", fieldIsRequired);
    fieldJSON.put("fieldNumLines", fieldNumLines);
    fieldJSON.put("fieldIsMatched", fieldIsMatched);
    fieldJSON.put("fieldQuestion", fieldQuestion);
    return fieldJSON;
  }

  // Source:
  // https://stackoverflow.com/questions/14868059/how-to-get-the-position-of-a-field-using-pdfbox
  private PDRectangle getFieldRectangle(PDField field) {
    COSDictionary fieldDict = field.getCOSObject();
    COSArray fieldAreaArray = (COSArray) fieldDict.getDictionaryObject(COSName.RECT);
    return new PDRectangle(fieldAreaArray);
  }

  // Order fields based on bounding rectangle location.
  // Note: Only works if equals() in map only checks object reference equality,
  // else there could be problems with two rectangles with the same location
  private void orderFields(Map<PDRectangle, JSONObject> fieldsJSON) {}
}
