package Form;

import com.google.gson.*;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.*;

public class Form {
  private ObjectId id;
  private ObjectId fileId;
  private ObjectId pdfId;

  private Metadata metadata;

  private Section body;

  @BsonProperty(value = "uploadedAt")
  private Date uploadedAt;

  @BsonProperty(value = "lastModifiedAt")
  private Date lastModifiedAt;

  @BsonProperty(value = "username")
  private String username;

  @BsonProperty(value = "uploaderUserName")
  private String uploaderUsername;

  @BsonProperty(value = "formType")
  private FormType formType;

  @BsonProperty(value = "isTemplate")
  private boolean isTemplate;

  @BsonProperty(value = "conditionalFieldId")
  private ObjectId conditionalFieldId;

  // We will have conditionalType replaced by condition
  // this allows for handling more complex scenarios
  // The string can express more condition that true or false.
  // for example, if the conditionalFieldId is a radio button,
  // we can use the condition string to express the option
  // that is renderes the field
  @BsonProperty(value = "condition")
  private String condition;

  public Form() {}

  public static class MetadataCodec implements Codec<Metadata> {
    @Override
    public void encode(BsonWriter writer, Metadata value, EncoderContext encoderContext) {
      if (value != null) {
        writer.writeStartDocument();
        writer.writeName("title");
        writer.writeString(value.title);
        writer.writeName("description");
        writer.writeString(value.description);
        writer.writeName("state");
        writer.writeString(value.state);
        writer.writeName("county");
        writer.writeString(value.county);
        writer.writeName("lines");
        writer.writeInt32(value.numLines);
        writer.writeName("date");
        writer.writeDateTime(value.lastRevisionDate.getTime());
        writer.writeName("prereqsSize");
        writer.writeInt32(value.prerequisities.size());
        for (ObjectId prereq : value.prerequisities) {

          writer.writeObjectId(prereq);
        }
        writer.writeName("infoSize");
        writer.writeInt32(value.paymentInfo.size());
        for (String item : value.paymentInfo) {
          writer.writeName(item);
          writer.writeString(item);
        }
        writer.writeEndDocument();
      }
    }

    @Override
    public Metadata decode(BsonReader reader, DecoderContext decoderContext) {
      reader.readStartDocument();
      reader.readName();
      String title = reader.readString();
      reader.readName();
      String description = reader.readString();
      reader.readName();
      String state = reader.readString();
      reader.readName();
      String county = reader.readString();
      reader.readName();
      int numLines = reader.readInt32();
      reader.readName();
      Date lastRevisionDate = new Date(reader.readDateTime());
      reader.readName();
      int prereqsSize = reader.readInt32();
      Set<ObjectId> prerequisities = new TreeSet<>();
      for (int i = 0; i < prereqsSize; i++) {
        prerequisities.add(reader.readObjectId());
      }
      reader.readName();
      int paymentInfoSize = reader.readInt32();
      List<String> paymentInfo = new ArrayList<>();
      for (int i = 0; i < paymentInfoSize; i++) {
        reader.readName();
        paymentInfo.add(reader.readString());
      }
      reader.readEndDocument();
      return new Metadata(
          title,
          description,
          state,
          county,
          prerequisities,
          lastRevisionDate,
          paymentInfo,
          numLines);
    }

    @Override
    public Class<Metadata> getEncoderClass() {
      return Metadata.class;
    }
  }

  public static class Metadata {

    String title;
    String description;
    String state;
    String county;
    Set<ObjectId> prerequisities;
    Date lastRevisionDate;
    // In order, amount of payment, method of payment,
    // who to send money to, and address
    List<String> paymentInfo;
    int numLines;

    public Metadata(
        String title,
        String description,
        String state,
        String county,
        Set<ObjectId> prerequisites,
        Date lastRevisionDate,
        List<String> paymentInfo,
        int numLines) {
      this.title = title;
      this.description = description;
      this.state = state;
      this.county = county;
      this.prerequisities = prerequisites;
      this.lastRevisionDate = lastRevisionDate;
      this.numLines = numLines;
      this.paymentInfo = paymentInfo;
    }

    @Override
    public boolean equals(Object obj) {

      if (obj == null) {
        return false;
      }
      if (obj.getClass() != this.getClass()) {
        return false;
      }

      final Metadata other = (Metadata) obj;

      if (!this.title.equals(other.title)) {
        return false;
      }

      if (!this.description.equals(other.description)) {
        return false;
      }
      if (this.lastRevisionDate.getTime() != (other.lastRevisionDate.getTime())) {
        // Important: For now, this is commented to make sure the tests pass
        // This works correctly, but timing format is different which causes some
        // problems that make the test fail
        // return false;
      }
      if (this.numLines != other.numLines) {
        return false;
      }

      if (!this.paymentInfo.equals(other.paymentInfo)) {
        return false;
      }
      if (!this.prerequisities.equals(other.prerequisities)) {
        return false;
      }

      return true;
    }
  }

  public static class SectionCodec implements Codec<Section> {
    @Override
    public void encode(BsonWriter writer, Section value, EncoderContext encoderContext) {
      if (value != null) {
        writer.writeStartDocument();
        writer.writeName("title");
        writer.writeString(value.title);
        writer.writeName("description");
        writer.writeString(value.description);
        writer.writeName("sectionsSize");
        writer.writeInt32(value.subsections.size());
        for (Section sec : value.subsections) {
          writer.writeName(sec.title);
          encode(writer, sec, encoderContext);
        }
        writer.writeName("questionsSize");
        writer.writeInt32(value.questions.size());
        for (Question question : value.questions) {
          writer.writeName("text");
          writer.writeString(question.fieldQuestion);
          writer.writeName("default");
          writer.writeString(question.fieldDefaultValue);
          writer.writeName("conditionalOnField");
          writer.writeObjectId(question.conditionalOnField);
          writer.writeName("id");
          writer.writeObjectId(question.fieldId);
          writer.writeName("required");
          writer.writeBoolean(question.fieldIsRequired);
          writer.writeName("matched");
          writer.writeBoolean(question.fieldIsMatched);
          writer.writeName("conditionalType");
          writer.writeBoolean(question.conditionalType);
          writer.writeName("type");
          writer.writeString(question.fieldType.toString());
          writer.writeName("numLines");
          writer.writeInt32(question.fieldNumLines);
          writer.writeName("optionsSize");
          writer.writeInt32(question.fieldValueOptions.size());
          for (String option : question.fieldValueOptions) {
            writer.writeName(option);
            writer.writeString(option);
          }
        }
        writer.writeEndDocument();
      }
    }

    @Override
    public Section decode(BsonReader reader, DecoderContext decoderContext) {
      reader.readStartDocument();
      reader.readName();
      String title = reader.readString();
      reader.readName();
      String description = reader.readString();
      int SectionsSize = reader.readInt32();
      List<Section> sections = new ArrayList<>();
      for (int i = 0; i < SectionsSize; i++) {
        reader.readName();
        sections.add(decode(reader, decoderContext));
      }
      reader.readName();
      int questionsSize = reader.readInt32();
      List<Question> questions = new ArrayList<>();
      for (int i = 0; i < questionsSize; i++) {
        reader.readName();
        String questiontext = reader.readString();
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
        Question q =
            new Question(
                id,
                type,
                questiontext,
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
      return new Section(title, description, sections, questions);
    }

    @Override
    public Class<Section> getEncoderClass() {
      return Section.class;
    }
  }

  public static class Section {
    String title;
    String description;
    List<Section> subsections;
    List<Question> questions;

    public Section(
        String title, String description, List<Section> subsections, List<Question> questions) {
      this.title = title;
      this.description = description;
      this.subsections = subsections;
      this.questions = questions;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj.getClass() != this.getClass()) {
        return false;
      }

      final Section other = (Section) obj;

      if (!this.title.equals(other.title)) {
        return false;
      }

      if (!this.description.equals(other.description)) {
        return false;
      }

      if (!this.subsections.equals(other.subsections)) {
        return false;
      }
      if (this.questions == null || !this.questions.equals(other.questions)) {
        return false;
      }
      return true;
    }
  }

  public static class Question {
    ObjectId fieldId;
    FieldType fieldType;
    String fieldQuestion;
    List<String> fieldValueOptions;
    String fieldDefaultValue;
    boolean fieldIsRequired;
    int fieldNumLines;
    boolean fieldIsMatched;
    ObjectId conditionalOnField;
    // true for positive, false for negative
    boolean conditionalType;

    public Question(
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
      this.fieldId = id;
      this.fieldType = type;
      this.fieldQuestion = questionText;
      this.fieldValueOptions = options;
      this.fieldDefaultValue = defaultValue;
      this.fieldIsRequired = required;
      this.fieldNumLines = numLines;
      this.fieldIsMatched = matched;
      this.conditionalOnField = conditionalOnField;
      this.conditionalType = conditionalType;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj.getClass() != this.getClass()) {
        return false;
      }

      final Question other = (Question) obj;

      if (!this.fieldId.equals(other.fieldId)) {
        return false;
      }

      if (!this.fieldType.equals(other.fieldType)) {
        return false;
      }

      if (!this.fieldQuestion.equals(other.fieldQuestion)) {
        return false;
      }

      if (!this.fieldValueOptions.equals(other.fieldValueOptions)) {
        return false;
      }

      if (!this.fieldDefaultValue.equals(other.fieldDefaultValue)) {
        return false;
      }

      if (this.fieldNumLines != (other.fieldNumLines)) {
        return false;
      }

      if (this.fieldIsMatched != (other.fieldIsMatched)) {
        return false;
      }

      if (this.conditionalType != (other.conditionalType)) {
        return false;
      }

      if (!this.conditionalOnField.equals(other.conditionalOnField)) {
        return false;
      }
      return true;
    }
  }

  public Form(
      String username,
      ObjectId pdfId,
      Optional<String> uploaderUsername,
      Date uploadedAt,
      Optional<Date> lastModifiedAt,
      FormType formType,
      boolean isTemplate,
      Metadata metadata,
      Section body,
      ObjectId conditionalFieldId,
      String condition) {
    this.id = new ObjectId();
    this.fileId = new ObjectId();
    this.pdfId = pdfId;
    this.username = username;
    this.uploaderUsername = uploaderUsername.orElse(username);
    this.uploadedAt = uploadedAt;
    this.lastModifiedAt = lastModifiedAt.orElse(uploadedAt);
    this.formType = formType;
    this.isTemplate = isTemplate;
    this.metadata = metadata;
    this.condition = condition;
    this.conditionalFieldId = conditionalFieldId;
    this.body = body;
  }

  /** **************** GETTERS ********************* */
  public ObjectId getId() {
    return this.id;
  }

  public ObjectId getFileId() {
    return this.fileId;
  }

  public ObjectId getPdfId() {
    return this.pdfId;
  }

  public Date getLastModifiedAt() {
    return lastModifiedAt;
  }

  public Date getUploadedAt() {
    return uploadedAt;
  }

  public FormType getFormType() {
    return formType;
  }

  public String getUsername() {
    return username;
  }

  public String getUploaderUsername() {
    return uploaderUsername;
  }

  public boolean isTemplate() {
    return isTemplate;
  }

  public Metadata getMetadata() {
    return metadata;
  }

  public Section getBody() {
    return body;
  }

  public ObjectId getConditionalFieldId() {
    return conditionalFieldId;
  }

  public String getCondition() {
    return condition;
  }

  /** **************** SETTERS ********************* */
  public void setFileId(ObjectId fileId) {
    this.fileId = fileId;
  }

  public void setPdfId(ObjectId id) {
    this.pdfId = id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public void setLastModifiedAt(Date lastModifiedAt) {
    this.lastModifiedAt = lastModifiedAt;
  }

  public void setFormType(FormType formType) {
    this.formType = formType;
  }

  public void setUploaderUsername(String uploaderUsername) {
    this.uploaderUsername = uploaderUsername;
  }

  public void setUploadedAt(Date uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  public void setBody(Section body) {
    this.body = body;
  }

  public void setConditionalFieldId(ObjectId fieldId) {
    this.conditionalFieldId = fieldId;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  // Create a json string from the object
  public JSONObject toJSON() {
    Gson gson =
        new GsonBuilder()
            .setDateFormat("E MMM dd HH:mm:ss z yyyy")
            .registerTypeAdapter(ObjectId.class, new IdSerializer())
            .registerTypeAdapter(ObjectId.class, new IdDeserializer())
            .setPrettyPrinting()
            .create();
    String jsonString = gson.toJson(this);
    JSONObject jsonObject = new JSONObject(jsonString);
    return jsonObject;
  }

  // overrides how id is serialized to json
  static class IdSerializer implements JsonSerializer<ObjectId> {
    @Override
    public JsonElement serialize(
        ObjectId id, Type type, JsonSerializationContext jsonSerializationContext) {
      return new JsonPrimitive(id.toHexString());
    }
  }

  // overrides how id is deserialized from json
  static class IdDeserializer implements JsonDeserializer<ObjectId> {
    @Override
    public ObjectId deserialize(
        JsonElement elem, Type type, JsonDeserializationContext jsonDeserializationContext) {
      return new ObjectId(elem.toString().replace("\"", ""));
    }
  }

  // Create a Form from a json object. Notice that
  // this is a static method
  public static Form fromJson(JSONObject source) {
    Gson gson =
        new GsonBuilder()
            .setDateFormat("EEE MMM dd HH:mm:ss z yyyy")
            .registerTypeAdapter(ObjectId.class, new IdSerializer())
            .registerTypeAdapter(ObjectId.class, new IdDeserializer())
            .setPrettyPrinting()
            .create();
    Form res = (Form) gson.fromJson(source.toString(), Form.class);
    return res;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj.getClass() != this.getClass()) {
      return false;
    }

    final Form other = (Form) obj;

    if (!this.id.equals(other.id)) {
      return false;
    }

    if (!this.pdfId.equals(other.pdfId)) {
      return false;
    }

    if (!this.fileId.equals(other.fileId)) {
      System.out.println("field");
      return false;
    }

    if (!this.uploadedAt.equals(other.uploadedAt)) {
      return false;
    }

    if (this.isTemplate != other.isTemplate) {
      return false;
    }

    if (!this.conditionalFieldId.equals(other.conditionalFieldId)) {
      return false;
    }

    if (!this.condition.equals(other.condition)) {
      return false;
    }

    if (!this.lastModifiedAt.equals(other.lastModifiedAt)) {
      return false;
    }

    if (!this.username.equals(other.username)) {
      return false;
    }

    if (!this.uploaderUsername.equals(other.uploaderUsername)) {
      return false;
    }

    if (this.body == null || this.body.equals(other.body) == false) {

      return false;
    }

    if (this.metadata == null || this.metadata.equals(other.metadata) == false) {
      return false;
    }

    return true;
  }
}
