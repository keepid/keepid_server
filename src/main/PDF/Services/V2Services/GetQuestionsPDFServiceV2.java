package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Database.User.UserDao;
import Form.FieldType;
import Form.Form;
import Form.FormQuestion;
import Form.FormSection;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import User.Services.GetUserInfoService;
import User.UserMessage;
import User.UserType;
import Validation.ValidationUtils;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

@Slf4j
public class GetQuestionsPDFServiceV2 implements Service {
  private FormDao formDao;
  private UserDao userDao;
  private String username;
  private UserType privilegeLevel;
  private String fileId;
  private JSONObject applicationInformation;
  private Form form;
  private Map<String, String> flattenedFieldMap;
  private FormQuestion currentFormQuestion;

  /** Alias map: common alternative field names -> canonical flattened map keys. */
  private static final Map<String, String> FIELD_ALIASES = new HashMap<>();

  static {
    FIELD_ALIASES.put("firstName", "currentName.first");
    FIELD_ALIASES.put("lastName", "currentName.last");
    FIELD_ALIASES.put("middleName", "currentName.middle");
    FIELD_ALIASES.put("phone", "phoneBook.0.phoneNumber");
    FIELD_ALIASES.put("phoneNumber", "phoneBook.0.phoneNumber");
    FIELD_ALIASES.put("address", "personalAddress.line1");
    FIELD_ALIASES.put("streetAddress", "personalAddress.line1");
    FIELD_ALIASES.put("city", "personalAddress.city");
    FIELD_ALIASES.put("state", "personalAddress.state");
    FIELD_ALIASES.put("zipcode", "personalAddress.zip");
    FIELD_ALIASES.put("genderAssignedAtBirth", "sex");
    FIELD_ALIASES.put("emailAddress", "email");
  }

  public GetQuestionsPDFServiceV2(
      FormDao formDao, UserDao userDao, UserParams userParams, FileParams fileParams) {
    this.formDao = formDao;
    this.userDao = userDao;
    this.username = userParams.getUsername();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.fileId = fileParams.getFileId();
    this.applicationInformation = new JSONObject();
  }

  public JSONObject getApplicationInformation() {
    return Objects.requireNonNull(applicationInformation);
  }

  @Override
  public Message executeAndGetResponse() {
    Message getQuestionsConditionsErrorMessage = checkGetQuestionsConditions();
    if (getQuestionsConditionsErrorMessage != null) {
      return getQuestionsConditionsErrorMessage;
    }
    GetUserInfoService getUserInfoService = new GetUserInfoService(userDao, username);
    Message getUserInfoServiceResponse = getUserInfoService.executeAndGetResponse();
    if (getUserInfoServiceResponse != UserMessage.SUCCESS) {
      return getUserInfoServiceResponse;
    }
    this.flattenedFieldMap = getUserInfoService.getFlattenedFieldMap();
    return getQuestions();
  }

  public Message checkGetQuestionsConditions() {
    if (privilegeLevel == null) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
    }
    if (!ValidationUtils.isValidObjectId(fileId)) {
      return PdfMessage.INVALID_PARAMETER;
    }
    ObjectId fileObjectId = new ObjectId(fileId);
    Optional<Form> formOptional = formDao.getByFileId(fileObjectId);
    if (formOptional.isEmpty()) {
      return PdfMessage.MISSING_FORM;
    }
    Form tempForm = formOptional.get();
    form = formDao.get(tempForm.getId()).get();
    return null;
  }

  public Message setMatchedFields(FormQuestion fq) {
    String directive = fq.getDirective();

    // Fall back to parsing questionName for legacy forms that lack a stored directive
    if (directive == null || directive.isEmpty()) {
      String questionName = fq.getQuestionName();
      int lastColonIndex = questionName.lastIndexOf(':');
      if (lastColonIndex >= 0) {
        directive = questionName.substring(lastColonIndex + 1);
        fq.setQuestionText(questionName.substring(0, lastColonIndex));
      } else {
        directive = questionName;
      }
    }

    String existingText = fq.getQuestionText();
    if (existingText == null || existingText.isEmpty()
        || existingText.equals(fq.getQuestionName())) {
      fq.setQuestionText(humanizeFieldName(fq.getQuestionName()));
    }

    if (directive == null || directive.isEmpty()) {
      this.currentFormQuestion = fq;
      return null;
    }

    if (directive.startsWith("+")) {
      fq.setConditionalType("POSITIVE");
      fq.setConditionalOnField(new ObjectId(directive.substring(1)));
    } else if (directive.startsWith("-")) {
      fq.setConditionalType("NEGATIVE");
      fq.setConditionalOnField(new ObjectId(directive.substring(1)));
    } else if (directive.equals("anyDate")) {
      fq.setType(FieldType.DATE_FIELD);
    } else if (directive.equals("currentDate")) {
      fq.setType(FieldType.DATE_FIELD);
      fq.setMatched(true);
    } else if (directive.equals("signature")) {
      fq.setType(FieldType.SIGNATURE);
    } else {
      matchFieldFromFlattenedMap(fq, directive);
    }
    this.currentFormQuestion = fq;
    return null;
  }

  /**
   * Converts a camelCase or PascalCase field name into a human-readable label.
   * e.g. "firstName" -> "First Name", "zipcode" -> "Zipcode", "LastName" -> "Last Name"
   */
  private static String humanizeFieldName(String fieldName) {
    if (fieldName == null || fieldName.isEmpty()) {
      return fieldName;
    }
    // Insert space before each uppercase letter that follows a lowercase letter
    String spaced = fieldName.replaceAll("([a-z])([A-Z])", "$1 $2");
    // Capitalize the first letter
    return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
  }

  /**
   * Attempts to match a directive against the flattened user field map.
   * Matching strategy (in order):
   *   1. Exact key match
   *   2. Alias lookup (e.g. emailAddress -> email)
   *   3. Case-insensitive match against full keys
   *   4. Case-insensitive match against leaf keys (last segment after last dot)
   *
   * If no match is found, logs a debug message and leaves the field unmatched
   * (graceful degradation -- never errors).
   */
  private void matchFieldFromFlattenedMap(FormQuestion fq, String directive) {
    if (this.flattenedFieldMap == null) {
      log.warn("Flattened field map is null; cannot match directive '{}'", directive);
      return;
    }

    // 1. Exact key match
    String value = this.flattenedFieldMap.get(directive);

    // 2. Alias lookup
    if (value == null && FIELD_ALIASES.containsKey(directive)) {
      String aliasedKey = FIELD_ALIASES.get(directive);
      value = this.flattenedFieldMap.get(aliasedKey);
    }

    // 3. Case-insensitive full-key match
    if (value == null) {
      for (Map.Entry<String, String> entry : this.flattenedFieldMap.entrySet()) {
        if (entry.getKey().equalsIgnoreCase(directive)) {
          value = entry.getValue();
          break;
        }
      }
    }

    // 4. Case-insensitive leaf-key match (e.g. "firstname" matches "optionalInformation.person.firstName")
    if (value == null) {
      for (Map.Entry<String, String> entry : this.flattenedFieldMap.entrySet()) {
        String key = entry.getKey();
        int lastDot = key.lastIndexOf('.');
        String leafKey = lastDot >= 0 ? key.substring(lastDot + 1) : key;
        if (leafKey.equalsIgnoreCase(directive)) {
          value = entry.getValue();
          break;
        }
      }
    }

    if (value != null) {
      fq.setMatched(true);
      fq.setDefaultValue(value);
    } else {
      log.debug(
          "Field directive '{}' not found in user profile for user '{}'; skipping autofill",
          directive,
          this.username);
      fq.setMatched(false);
      fq.setDefaultValue("");
    }
  }

  public Message getQuestions() {
    FormSection formBody = form.getBody();
    applicationInformation.put("title", formBody.getTitle());
    applicationInformation.put("description", formBody.getDescription());
    List<FormQuestion> formQuestions = formBody.getQuestions();
    List<JSONObject> formFields = new LinkedList<>();
    for (FormQuestion formQuestion : formQuestions) {
      setMatchedFields(formQuestion);
      JSONObject formField = new JSONObject();
      formField.put("fieldName", formQuestion.getQuestionName());
      formField.put("fieldType", formQuestion.getType().toString());
      formField.put("fieldValueOptions", new JSONArray(formQuestion.getOptions()));
      formField.put("fieldDefaultValue", formQuestion.getDefaultValue());
      // All fields are optional in the web form -- users can skip what they can't answer.
      // Unanswered fields are left blank in the filled PDF.
      formField.put("fieldIsRequired", false);
      formField.put("fieldNumLines", formQuestion.getNumLines());
      formField.put("fieldIsMatched", formQuestion.isMatched());
      formField.put("fieldQuestion", formQuestion.getQuestionText());
      formField.put("fieldLinkageType", formQuestion.getConditionalType());
      formField.put("fieldLinkedTo", formQuestion.getConditionalOnField());
      formField.put("fieldStatus", "SUCCESS");
      formFields.add(formField);
    }
    applicationInformation.put("fields", formFields);
    return PdfMessage.SUCCESS;
  }
}
