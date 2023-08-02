package Form;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

public class FormSection implements Comparable<FormSection> {
  String title;
  String description;
  List<FormSection> subsections;
  List<FormQuestion> questions;

  public FormSection(
      String title,
      String description,
      List<FormSection> subsections,
      List<FormQuestion> questions) {
    this.title = title;
    this.description = description;
    this.subsections = subsections;
    this.questions = questions;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public List<FormSection> getSubsections() {
    return subsections;
  }

  public List<FormQuestion> getQuestions() {
    return questions;
  }

  public Comparator<FormSection> getComparator() {
    return Comparator.comparing(FormSection::getTitle)
        .thenComparing(FormSection::getDescription)
        .thenComparing(
            section ->
                section.getSubsections().stream()
                    .map(subsection -> subsection.serialize().toString())
                    .reduce("", String::concat))
        .thenComparing(
            section ->
                section.getQuestions().stream()
                    .map(formQuestion -> formQuestion.serialize().toString())
                    .reduce("", String::concat));
  }

  public JSONObject serialize() {
    JSONObject titleAndDescription =
        new JSONObject().put("title", title).put("description", description);
    List<JSONObject> subsectionsSerialized =
        subsections.stream()
            .sorted()
            .map(subsection -> subsection.serialize())
            .collect(Collectors.toList());
    List<JSONObject> questionsSerialized =
        questions.stream()
            .sorted()
            .map(questions -> questions.serialize())
            .collect(Collectors.toList());
    titleAndDescription.put("subsections", subsectionsSerialized);
    titleAndDescription.put("questions", questionsSerialized);
    return titleAndDescription;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj.getClass() != this.getClass()) {
      return false;
    }

    final FormSection other = (FormSection) obj;
    return this.compareTo(other) == 0;
  }

  @Override
  public int compareTo(@NotNull FormSection section) {
    return getComparator().compare(this, section);
  }

  public static class SectionCodec implements Codec<FormSection> {
    @Override
    public void encode(BsonWriter writer, FormSection value, EncoderContext encoderContext) {
      if (value != null) {
        writer.writeStartDocument();
        writer.writeName("title");
        writer.writeString(value.title);
        writer.writeName("description");
        writer.writeString(value.description);
        writer.writeName("sectionsSize");
        writer.writeInt32(value.subsections.size());
        value.getSubsections().stream()
            .sorted()
            .forEach(
                subsection -> {
                  writer.writeName(subsection.title);
                  encode(writer, subsection, encoderContext);
                });
        writer.writeName("questionsSize");
        writer.writeInt32(value.questions.size());
        value.getQuestions().stream()
            .sorted()
            .forEach(
                question -> {
                  writer.writeName("questionName");
                  writer.writeString(question.questionName);
                  writer.writeName("text");
                  writer.writeString(question.questionText);
                  writer.writeName("answerText");
                  writer.writeString(question.answerText);
                  writer.writeName("answerBoolean");
                  writer.writeBoolean(question.answerBoolean);
                  writer.writeName("answerArray");
                  writer.writeString(question.answerArray.toString());
                  writer.writeName("default");
                  writer.writeString(question.defaultValue);
                  writer.writeName("conditionalOnField");
                  writer.writeObjectId(question.conditionalOnField);
                  writer.writeName("id");
                  writer.writeObjectId(question.id);
                  writer.writeName("required");
                  writer.writeBoolean(question.required);
                  writer.writeName("matched");
                  writer.writeBoolean(question.matched);
                  writer.writeName("conditionalType");
                  writer.writeBoolean(question.conditionalType);
                  writer.writeName("type");
                  writer.writeString(question.type.toString());
                  writer.writeName("numLines");
                  writer.writeInt32(question.numLines);
                  writer.writeName("optionsSize");
                  writer.writeInt32(question.options.size());
                  question.options.stream()
                      .sorted()
                      .forEach(
                          option -> {
                            writer.writeName(option);
                            writer.writeString(option);
                          });
                });
        writer.writeEndDocument();
      }
    }

    @Override
    public FormSection decode(BsonReader reader, DecoderContext decoderContext) {
      reader.readStartDocument();
      reader.readName();
      String title = reader.readString();
      reader.readName();
      String description = reader.readString();
      int SectionsSize = reader.readInt32();
      List<FormSection> sections = new ArrayList<>();
      for (int i = 0; i < SectionsSize; i++) {
        reader.readName();
        sections.add(decode(reader, decoderContext));
      }
      reader.readName();
      int questionsSize = reader.readInt32();
      List<FormQuestion> questions = new ArrayList<>();
      for (int i = 0; i < questionsSize; i++) {
        reader.readName();
        String questionName = reader.readString();
        reader.readName();
        String questionText = reader.readString();
        reader.readName();
        String answerText = reader.readString();
        reader.readName();
        boolean answerBoolean = reader.readBoolean();
        reader.readName();
        JSONArray answerArray = new JSONArray(reader.readString());
        reader.readName();
        String defaultValue = reader.readString();
        reader.readName();
        ObjectId conditionalOnField = reader.readObjectId();
        reader.readName();
        ObjectId id = reader.readObjectId();
        reader.readName();
        boolean required = reader.readBoolean();
        reader.readName();
        boolean matched = reader.readBoolean();
        reader.readName();
        boolean conditionalType = reader.readBoolean();
        reader.readName();
        String enumType = reader.readString();
        FieldType type;
        switch (enumType) {
          case "textField":
            type = FieldType.TEXT_FIELD;
            break;
          case "checkBox":
            type = FieldType.CHECKBOX;
            break;
          case "multipleChoice":
            type = FieldType.MULTIPLE_CHOICE;
            break;
          case "signature":
            type = FieldType.SIGNATURE;
            break;
          default:
            type = FieldType.TEXT_FIELD;
            break;
        }
        reader.readName();
        int numLines = reader.readInt32();
        reader.readName();
        int optionsSize = reader.readInt32();
        List<String> options = new ArrayList<>();
        for (int j = 0; j < optionsSize; j++) {
          reader.readName();
          options.add(reader.readString());
        }
        FormQuestion q =
            new FormQuestion(
                id,
                type,
                questionName,
                questionText,
                answerText,
                answerBoolean,
                answerArray,
                options,
                defaultValue,
                required,
                numLines,
                matched,
                conditionalOnField,
                conditionalType);
        questions.add(q);
      }
      reader.readEndDocument();
      return new FormSection(title, description, sections, questions);
    }

    @Override
    public Class<FormSection> getEncoderClass() {
      return FormSection.class;
    }
  }
}
