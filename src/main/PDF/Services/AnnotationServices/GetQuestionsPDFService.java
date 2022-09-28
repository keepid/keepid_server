package PDF.Services.AnnotationServices;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import PDF.PdfAnnotationError;
import PDF.PdfMessage;
import User.Services.GetUserInfoService;
import User.UserMessage;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class GetQuestionsPDFService implements Service {
  public static final int DEFAULT_FIELD_NUM_LINES = 3;
  public static final String successStatus = "Success";

  UserType privilegeLevel;
  String username;
  JSONObject userInfo;
  InputStream fileStream;
  UserDao userDao;
  JSONObject applicationInformation;

  public GetQuestionsPDFService(
      UserDao userDao, UserType privilegeLevel, String username, InputStream fileStream) {
    this.userDao = userDao;
    this.privilegeLevel = privilegeLevel;
    this.username = username;
    this.fileStream = fileStream;
  }

  @Override
  public Message executeAndGetResponse() {
    // First, get the user's profile so we can autofill the field questions
    GetUserInfoService userInfoService = new GetUserInfoService(userDao, username);
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
            || privilegeLevel == UserType.Admin
            || privilegeLevel == UserType.Developer) {
          try {
            return getFieldInformation();
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

  public Message getFieldInformation() throws IOException {
    PDDocument pdfDocument = Loader.loadPDF(fileStream);
    pdfDocument.setAllSecurityToBeRemoved(true);
    JSONObject responseJSON = new JSONObject();
    List<JSONObject> fieldsJSON = new LinkedList<>();

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
      if (field instanceof PDNonTerminalField) {
        // If the field has children, continue recursing
        List<PDField> childrenFields = ((PDNonTerminalField) field).getChildren();
        fields.addAll(childrenFields);
      } else {
        JSONObject fieldJSON = null;
        // If the field is a leaf, then get the relevant data and return it
        if (field instanceof PDButton) {
          if (field instanceof PDCheckBox) {
            fieldJSON = getCheckBox((PDCheckBox) field);
          } else if (field instanceof PDPushButton) {
            // Do nothing for a push button, as we don't need to support them right now
          } else if (field instanceof PDRadioButton) {
            fieldJSON = getRadioButton((PDRadioButton) field);
          }
        } else if (field instanceof PDVariableText) {
          if (field instanceof PDChoice) {
            fieldJSON = getChoiceField((PDChoice) field);
          } else if (field instanceof PDTextField) {
            fieldJSON = getTextField((PDTextField) field);
          }
        } else if (field instanceof PDSignatureField) {
          // Do nothing, as signatures are dealt with in findSignatureFields
          continue;
        }

        // Check for an annotation error - if so, then return error with field name
        if (fieldJSON != null && !fieldJSON.get("fieldStatus").equals(successStatus)) {
          return new PdfAnnotationError(fieldJSON.getString("fieldStatus"));
        }
        fieldsJSON.add(fieldJSON);
      }
    }

    // Replace fieldLinkedTo with the actual field name it is linked to (currently ordering index)
    for (JSONObject fieldJSON : fieldsJSON) {
      if (!fieldJSON.getString("fieldLinkageType").equals("NONE")) {
        String fieldName = fieldJSON.getString("fieldName");
        String fieldLinkedToFieldOrdering = fieldJSON.getString("fieldLinkedTo");

        // Find the linked field name by the ordering index
        String fieldLinkedToFieldName = null;
        Iterator<JSONObject> fieldIterator = fieldsJSON.iterator();
        while (fieldIterator.hasNext() && fieldLinkedToFieldName == null) {
          JSONObject field = fieldIterator.next();
          if (field.getString("fieldOrdering").equals(fieldLinkedToFieldOrdering)) {
            fieldLinkedToFieldName = field.getString("fieldName");
          }
        }

//        if (fieldLinkedToFieldName == null) {
//          return new PdfAnnotationError(
//              "Field Directive not Understood for Field '" + fieldName + "'");
//        }
        fieldJSON.put("fieldLinkedTo", fieldLinkedToFieldName);
      }
    }

    pdfDocument.close();
    responseJSON.put("fields", fieldsJSON);
    this.applicationInformation = responseJSON;
    return PdfMessage.SUCCESS;
  }

  private JSONObject getTextField(PDTextField field) {
    System.out.println("Mapping Name " + field.getMappingName());
    System.out.println("Fully Qualified Name " + field.getFullyQualifiedName());
    System.out.println("Alternative Field Name " + field.getAlternateFieldName());
    System.out.println(field);
    COSDictionary fieldDictionary = field.getCOSObject();

    for (COSName fieldAttribute: fieldDictionary.keySet()) {
      System.out.println("Attribute Name " + fieldAttribute);
      System.out.println("Attribute Value " + fieldDictionary.getItem(fieldAttribute));
    }

    String fieldName = field.getFullyQualifiedName();
    String fieldType;
    String fieldQuestion;
    if (field.isReadOnly()) {
      fieldType = "ReadOnlyField";
      fieldQuestion = fieldName;
      String fieldValue = field.getValue();
      if (fieldValue != null && !fieldValue.equals("")) {
        fieldQuestion += ": " + fieldValue;
      }
    } else if (field.isMultiline()) {
      fieldType = "MultilineTextField";
      fieldQuestion = "Please Enter Your: " + fieldName;
    } else {
      fieldType = "TextField";
      fieldQuestion = "Please Enter Your: " + fieldName;
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
    String fieldQuestion = "Please Select: " + fieldName;
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
    int numLines = fieldValueOptions.length();
    String fieldQuestion = "Please Select an Option for: " + fieldName;
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
    int numLines = fieldValueOptions.length();
    if (field.isMultiSelect()) {
      fieldQuestion =
          "Please Select Option(s) for: "
              + fieldName
              + " (you can select multiple options with CTRL)";
    } else {
      fieldQuestion = "Please Select an Option for: " + fieldName;
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

    JSONObject fieldJSON = new JSONObject();

    // TODO: Generalize to multiple field types - only textFields can be autofilled right now
    String[] splitFieldName = fieldName.split(":");
    if (splitFieldName.length != 1 && splitFieldName.length != 2 && splitFieldName.length != 3) {
      fieldJSON.put("fieldStatus", "Invalid Number of Colons for Field '" + fieldName + "'");
      return fieldJSON;
    } else {
      boolean fieldIsMatched = false;
      String fieldLinkageType = "NONE"; // None, Positive, Negative
      String fieldLinkedTo = ""; // Field name it is linked to

      String fieldNameBase = splitFieldName[0];
      // TODO: Make a better way of changing the question fieldName (as current method is clumsy)
      fieldQuestion = fieldQuestion.replaceFirst(fieldName, fieldNameBase);

      if (splitFieldName.length == 2 || splitFieldName.length == 3) {
        // Annotation for matched field - has directive at the end
        String fieldDirective = splitFieldName[1];
        if (fieldDirective.startsWith("+")) {
          // Positively linked field
          fieldLinkageType = "POSITIVE";
          fieldLinkedTo = fieldDirective.substring(1);
        } else if (fieldDirective.startsWith("-")) {
          // Negatively linked field
          fieldLinkageType = "NEGATIVE";
          fieldLinkedTo = fieldDirective.substring(1);
        } else if (fieldDirective.equals("anyDate")) {
          // Make it a date field that can be selected by the client
          fieldType = "DateField";
        } else if (fieldDirective.equals("currentDate")) {
          // Make a date field with the current date that cannot be changed (value set on frontend)
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
          String fieldStatus = "Field Directive not Understood for Field '" + fieldName + "'";
          fieldJSON.put("fieldStatus", fieldStatus);
          // return fieldJSON;
        }
      }

      fieldJSON.put("fieldName", fieldName);
      fieldJSON.put("fieldType", fieldType);
      fieldJSON.put("fieldValueOptions", fieldValueOptions);
      fieldJSON.put("fieldDefaultValue", fieldDefaultValue);
      fieldJSON.put("fieldIsRequired", fieldIsRequired);
      fieldJSON.put("fieldNumLines", fieldNumLines);
      fieldJSON.put("fieldIsMatched", fieldIsMatched);
      fieldJSON.put("fieldQuestion", fieldQuestion);
      fieldJSON.put("fieldLinkageType", fieldLinkageType);
      fieldJSON.put("fieldLinkedTo", fieldLinkedTo);
      fieldJSON.put("fieldStatus", successStatus);
      return fieldJSON;
    }
  }
}
