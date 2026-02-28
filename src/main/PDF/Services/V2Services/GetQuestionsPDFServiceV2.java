package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Database.Organization.OrgDao;
import Database.User.UserDao;
import Form.FieldType;
import Form.Form;
import Form.FormQuestion;
import Form.FormSection;
import Organization.Organization;
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
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

@Slf4j
public class GetQuestionsPDFServiceV2 implements Service {
  private FormDao formDao;
  private OrgDao orgDao;
  private UserDao userDao;
  private String username; // target client username
  private String actorUsername; // session user (worker/admin/client)
  private UserType privilegeLevel;
  private String organizationName;
  private String fileId;
  private JSONObject applicationInformation;
  private Form form;
  private Map<String, String> clientFlattenedFieldMap;
  private Map<String, String> workerFlattenedFieldMap;
  private Map<String, String> orgFlattenedFieldMap;
  private FormQuestion currentFormQuestion;

  /** Alias map: common alternative field names -> canonical flattened map keys. */
  private static final Map<String, String> FIELD_ALIASES = new HashMap<>();
  private static final Map<String, String> ORG_FIELD_ALIASES = new HashMap<>();

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

    ORG_FIELD_ALIASES.put("name", "organizationName");
    ORG_FIELD_ALIASES.put("orgName", "organizationName");
    ORG_FIELD_ALIASES.put("organizationName", "organizationName");
    ORG_FIELD_ALIASES.put("address", "address.line1");
    ORG_FIELD_ALIASES.put("city", "address.city");
    ORG_FIELD_ALIASES.put("state", "address.state");
    ORG_FIELD_ALIASES.put("zipcode", "address.zip");
    ORG_FIELD_ALIASES.put("zip", "address.zip");
    ORG_FIELD_ALIASES.put("phone", "phoneNumber");
    ORG_FIELD_ALIASES.put("phoneNumber", "phoneNumber");
    ORG_FIELD_ALIASES.put("email", "email");
    ORG_FIELD_ALIASES.put("website", "website");
    ORG_FIELD_ALIASES.put("ein", "ein");
  }

  public GetQuestionsPDFServiceV2(
      FormDao formDao, OrgDao orgDao, UserDao userDao, UserParams userParams, FileParams fileParams) {
    this.formDao = formDao;
    this.orgDao = orgDao;
    this.userDao = userDao;
    this.username = userParams.getUsername();
    this.actorUsername = userParams.getActorUsername();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.organizationName = userParams.getOrganizationName();
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
    Message loadProfileMapsResponse = loadDirectiveProfileMaps();
    if (loadProfileMapsResponse != UserMessage.SUCCESS) {
      return loadProfileMapsResponse;
    }
    return getQuestions();
  }

  private Message loadDirectiveProfileMaps() {
    GetUserInfoService clientUserInfoService = new GetUserInfoService(userDao, username);
    Message clientResponse = clientUserInfoService.executeAndGetResponse();
    if (clientResponse != UserMessage.SUCCESS) {
      return clientResponse;
    }
    this.clientFlattenedFieldMap = clientUserInfoService.getFlattenedFieldMap();

    String resolvedActorUsername =
        (actorUsername == null || actorUsername.isBlank()) ? username : actorUsername;
    GetUserInfoService workerUserInfoService = new GetUserInfoService(userDao, resolvedActorUsername);
    Message workerResponse = workerUserInfoService.executeAndGetResponse();
    if (workerResponse == UserMessage.SUCCESS) {
      this.workerFlattenedFieldMap = workerUserInfoService.getFlattenedFieldMap();
    } else {
      // Gracefully fallback so worker.* directives still resolve to something predictable.
      this.workerFlattenedFieldMap = this.clientFlattenedFieldMap;
    }

    this.orgFlattenedFieldMap = new HashMap<>();
    if (organizationName != null && !organizationName.isBlank()) {
      Optional<Organization> orgOpt = orgDao.get(organizationName);
      if (orgOpt.isPresent()) {
        flattenJSON(orgOpt.get().serialize(), "", this.orgFlattenedFieldMap);
      }
    }

    return UserMessage.SUCCESS;
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
      DirectiveScope directiveScope = getDirectiveScope(directive);
      String normalizedDirective = normalizeProfileDirective(directive);
      if (!matchComputedDirective(fq, normalizedDirective, directiveScope)) {
        matchFieldFromFlattenedMap(fq, normalizedDirective, directiveScope);
      }
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
  private void matchFieldFromFlattenedMap(
      FormQuestion fq, String directive, DirectiveScope directiveScope) {
    Map<String, String> sourceMap = getDirectiveSourceMap(directiveScope);
    Map<String, String> aliasMap = getDirectiveAliasMap(directiveScope);

    if (sourceMap == null) {
      log.warn(
          "Directive source map is null for scope '{}'; cannot match directive '{}'",
          directiveScope,
          directive);
      return;
    }

    // 1. Exact key match
    String value = sourceMap.get(directive);

    // 2. Alias lookup
    if (value == null && aliasMap.containsKey(directive)) {
      String aliasedKey = aliasMap.get(directive);
      value = sourceMap.get(aliasedKey);
    }

    // 3. Case-insensitive full-key match
    if (value == null) {
      for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
        if (entry.getKey().equalsIgnoreCase(directive)) {
          value = entry.getValue();
          break;
        }
      }
    }

    // 4. Case-insensitive leaf-key match (e.g. "firstname" matches "optionalInformation.person.firstName")
    if (value == null) {
      for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
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
          "Field directive '{}' not found in scope '{}' for user '{}'; skipping autofill",
          directive,
          directiveScope,
          this.username);
      fq.setMatched(false);
      fq.setDefaultValue("");
    }
  }

  private String normalizeProfileDirective(String directive) {
    if (directive == null) return "";
    String normalized = directive.trim();
    if (normalized.startsWith("client.")) normalized = normalized.substring("client.".length());
    else if (normalized.startsWith("worker.")) normalized = normalized.substring("worker.".length());
    else if (normalized.startsWith("org.")) normalized = normalized.substring("org.".length());
    return normalized;
  }

  private boolean matchComputedDirective(
      FormQuestion fq, String normalizedDirective, DirectiveScope directiveScope) {
    String normalized = normalizedDirective;
    if (normalized.startsWith("$")) {
      normalized = normalized.substring(1);
    }
    Map<String, String> sourceMap = getDirectiveSourceMap(directiveScope);
    String value = computeDirectiveValue(normalized, sourceMap, directiveScope);
    if (value == null || value.isEmpty()) {
      return false;
    }
    fq.setMatched(true);
    fq.setDefaultValue(value);
    return true;
  }

  private String computeDirectiveValue(
      String computedKey, Map<String, String> sourceMap, DirectiveScope directiveScope) {
    switch (computedKey) {
      case "age":
        return computeAge(sourceMap);
      case "birthYear":
        return getBirthPart(Part.YEAR, sourceMap);
      case "birthMonth":
        return getBirthPart(Part.MONTH, sourceMap);
      case "birthDay":
        return getBirthPart(Part.DAY, sourceMap);
      case "primaryPhoneAreaCode":
        return getPrimaryPhonePart(Part.AREA_CODE, sourceMap);
      case "primaryPhoneTelephonePrefix":
        return getPrimaryPhonePart(Part.PREFIX, sourceMap);
      case "primaryPhoneLineNumber":
        return getPrimaryPhonePart(Part.LINE_NUMBER, sourceMap);
      case "fullName":
        return buildFullName(sourceMap);
      case "date":
        return LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
      case "fullPersonalAddress":
        return buildAddress(sourceMap, "personalAddress");
      case "fullMailAddress":
        return buildAddress(sourceMap, "mailAddress");
      case "fullAddress":
        if (directiveScope == DirectiveScope.ORG) {
          String orgAddress = buildAddress(sourceMap, "address");
          if (orgAddress != null) return orgAddress;
          return buildAddress(sourceMap, "orgAddress");
        }
        return buildAddress(sourceMap, "personalAddress");
      default:
        return null;
    }
  }

  private enum Part {
    YEAR,
    MONTH,
    DAY,
    AREA_CODE,
    PREFIX,
    LINE_NUMBER
  }

  private String computeAge(Map<String, String> sourceMap) {
    LocalDate birthDate = parseBirthDate(sourceMap);
    if (birthDate == null) return null;
    int years = Period.between(birthDate, LocalDate.now()).getYears();
    return years >= 0 ? Integer.toString(years) : null;
  }

  private String getBirthPart(Part part, Map<String, String> sourceMap) {
    LocalDate birthDate = parseBirthDate(sourceMap);
    if (birthDate == null) return null;
    switch (part) {
      case YEAR:
        return Integer.toString(birthDate.getYear());
      case MONTH:
        return Integer.toString(birthDate.getMonthValue());
      case DAY:
        return Integer.toString(birthDate.getDayOfMonth());
      default:
        return null;
    }
  }

  private LocalDate parseBirthDate(Map<String, String> sourceMap) {
    if (sourceMap == null) return null;
    String birthDateStr = sourceMap.get("birthDate");
    if (birthDateStr == null || birthDateStr.isEmpty()) return null;
    List<DateTimeFormatter> formatters =
        List.of(
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    for (DateTimeFormatter formatter : formatters) {
      try {
        return LocalDate.parse(birthDateStr, formatter);
      } catch (DateTimeParseException ignored) {
      }
    }
    return null;
  }

  private String getPrimaryPhonePart(Part part, Map<String, String> sourceMap) {
    String phone = getPrimaryPhoneNumber(sourceMap);
    if (phone == null) return null;
    String digitsOnly = phone.replaceAll("\\D", "");
    if (digitsOnly.length() < 10) return null;
    String lastTen = digitsOnly.substring(digitsOnly.length() - 10);
    switch (part) {
      case AREA_CODE:
        return lastTen.substring(0, 3);
      case PREFIX:
        return lastTen.substring(3, 6);
      case LINE_NUMBER:
        return lastTen.substring(6, 10);
      default:
        return null;
    }
  }

  private String getPrimaryPhoneNumber(Map<String, String> sourceMap) {
    if (sourceMap == null) return null;
    // Preferred lookup: find phoneBook.N.label == "primary", then get phoneBook.N.phoneNumber
    for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith("phoneBook.") && key.endsWith(".label")
          && "primary".equalsIgnoreCase(entry.getValue())) {
        String prefix = key.substring(0, key.length() - ".label".length());
        String phone = sourceMap.get(prefix + ".phoneNumber");
        if (phone != null && !phone.isEmpty()) return phone;
      }
    }
    // Fallbacks
    String fallback = sourceMap.get("phoneBook.0.phoneNumber");
    if (fallback != null && !fallback.isEmpty()) return fallback;
    return sourceMap.get("phone");
  }

  private String buildFullName(Map<String, String> sourceMap) {
    if (sourceMap == null) return null;
    String first = safeValue(sourceMap.get("currentName.first"));
    String middle = safeValue(sourceMap.get("currentName.middle"));
    String last = safeValue(sourceMap.get("currentName.last"));
    String suffix = safeValue(sourceMap.get("currentName.suffix"));
    String fullName = String.join(" ", List.of(first, middle, last, suffix)).trim();
    return fullName.isEmpty() ? null : fullName.replaceAll("\\s+", " ");
  }

  private String buildAddress(Map<String, String> sourceMap, String prefix) {
    if (sourceMap == null) return null;
    String line1 = safeValue(sourceMap.get(prefix + ".line1"));
    String line2 = safeValue(sourceMap.get(prefix + ".line2"));
    String city = safeValue(sourceMap.get(prefix + ".city"));
    String state = safeValue(sourceMap.get(prefix + ".state"));
    String zip = safeValue(sourceMap.get(prefix + ".zip"));

    List<String> chunks = new LinkedList<>();
    if (!line1.isEmpty()) chunks.add(line1);
    if (!line2.isEmpty()) chunks.add(line2);

    String cityStateZip = String.join(" ", List.of(city, state, zip)).trim().replaceAll("\\s+", " ");
    if (!cityStateZip.isEmpty()) chunks.add(cityStateZip);

    if (chunks.isEmpty()) return null;
    return String.join(", ", chunks);
  }

  private enum DirectiveScope {
    CLIENT,
    WORKER,
    ORG
  }

  private DirectiveScope getDirectiveScope(String directive) {
    if (directive == null) return DirectiveScope.CLIENT;
    String trimmed = directive.trim();
    if (trimmed.startsWith("worker.")) return DirectiveScope.WORKER;
    if (trimmed.startsWith("org.")) return DirectiveScope.ORG;
    return DirectiveScope.CLIENT;
  }

  private Map<String, String> getDirectiveSourceMap(DirectiveScope directiveScope) {
    switch (directiveScope) {
      case WORKER:
        return this.workerFlattenedFieldMap;
      case ORG:
        return this.orgFlattenedFieldMap;
      case CLIENT:
      default:
        return this.clientFlattenedFieldMap;
    }
  }

  private Map<String, String> getDirectiveAliasMap(DirectiveScope directiveScope) {
    return directiveScope == DirectiveScope.ORG ? ORG_FIELD_ALIASES : FIELD_ALIASES;
  }

  private void flattenJSON(JSONObject json, String prefix, Map<String, String> result) {
    if (json == null) return;
    String[] names = JSONObject.getNames(json);
    if (names == null) return;

    for (String key : names) {
      String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
      Object value = json.opt(key);
      if (value == null || JSONObject.NULL.equals(value)) continue;
      if (value instanceof JSONObject) {
        flattenJSON((JSONObject) value, fullKey, result);
      } else if (value instanceof JSONArray) {
        flattenJSONArray((JSONArray) value, fullKey, result);
      } else {
        result.put(fullKey, String.valueOf(value));
      }
    }
  }

  private void flattenJSONArray(JSONArray array, String prefix, Map<String, String> result) {
    for (int i = 0; i < array.length(); i++) {
      Object value = array.opt(i);
      if (value == null || JSONObject.NULL.equals(value)) continue;
      String fullKey = prefix + "." + i;
      if (value instanceof JSONObject) {
        flattenJSON((JSONObject) value, fullKey, result);
      } else if (value instanceof JSONArray) {
        flattenJSONArray((JSONArray) value, fullKey, result);
      } else {
        result.put(fullKey, String.valueOf(value));
      }
    }
  }

  private String safeValue(String value) {
    return value == null ? "" : value.trim();
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
      formField.put("fieldDirective", formQuestion.getDirective());
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
