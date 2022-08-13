package Form;

import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class FormQuestion implements Comparable<FormQuestion> {
  ObjectId id;
  FieldType type;
  String questionText;
  List<String> options;
  String defaultValue;
  boolean required;
  int numLines;
  boolean matched;
  ObjectId conditionalOnField;
  // true for positive, false for negative
  boolean conditionalType;

  public FormQuestion(
      ObjectId id,
      FieldType type,
      String questionText,
      List<String> options,
      String defaultValue,
      boolean required,
      int numLines,
      boolean matched,
      ObjectId conditionalOnField,
      boolean conditionalType) {
    this.id = id;
    this.type = type;
    this.questionText = questionText;
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

  public String getQuestionText() {
    return questionText;
  }

  public List<String> getOptions() {
    return options;
  }

  public String getDefaultValue() {
    return defaultValue;
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

  public ObjectId getConditionalOnField() {
    return conditionalOnField;
  }

  public boolean isConditionalType() {
    return conditionalType;
  }

  public Comparator<FormQuestion> getComparator() {
    return Comparator.comparing(FormQuestion::getId)
        .thenComparing(FormQuestion::getQuestionText)
        .thenComparing(FormQuestion::getType)
        .thenComparing(FormQuestion::getConditionalOnField)
        .thenComparing(FormQuestion::getNumLines)
        .thenComparing(question -> question.getOptions().stream()
            .flatMap(option -> Stream.of(option.hashCode()))
            .reduce(Integer::sum)
            .orElse(0));
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
