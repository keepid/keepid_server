package FormTest;

import static org.junit.Assert.*;

import Form.FieldType;
import Form.FormQuestion;
import Form.InteractiveFormConfigUtils;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class InteractiveFormConfigUtilsUnitTests {

  @Test
  public void extractPropertyNameFromScope() {
    assertEquals(
        "firstName",
        InteractiveFormConfigUtils.extractPropertyName("#/properties/firstName"));
    assertNull(InteractiveFormConfigUtils.extractPropertyName(""));
    assertNull(InteractiveFormConfigUtils.extractPropertyName(null));
    assertNull(InteractiveFormConfigUtils.extractPropertyName("bogus"));
  }

  @Test
  public void generatesQuestionsFromSimpleSchema() {
    JSONObject jsonSchema =
        new JSONObject()
            .put("type", "object")
            .put(
                "properties",
                new JSONObject()
                    .put("firstName", new JSONObject().put("type", "string"))
                    .put("lastName", new JSONObject().put("type", "string")))
            .put("required", new JSONArray().put("firstName"));

    JSONObject uiSchema =
        new JSONObject()
            .put("type", "Categorization")
            .put(
                "elements",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("type", "Category")
                            .put("label", "Personal Info")
                            .put(
                                "elements",
                                new JSONArray()
                                    .put(
                                        new JSONObject()
                                            .put("type", "Control")
                                            .put("scope", "#/properties/firstName")
                                            .put("label", "First Name")
                                            .put(
                                                "options",
                                                new JSONObject()
                                                    .put("pdfField", "Text16")
                                                    .put("directive", "client.currentName.first")))
                                    .put(
                                        new JSONObject()
                                            .put("type", "Control")
                                            .put("scope", "#/properties/lastName")
                                            .put("label", "Last Name")
                                            .put(
                                                "options",
                                                new JSONObject()
                                                    .put("pdfField", "Text17")
                                                    .put("directive", "client.currentName.last"))))));

    List<FormQuestion> questions =
        InteractiveFormConfigUtils.generateFormQuestions(
            jsonSchema.toString(), uiSchema.toString());

    assertEquals(2, questions.size());

    FormQuestion first = questions.get(0);
    assertEquals("Text16", first.getQuestionName());
    assertEquals("First Name", first.getQuestionText());
    assertEquals("client.currentName.first", first.getDirective());
    assertEquals(FieldType.TEXT_FIELD, first.getType());
    assertTrue(first.isRequired());

    FormQuestion second = questions.get(1);
    assertEquals("Text17", second.getQuestionName());
    assertEquals("Last Name", second.getQuestionText());
    assertEquals("client.currentName.last", second.getDirective());
    assertFalse(second.isRequired());
  }

  @Test
  public void skipsControlsWithoutPdfField() {
    JSONObject jsonSchema =
        new JSONObject()
            .put("type", "object")
            .put(
                "properties",
                new JSONObject()
                    .put("applicantRole", new JSONObject().put("type", "string")));

    JSONObject uiSchema =
        new JSONObject()
            .put("type", "Categorization")
            .put(
                "elements",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("type", "Category")
                            .put("label", "Role")
                            .put(
                                "elements",
                                new JSONArray()
                                    .put(
                                        new JSONObject()
                                            .put("type", "Control")
                                            .put("scope", "#/properties/applicantRole")
                                            .put("label", "Who is applying?")))));

    List<FormQuestion> questions =
        InteractiveFormConfigUtils.generateFormQuestions(
            jsonSchema.toString(), uiSchema.toString());

    assertEquals(0, questions.size());
  }

  @Test
  public void infersDateFieldFromFormat() {
    JSONObject jsonSchema =
        new JSONObject()
            .put("type", "object")
            .put(
                "properties",
                new JSONObject()
                    .put(
                        "birthDate",
                        new JSONObject().put("type", "string").put("format", "date")));

    JSONObject uiSchema =
        new JSONObject()
            .put("type", "Categorization")
            .put(
                "elements",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("type", "Category")
                            .put("label", "Info")
                            .put(
                                "elements",
                                new JSONArray()
                                    .put(
                                        new JSONObject()
                                            .put("type", "Control")
                                            .put("scope", "#/properties/birthDate")
                                            .put("label", "Date of Birth")
                                            .put(
                                                "options",
                                                new JSONObject()
                                                    .put("pdfField", "Text20"))))));

    List<FormQuestion> questions =
        InteractiveFormConfigUtils.generateFormQuestions(
            jsonSchema.toString(), uiSchema.toString());

    assertEquals(1, questions.size());
    assertEquals(FieldType.DATE_FIELD, questions.get(0).getType());
  }

  @Test
  public void infersCheckboxFromBoolean() {
    JSONObject jsonSchema =
        new JSONObject()
            .put("type", "object")
            .put(
                "properties",
                new JSONObject()
                    .put("isHomeless", new JSONObject().put("type", "boolean")));

    JSONObject uiSchema =
        new JSONObject()
            .put("type", "Categorization")
            .put(
                "elements",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("type", "Category")
                            .put("label", "Info")
                            .put(
                                "elements",
                                new JSONArray()
                                    .put(
                                        new JSONObject()
                                            .put("type", "Control")
                                            .put("scope", "#/properties/isHomeless")
                                            .put("label", "Are you homeless?")
                                            .put(
                                                "options",
                                                new JSONObject()
                                                    .put("pdfField", "CheckBox3"))))));

    List<FormQuestion> questions =
        InteractiveFormConfigUtils.generateFormQuestions(
            jsonSchema.toString(), uiSchema.toString());

    assertEquals(1, questions.size());
    assertEquals(FieldType.CHECKBOX, questions.get(0).getType());
  }

  @Test
  public void infersRadioFromOneOf() {
    JSONObject jsonSchema =
        new JSONObject()
            .put("type", "object")
            .put(
                "properties",
                new JSONObject()
                    .put(
                        "gender",
                        new JSONObject()
                            .put("type", "string")
                            .put(
                                "oneOf",
                                new JSONArray()
                                    .put(
                                        new JSONObject()
                                            .put("const", "M")
                                            .put("title", "Male"))
                                    .put(
                                        new JSONObject()
                                            .put("const", "F")
                                            .put("title", "Female")))));

    JSONObject uiSchema =
        new JSONObject()
            .put("type", "Categorization")
            .put(
                "elements",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("type", "Category")
                            .put("label", "Info")
                            .put(
                                "elements",
                                new JSONArray()
                                    .put(
                                        new JSONObject()
                                            .put("type", "Control")
                                            .put("scope", "#/properties/gender")
                                            .put("label", "Gender")
                                            .put(
                                                "options",
                                                new JSONObject()
                                                    .put("pdfField", "RadioButton1"))))));

    List<FormQuestion> questions =
        InteractiveFormConfigUtils.generateFormQuestions(
            jsonSchema.toString(), uiSchema.toString());

    assertEquals(1, questions.size());
    assertEquals(FieldType.RADIO_BUTTON, questions.get(0).getType());
    assertEquals(2, questions.get(0).getOptions().size());
    assertEquals("Male", questions.get(0).getOptions().get(0));
    assertEquals("Female", questions.get(0).getOptions().get(1));
  }

  @Test
  public void handlesConditionalDirectiveArray() {
    JSONObject jsonSchema =
        new JSONObject()
            .put("type", "object")
            .put(
                "properties",
                new JSONObject()
                    .put("firstName", new JSONObject().put("type", "string")));

    JSONArray conditionalDirectives =
        new JSONArray()
            .put(
                new JSONObject()
                    .put(
                        "when",
                        new JSONObject()
                            .put("scope", "#/properties/applicantRole")
                            .put("schema", new JSONObject().put("const", "self")))
                    .put("use", "client.currentName.first"))
            .put(
                new JSONObject()
                    .put(
                        "when",
                        new JSONObject()
                            .put("scope", "#/properties/applicantRole")
                            .put("schema", new JSONObject().put("const", "caseworker")))
                    .put("use", "worker.currentName.first"));

    JSONObject uiSchema =
        new JSONObject()
            .put("type", "Categorization")
            .put(
                "elements",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("type", "Category")
                            .put("label", "Info")
                            .put(
                                "elements",
                                new JSONArray()
                                    .put(
                                        new JSONObject()
                                            .put("type", "Control")
                                            .put("scope", "#/properties/firstName")
                                            .put("label", "First Name")
                                            .put(
                                                "options",
                                                new JSONObject()
                                                    .put("pdfField", "Text16")
                                                    .put(
                                                        "directive",
                                                        conditionalDirectives))))));

    List<FormQuestion> questions =
        InteractiveFormConfigUtils.generateFormQuestions(
            jsonSchema.toString(), uiSchema.toString());

    assertEquals(1, questions.size());
    assertEquals("client.currentName.first", questions.get(0).getDirective());
  }

  @Test
  public void handlesGroupWithMultiplePdfFields() {
    JSONObject jsonSchema =
        new JSONObject()
            .put("type", "object")
            .put(
                "properties",
                new JSONObject()
                    .put("q_1__0", new JSONObject().put("type", "string"))
                    .put("q_1__1", new JSONObject().put("type", "string"))
                    .put("q_1__2", new JSONObject().put("type", "string")));

    JSONObject uiSchema =
        new JSONObject()
            .put("type", "Categorization")
            .put(
                "elements",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("type", "Category")
                            .put("label", "Personal Info")
                            .put(
                                "elements",
                                new JSONArray()
                                    .put(
                                        new JSONObject()
                                            .put("type", "Group")
                                            .put("label", "Full Name")
                                            .put(
                                                "elements",
                                                new JSONArray()
                                                    .put(
                                                        new JSONObject()
                                                            .put("type", "Control")
                                                            .put("scope", "#/properties/q_1__0")
                                                            .put("label", "First")
                                                            .put(
                                                                "options",
                                                                new JSONObject()
                                                                    .put("pdfField", "First_Name")
                                                                    .put(
                                                                        "directive",
                                                                        "client.currentName.first")))
                                                    .put(
                                                        new JSONObject()
                                                            .put("type", "Control")
                                                            .put("scope", "#/properties/q_1__1")
                                                            .put("label", "Middle")
                                                            .put(
                                                                "options",
                                                                new JSONObject()
                                                                    .put("pdfField", "Middle_Name")))
                                                    .put(
                                                        new JSONObject()
                                                            .put("type", "Control")
                                                            .put("scope", "#/properties/q_1__2")
                                                            .put("label", "Last")
                                                            .put(
                                                                "options",
                                                                new JSONObject()
                                                                    .put("pdfField", "Last_Name")
                                                                    .put(
                                                                        "directive",
                                                                        "client.currentName.last"))))))));

    List<FormQuestion> questions =
        InteractiveFormConfigUtils.generateFormQuestions(
            jsonSchema.toString(), uiSchema.toString());

    assertEquals(3, questions.size());
    assertEquals("First_Name", questions.get(0).getQuestionName());
    assertEquals("client.currentName.first", questions.get(0).getDirective());
    assertEquals("Middle_Name", questions.get(1).getQuestionName());
    assertNull(questions.get(1).getDirective());
    assertEquals("Last_Name", questions.get(2).getQuestionName());
    assertEquals("client.currentName.last", questions.get(2).getDirective());
  }

  @Test
  public void handlesConditionalFillsWithAndConditions() {
    JSONObject jsonSchema =
        new JSONObject()
            .put("type", "object")
            .put(
                "properties",
                new JSONObject()
                    .put("q_name", new JSONObject().put("type", "string")));

    JSONArray conditionalFills =
        new JSONArray()
            .put(
                new JSONObject()
                    .put("pdfField", "Birth_First_Name")
                    .put("directive", "client.currentName.first")
                    .put(
                        "fillCondition",
                        new JSONArray()
                            .put(
                                new JSONObject()
                                    .put("scope", "#/properties/q_who")
                                    .put("schema", new JSONObject().put("const", "Myself")))
                            .put(
                                new JSONObject()
                                    .put("scope", "#/properties/q_only")
                                    .put("schema", new JSONObject().put("const", true)))));

    JSONObject uiSchema =
        new JSONObject()
            .put("type", "Categorization")
            .put(
                "elements",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("type", "Category")
                            .put("label", "Name")
                            .put(
                                "elements",
                                new JSONArray()
                                    .put(
                                        new JSONObject()
                                            .put("type", "Control")
                                            .put("scope", "#/properties/q_name")
                                            .put("label", "First Name")
                                            .put(
                                                "options",
                                                new JSONObject()
                                                    .put("pdfField", "Applicant_First")
                                                    .put("directive", "client.currentName.first")
                                                    .put("conditionalFills", conditionalFills))))));

    List<FormQuestion> questions =
        InteractiveFormConfigUtils.generateFormQuestions(
            jsonSchema.toString(), uiSchema.toString());

    assertEquals(2, questions.size());

    FormQuestion primary = questions.get(0);
    assertEquals("Applicant_First", primary.getQuestionName());
    assertEquals("client.currentName.first", primary.getDirective());

    FormQuestion conditional = questions.get(1);
    assertEquals("Birth_First_Name", conditional.getQuestionName());
    assertEquals("client.currentName.first", conditional.getDirective());
  }

  @Test
  public void handlesGroupWithConditionalFillDestinations() {
    JSONObject jsonSchema =
        new JSONObject()
            .put("type", "object")
            .put(
                "properties",
                new JSONObject()
                    .put("q_name", new JSONObject().put("type", "string"))
                    .put("q_name__1", new JSONObject().put("type", "string")));

    JSONArray conditionalFills =
        new JSONArray()
            .put(
                new JSONObject()
                    .put("pdfField", "Birth_First")
                    .put("directive", "client.currentName.first"))
            .put(
                new JSONObject()
                    .put("pdfField", "Birth_Last")
                    .put("directive", "client.currentName.last"));

    JSONObject uiSchema =
        new JSONObject()
            .put("type", "Categorization")
            .put(
                "elements",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("type", "Category")
                            .put("label", "Name")
                            .put(
                                "elements",
                                new JSONArray()
                                    .put(
                                        new JSONObject()
                                            .put("type", "Group")
                                            .put("label", "Full Name")
                                            .put(
                                                "options",
                                                new JSONObject()
                                                    .put("conditionalFills", conditionalFills))
                                            .put(
                                                "elements",
                                                new JSONArray()
                                                    .put(
                                                        new JSONObject()
                                                            .put("type", "Control")
                                                            .put("scope", "#/properties/q_name")
                                                            .put("label", "First")
                                                            .put(
                                                                "options",
                                                                new JSONObject()
                                                                    .put("pdfField", "Applicant_First")
                                                                    .put("directive", "client.currentName.first")))
                                                    .put(
                                                        new JSONObject()
                                                            .put("type", "Control")
                                                            .put("scope", "#/properties/q_name__1")
                                                            .put("label", "Last")
                                                            .put(
                                                                "options",
                                                                new JSONObject()
                                                                    .put("pdfField", "Applicant_Last")
                                                                    .put("directive", "client.currentName.last"))))))));

    List<FormQuestion> questions =
        InteractiveFormConfigUtils.generateFormQuestions(
            jsonSchema.toString(), uiSchema.toString());

    assertEquals(4, questions.size());
    assertEquals("Applicant_First", questions.get(0).getQuestionName());
    assertEquals("Applicant_Last", questions.get(1).getQuestionName());
    assertEquals("Birth_First", questions.get(2).getQuestionName());
    assertEquals("Birth_Last", questions.get(3).getQuestionName());
  }

  @Test
  public void emptySchemaProducesNoQuestions() {
    List<FormQuestion> questions =
        InteractiveFormConfigUtils.generateFormQuestions(
            "{\"type\":\"object\"}", "{\"type\":\"Categorization\",\"elements\":[]}");
    assertEquals(0, questions.size());
  }
}
