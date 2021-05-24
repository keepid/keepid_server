package PDF.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import PDF.PdfAnnotationError;
import PDF.PdfMessage;
import User.Services.GetUserInfoService;
import User.UserMessage;
import User.UserType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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
            log.info(e.getMessage());
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
    PDDocument pdfDocument = PDDocument.load(fileStream);
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
      if (!fieldJSON.getString("fieldLinkage").equals("NONE")) {
        String fieldName = fieldJSON.getString("fieldName");
        String fieldLinkedToFieldOrdering = fieldJSON.getString("fieldLinkedToName");

        // Find the linked field name by the ordering index
        String fieldLinkedToName = null;
        String fieldLinkedToType = null;
        Iterator<JSONObject> fieldIterator = fieldsJSON.iterator();
        while (fieldIterator.hasNext() && fieldLinkedToName == null) {
          JSONObject field = fieldIterator.next();
          if (field.getString("fieldOrdering").equals(fieldLinkedToFieldOrdering)) {
            fieldLinkedToName = field.getString("fieldName");
            fieldLinkedToType = field.getString("fieldType");
          }
        }

        if (fieldLinkedToName == null || fieldLinkedToType == null) {
          return new PdfAnnotationError(
              "Field Directive not Understood for Field '" + fieldName + "'");
        }
        fieldJSON.put("fieldLinkedToName", fieldLinkedToName);
        fieldJSON.put("fieldLinkedToType", fieldLinkedToType);
      }
    }

    // Sort fields by their ordering index
    try {
      fieldsJSON.sort(new FieldOrderingComparator());
    } catch (IllegalArgumentException exception) {
      return new PdfAnnotationError(
          "Field Orderings for Two Fields Are the Same: " + exception.getMessage());
    }

    pdfDocument.close();
    responseJSON.put("fields", fieldsJSON);
    this.applicationInformation = responseJSON;
    return PdfMessage.SUCCESS;
  }

  private JSONObject getTextField(PDTextField field) {
    String fieldName = field.getFullyQualifiedName();
    String fieldType;
    String fieldDefaultValue = "";
    if (field.isReadOnly()) {
      fieldType = "ReadOnlyField";
    } else if (field.isMultiline()) {
      fieldType = "MultilineTextField";
    } else {
      fieldType = "TextField";
    }
    JSONArray fieldValueOptions = new JSONArray();
    Boolean fieldIsRequired = field.isRequired();
    int numLines = DEFAULT_FIELD_NUM_LINES;
    return createFieldJSONEntry(
        fieldName, fieldType, fieldValueOptions, fieldDefaultValue, fieldIsRequired, numLines);
  }

  private JSONObject getCheckBox(PDCheckBox field) {
    String fieldName = field.getFullyQualifiedName();
    String fieldType = "CheckBox";
    JSONArray fieldValueOptions = new JSONArray();
    fieldValueOptions.put(field.getOnValue());
    Boolean fieldDefaultValue = Boolean.FALSE;
    Boolean fieldIsRequired = field.isRequired();
    int numLines = DEFAULT_FIELD_NUM_LINES;
    return createFieldJSONEntry(
        fieldName, fieldType, fieldValueOptions, fieldDefaultValue, fieldIsRequired, numLines);
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
    return createFieldJSONEntry(
        fieldName, fieldType, fieldValueOptions, fieldDefaultValue, fieldIsRequired, numLines);
  }

  private JSONObject getChoiceField(PDChoice field) {
    String fieldName = field.getFullyQualifiedName();
    String fieldType;
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
    return createFieldJSONEntry(
        fieldName, fieldType, fieldValueOptions, fieldDefaultValue, fieldIsRequired, numLines);
  }

  private JSONObject createFieldJSONEntry(
      String fieldName,
      String fieldType,
      JSONArray fieldValueOptions,
      Object fieldDefaultValue,
      boolean fieldIsRequired,
      int fieldNumLines) {

    JSONObject fieldJSON = new JSONObject();
    // TODO: Generalize to multiple field types - only textFields can be autofilled right now
    String[] splitFieldName = fieldName.split(":");
    if (splitFieldName.length != 2 && splitFieldName.length != 3) {
      fieldJSON.put("fieldStatus", "Invalid Number of Colons for Field '" + fieldName + "'");
      return fieldJSON;
    } else {
      boolean fieldIsMatched = false;
      String fieldLinkage = "NONE"; // None, Positive, Negative
      String fieldLinkedToName = ""; // Field name it is linked to
      String fieldLinkedToType = "";

      String fieldOrdering = splitFieldName[0];
      if (!fieldOrdering.matches("[0-9.]*")) {
        // Field ordering has invalid character
        String fieldStatus = "Invalid Field Ordering for Field '" + fieldName + "'";
        fieldJSON.put("fieldStatus", fieldStatus);
        return fieldJSON;
      }

      String fieldNameBase = splitFieldName[1];
      if (splitFieldName.length == 3) {
        // Annotation for matched field - has directive at the end
        String fieldDirective = splitFieldName[2];
        if (fieldDirective.startsWith("+")) {
          // Positively linked field
          fieldLinkage = "POSITIVE";
          fieldLinkedToName = fieldDirective.substring(1);
        } else if (fieldDirective.startsWith("-")) {
          // Negatively linked field
          fieldLinkage = "NEGATIVE";
          fieldLinkedToName = fieldDirective.substring(1);
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
          return fieldJSON;
        }
      }

      String fieldQuestion = getFieldQuestion(fieldType, fieldNameBase);
      fieldJSON.put("fieldName", fieldName);
      fieldJSON.put("fieldNameBase", fieldNameBase);
      fieldJSON.put("fieldOrdering", fieldOrdering);
      fieldJSON.put("fieldType", fieldType);
      fieldJSON.put("fieldValueOptions", fieldValueOptions);
      fieldJSON.put("fieldDefaultValue", fieldDefaultValue);
      fieldJSON.put("fieldIsRequired", fieldIsRequired);
      fieldJSON.put("fieldNumLines", fieldNumLines);
      fieldJSON.put("fieldIsMatched", fieldIsMatched);
      fieldJSON.put("fieldQuestion", fieldQuestion);
      fieldJSON.put("fieldLinkage", fieldLinkage);
      fieldJSON.put("fieldLinkedToName", fieldLinkedToName);
      fieldJSON.put("fieldLinkedToType", fieldLinkedToType);
      fieldJSON.put("fieldStatus", successStatus);
      return fieldJSON;
    }
  }

  private String getFieldQuestion(String fieldType, String fieldName) {
    String fieldQuestion;
    if (fieldType.equals("ReadOnlyField")) {
      fieldQuestion = fieldName += ": ";
    } else if (fieldType.equals("MultilineTextField")) {
      fieldQuestion = "Please Enter Your: " + fieldName;
    } else if (fieldType.equals("TextField")) {
      fieldQuestion = "Please Enter Your: " + fieldName;
    } else if (fieldType.equals("CheckBox")) {
      fieldQuestion = "Please Select: " + fieldName;
    } else if (fieldType.equals("RadioButton")) {
      fieldQuestion = "Please Select One Option for: " + fieldName;
    } else if (fieldType.equals("ComboBox")) {
      fieldQuestion = "Please Select One Option for: " + fieldName;
    } else if (fieldType.equals("ListBox")) {
      fieldQuestion =
          "Please Select Option(s) for: "
              + fieldName
              + " (you can select multiple options with CTRL)";
    } else if (fieldType.equals("SignatureField")) {
      fieldQuestion = "";
    } else {
      // Should never be reachable - so we will return a blank question
      fieldQuestion = "";
    }
    return fieldQuestion;
  }

  private class FieldOrderingComparator implements Comparator<JSONObject> {
    @Override
    public int compare(JSONObject fieldJSON1, JSONObject fieldJSON2)
        throws IllegalArgumentException {
      String fieldOrdering1 = fieldJSON1.getString("fieldOrdering");
      String fieldOrdering2 = fieldJSON2.getString("fieldOrdering");
      String[] splitFieldOrdering1 = fieldOrdering1.split("\\.");
      String[] splitFieldOrdering2 = fieldOrdering2.split("\\.");

      int i = 0;
      while (i < splitFieldOrdering1.length && i < splitFieldOrdering2.length) {
        int ordering1 = Integer.parseInt(splitFieldOrdering1[i]);
        int ordering2 = Integer.parseInt(splitFieldOrdering2[i]);
        if (ordering1 > ordering2) {
          return 1;
        } else if (ordering1 < ordering2) {
          return -1;
        } // otherwise keep checking next subsection ordering

        i++;
      }

      // If they are both are the same or cannot be compared, through IllegalArgumentException
      throw new IllegalArgumentException(
          "'"
              + fieldJSON1.getString("fieldName")
              + "' and '"
              + fieldJSON2.getString("fieldName")
              + "'");
    }
  }
}
