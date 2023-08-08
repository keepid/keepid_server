package Form;

import java.util.Comparator;
import java.util.List;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class FormQuestion implements Comparable<FormQuestion> {
  ObjectId id;
  FieldType type;
  String questionName;
  String questionText;
  String answerText;
  List<String> options;
  String defaultValue;
  boolean required;
  int numLines;
  boolean matched;
  ObjectId conditionalOnField;
  // true for positive, false for negative/none
  String conditionalType;

  public FormQuestion(
      ObjectId id,
      FieldType type,
      String questionName,
      String questionText,
      String answerText,
      List<String> options,
      String defaultValue,
      boolean required,
      int numLines,
      boolean matched,
      ObjectId conditionalOnField,
      String conditionalType) {
    this.id = id;
    this.type = type;
    this.questionName = questionName;
    this.questionText = questionText;
    this.answerText = answerText;
    this.options = options;
    this.defaultValue = defaultValue;
    this.required = required;
    this.numLines = numLines;
    this.matched = matched;
    this.conditionalOnField = conditionalOnField;
    this.conditionalType = conditionalType;
  }

  public ObjectId getId() {
    return id;
  }

  public FieldType getType() {
    return type;
  }

  public void setType(FieldType type) {
    this.type = type;
  }

  public String getQuestionName() {
    return questionName;
  }

  public String getQuestionText() {
    return questionText;
  }

  public void setQuestionText(String questionText) {
    this.questionText = questionText;
  }

  public String getAnswerText() {
    return answerText;
  }

  public void setAnswerText(String answerText) {
    this.answerText = answerText;
  }

  public List<String> getOptions() {
    return options;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public boolean isRequired() {
    return required;
  }

  public int getNumLines() {
    return numLines;
  }

  public boolean isMatched() {
    return matched;
  }

  public void setMatched(boolean matched) {
    this.matched = matched;
  }

  public ObjectId getConditionalOnField() {
    return conditionalOnField;
  }

  public void setConditionalOnField(ObjectId conditionalOnField) {
    this.conditionalOnField = conditionalOnField;
  }

  public String getConditionalType() {
    return conditionalType;
  }

  public void setConditionalType(String conditionalType) {
    this.conditionalType = conditionalType;
  }

  public Comparator<FormQuestion> getComparator() {
    return Comparator.comparing(FormQuestion::getId)
        .thenComparing(FormQuestion::getQuestionText)
        .thenComparing(FormQuestion::getType)
        .thenComparing(FormQuestion::getConditionalOnField)
        .thenComparing(FormQuestion::getNumLines)
        .thenComparing(question -> question.getOptions().stream().reduce("", String::concat));
  }

  public JSONObject serialize() {
    return new JSONObject()
        .put("_id", id)
        .put("fieldType", type.toString())
        .put("name", questionName)
        .put("question", questionText)
        .put("answerText", answerText)
        .put("options", options)
        .put("defaultValue", defaultValue)
        .put("isRequired", this.isRequired())
        .put("numLines", numLines)
        .put("isMatched", matched)
        .put("conditionalOnField", conditionalOnField)
        .put("isConditionalType", conditionalType);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj.getClass() != this.getClass()) {
      return false;
    }

    final FormQuestion other = (FormQuestion) obj;
    return this.compareTo(other) == 0;
  }

  @Override
  public int compareTo(@NotNull FormQuestion o) {
    return getComparator().compare(this, o);
  }
}
