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
    FIELD_ALIASES.put("emailAddress", "email");
    FIELD_ALIASES.put("phoneNumber", "phone");
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
    String questionName = fq.getQuestionName();

    // Split on the LAST colon so that colons in question text are preserved.
    // e.g. "Question:with:colons:fieldName" -> questionText="Question:with:colons", directive="fieldName"
    int lastColonIndex = questionName.lastIndexOf(':');
    if (lastColonIndex < 0) {
      // No colon at all -- just question text, no directive
      fq.setQuestionText(questionName);
      return null;
    }

    String questionText = questionName.substring(0, lastColonIndex);
    String directive = questionName.substring(lastColonIndex + 1);
    fq.setQuestionText(questionText);

    if (directive.isEmpty()) {
      // Trailing colon with no directive -- treat as no directive
      return null;
    }

    // Special directives (keep existing behavior)
    if (directive.startsWith("+")) {
      // Positively linked field
      fq.setConditionalType("POSITIVE");
      fq.setConditionalOnField(new ObjectId(directive.substring(1)));
    } else if (directive.startsWith("-")) {
      // Negatively linked field
      fq.setConditionalType("NEGATIVE");
      fq.setConditionalOnField(new ObjectId(directive.substring(1)));
    } else if (directive.equals("anyDate")) {
      // Make it a date field that can be selected by the client
      fq.setType(FieldType.DATE_FIELD);
    } else if (directive.equals("currentDate")) {
      // Make a date field with the current date that cannot be changed (value set on frontend)
      fq.setType(FieldType.DATE_FIELD);
      fq.setMatched(true);
    } else if (directive.equals("signature")) {
      // Signatures not handled in first round of form completion
      fq.setType(FieldType.SIGNATURE);
    } else {
      // Attempt field matching via flattened map
      matchFieldFromFlattenedMap(fq, directive);
    }
    this.currentFormQuestion = fq;
    return null;
  }

  /**
   * Attempts to match a directive against the flattened user field map. Checks the directive
   * directly first, then resolves aliases. If no match is found, logs a warning and leaves the
   * field unmatched (graceful degradation -- never errors).
   */
  private void matchFieldFromFlattenedMap(FormQuestion fq, String directive) {
    if (this.flattenedFieldMap == null) {
      log.warn("Flattened field map is null; cannot match directive '{}'", directive);
      return;
    }

    // Direct lookup
    String value = this.flattenedFieldMap.get(directive);

    // Alias lookup if direct lookup failed
    if (value == null && FIELD_ALIASES.containsKey(directive)) {
      String aliasedKey = FIELD_ALIASES.get(directive);
      value = this.flattenedFieldMap.get(aliasedKey);
    }

    if (value != null) {
      fq.setMatched(true);
      fq.setDefaultValue(value);
    } else {
      // Graceful degradation: log warning, leave matched=false, defaultValue=""
      log.warn(
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
      //      Message matchedFieldsMessage = setMatchedFields(formQuestion);
      //      if (matchedFieldsMessage != null) {
      //        return matchedFieldsMessage;
      //      }
      JSONObject formField = new JSONObject();
      formField.put("fieldName", formQuestion.getQuestionName());
      formField.put("fieldType", formQuestion.getType().toString());
      formField.put("fieldValueOptions", new JSONArray(formQuestion.getOptions()));
      formField.put("fieldDefaultValue", formQuestion.getDefaultValue());
      formField.put("fieldIsRequired", formQuestion.isRequired());
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
