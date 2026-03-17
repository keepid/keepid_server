package Form;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Walks a JSON Schema + UI Schema pair and generates FormQuestion[] for PDF fill compatibility.
 * Every UI Schema Control that has options.pdfField becomes a FormQuestion whose questionName
 * is the PDF AcroForm field name.
 */
public class InteractiveFormConfigUtils {

  public static List<FormQuestion> generateFormQuestions(
      String jsonSchemaStr, String uiSchemaStr) {
    JSONObject jsonSchema = new JSONObject(jsonSchemaStr);
    JSONObject uiSchema = new JSONObject(uiSchemaStr);
    JSONObject properties =
        jsonSchema.has("properties") ? jsonSchema.getJSONObject("properties") : new JSONObject();
    Set<String> required = extractRequired(jsonSchema);

    List<FormQuestion> questions = new ArrayList<>();
    collectControls(uiSchema, properties, required, questions);
    return questions;
  }

  private static Set<String> extractRequired(JSONObject jsonSchema) {
    Set<String> required = new HashSet<>();
    if (jsonSchema.has("required")) {
      JSONArray arr = jsonSchema.getJSONArray("required");
      for (int i = 0; i < arr.length(); i++) {
        required.add(arr.getString(i));
      }
    }
    return required;
  }

  /**
   * Recursively walks the UI Schema tree collecting Controls with options.pdfField.
   */
  private static void collectControls(
      JSONObject element,
      JSONObject properties,
      Set<String> required,
      List<FormQuestion> questions) {

    String type = element.optString("type", "");

    if ("Control".equals(type)) {
      handleControl(element, properties, required, questions);
    }

    if (element.has("elements")) {
      JSONArray elements = element.getJSONArray("elements");
      for (int i = 0; i < elements.length(); i++) {
        collectControls(elements.getJSONObject(i), properties, required, questions);
      }
    }

    if (!"Control".equals(type)) {
      JSONObject elOptions = element.optJSONObject("options");
      if (elOptions != null) {
        if (elOptions.has("optionMappings")) {
          handleOptionMappings(elOptions.getJSONArray("optionMappings"), questions);
        }
        if (elOptions.has("conditionalFills")) {
          handleOptionMappings(elOptions.getJSONArray("conditionalFills"), questions);
        }
      }
    }
  }

  private static void handleControl(
      JSONObject control,
      JSONObject properties,
      Set<String> required,
      List<FormQuestion> questions) {

    JSONObject options = control.optJSONObject("options");
    if (options == null || !options.has("pdfField")) {
      if (options != null && options.has("optionMappings")) {
        handleOptionMappings(options.getJSONArray("optionMappings"), questions);
      }
      return;
    }

    String pdfFieldName = options.getString("pdfField");
    String label = control.optString("label", pdfFieldName);
    String propertyName = extractPropertyName(control.optString("scope", ""));
    String directive = resolveDirective(options);
    String fillValue = options.optString("fillValue", null);

    JSONObject propSchema =
        (propertyName != null && properties.has(propertyName))
            ? properties.getJSONObject(propertyName)
            : new JSONObject();

    FieldType fieldType = inferFieldType(propSchema, options);
    boolean isRequired = propertyName != null && required.contains(propertyName);
    List<String> fieldOptions = extractOptions(propSchema);

    String effectiveDirective = directive;
    if (effectiveDirective == null && fillValue != null) {
      effectiveDirective = fillValue;
    }

    FormQuestion q =
        new FormQuestion(
            new ObjectId(),
            fieldType,
            pdfFieldName,
            effectiveDirective,
            label,
            "",
            fieldOptions,
            "",
            isRequired,
            3,
            false,
            new ObjectId(),
            "NONE");
    questions.add(q);

    if (options.has("optionMappings")) {
      handleOptionMappings(options.getJSONArray("optionMappings"), questions);
    }
    if (options.has("conditionalFills")) {
      handleOptionMappings(options.getJSONArray("conditionalFills"), questions);
    }
  }

  private static void handleOptionMappings(JSONArray mappings, List<FormQuestion> questions) {
    for (int i = 0; i < mappings.length(); i++) {
      JSONObject m = mappings.getJSONObject(i);
      if (!m.has("pdfField")) continue;

      String pdfField = m.getString("pdfField");
      String fillValue = m.optString("fillValue", "On");
      String forOption = m.optString("forOption", "");
      String directive = resolveDirective(m);

      String label = pdfField + (forOption.isEmpty() ? "" : " (" + forOption + ")");

      FormQuestion q =
          new FormQuestion(
              new ObjectId(),
              FieldType.CHECKBOX,
              pdfField,
              directive != null ? directive : fillValue,
              label,
              "",
              new ArrayList<>(),
              "",
              false,
              3,
              false,
              new ObjectId(),
              "NONE");
      questions.add(q);
    }
  }

  /**
   * Extracts the property name from a JSON Forms scope string.
   * e.g. "#/properties/firstName" -> "firstName"
   */
  public static String extractPropertyName(String scope) {
    if (scope == null || scope.isEmpty()) return null;
    String prefix = "#/properties/";
    if (scope.startsWith(prefix)) {
      return scope.substring(prefix.length());
    }
    return null;
  }

  /**
   * Resolves the directive from options.directive.
   * If it's a string, return it directly.
   * If it's an array of conditional directives, return the first entry's "use" value
   * as a reasonable default for the generated FormQuestion.
   */
  private static String resolveDirective(JSONObject options) {
    if (!options.has("directive")) return null;

    Object directive = options.get("directive");
    if (directive instanceof String) {
      return (String) directive;
    }
    if (directive instanceof JSONArray) {
      JSONArray arr = (JSONArray) directive;
      if (arr.length() > 0) {
        return arr.getJSONObject(0).optString("use", null);
      }
    }
    return null;
  }

  private static FieldType inferFieldType(JSONObject propSchema, JSONObject options) {
    String widget = options.optString("widget", "");
    if ("phone".equals(widget)) return FieldType.TEXT_FIELD;
    if ("signature".equals(widget)) return FieldType.SIGNATURE;

    String format = propSchema.optString("format", "");
    if ("date".equals(format)) return FieldType.DATE_FIELD;

    String schemaType = propSchema.optString("type", "string");
    switch (schemaType) {
      case "boolean":
        return FieldType.CHECKBOX;
      case "array":
        return FieldType.LISTBOX;
      default:
        break;
    }

    if (propSchema.has("oneOf") || propSchema.has("enum")) {
      return FieldType.RADIO_BUTTON;
    }

    return FieldType.TEXT_FIELD;
  }

  private static List<String> extractOptions(JSONObject propSchema) {
    List<String> options = new ArrayList<>();

    if (propSchema.has("oneOf")) {
      JSONArray oneOf = propSchema.getJSONArray("oneOf");
      for (int i = 0; i < oneOf.length(); i++) {
        JSONObject opt = oneOf.getJSONObject(i);
        String label = opt.optString("title", opt.optString("const", ""));
        options.add(label);
      }
    } else if (propSchema.has("enum")) {
      JSONArray enumArr = propSchema.getJSONArray("enum");
      for (int i = 0; i < enumArr.length(); i++) {
        options.add(enumArr.getString(i));
      }
    }

    return options;
  }
}
