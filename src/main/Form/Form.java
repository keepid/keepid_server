package Form;

import com.google.gson.Gson;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

public class Form implements Comparable<Form> {
  private ObjectId id;
  private ObjectId fileId;

  private FormMetadata metadata;

  private FormSection body;

  @BsonProperty(value = "uploadedAt")
  private LocalDateTime uploadedAt;

  @BsonProperty(value = "lastModifiedAt")
  private LocalDateTime lastModifiedAt;

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
  // that is renders the field
  @BsonProperty(value = "condition")
  private String condition;

  public Form() {}

  @Override
  public int compareTo(@NotNull Form otherForm) {
    return this.getComparator().compare(this, otherForm);
  }

  public Form(
      String username,
      Optional<String> uploaderUsername,
      LocalDateTime uploadedAt,
      Optional<LocalDateTime> lastModifiedAt,
      FormType formType,
      boolean isTemplate,
      FormMetadata metadata,
      FormSection body,
      ObjectId conditionalFieldId,
      String condition) {
    this.id = new ObjectId();
    this.fileId = new ObjectId();
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

  public LocalDateTime getLastModifiedAt() {
    return lastModifiedAt;
  }

  public LocalDateTime getUploadedAt() {
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

  public FormMetadata getMetadata() {
    return metadata;
  }

  public FormSection getBody() {
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

  public void setId(ObjectId id) {
    this.id = id;
  }

  public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
    this.lastModifiedAt = lastModifiedAt;
  }

  public void setFormType(FormType formType) {
    this.formType = formType;
  }

  public void setUploaderUsername(String uploaderUsername) {
    this.uploaderUsername = uploaderUsername;
  }

  public void setUploadedAt(LocalDateTime uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setMetadata(FormMetadata metadata) {
    this.metadata = metadata;
  }

  public void setBody(FormSection body) {
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
    Gson gson = new Gson();
    String jsonString = gson.toJson(this);
    JSONObject jsonObject = new JSONObject(jsonString);
    return jsonObject;
  }

  // Create a Form from a json object. Notice that
  // this is a static method
  public static Form fromJson(JSONObject source) {
    String username = source.getString("username");
    Optional<String> uploaderUsername = Optional.of(source.getString("uploaderUsername"));
    LocalDateTime uploadedAt =
        !source.has("uploadedAt")
            ? LocalDateTime.now()
            : LocalDateTime.parse(
                source.getString("uploadedAt"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    Optional<LocalDateTime> lastModifiedAt =
        Optional.of(
            !source.has("lastModifiedAt")
                ? LocalDateTime.now()
                : LocalDateTime.parse(
                    source.getString("lastModifiedAt"), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    FormType formType = FormType.FORM;
    boolean isTemplate = false;

    JSONObject metadata = source.getJSONObject("metadata");
    FormMetadata formMetadata =
        new FormMetadata(
            metadata.getString("title"),
            metadata.getString("description"),
            metadata.getString("state"),
            metadata.getString("county"),
            Set.of(),
            !metadata.has("lastRevisedAt")
                ? LocalDateTime.now()
                : LocalDateTime.parse(
                    metadata.getString("lastRevisedAt"), DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            List.of(),
            0);

    JSONObject body = source.getJSONObject("body");
    String title = body.getString("title");
    String description = body.getString("description");
    List<FormQuestion> formQuestions = new ArrayList<>();
    JSONArray questions = body.getJSONArray("questions");
    for (int i = 0; i < questions.length(); i++) {
      JSONObject question = questions.getJSONObject(i);
      FormQuestion formQuestion =
          new FormQuestion(
              new ObjectId(),
              FieldType.createFromString(question.getString("type")),
              question.getString("questionName"),
              question.getString("questionText"),
              question.getString("answerText"),
              question.getJSONArray("options").toList().stream()
                  .map(x -> x.toString())
                  .collect(Collectors.toList()),
              question.getString("defaultValue"),
              question.getBoolean("required"),
              question.getInt("numLines"),
              question.getBoolean("matched"),
              new ObjectId(),
              question.getString("conditionalType"));
      formQuestions.add(formQuestion);
    }
    FormSection formBodyFlat = new FormSection(title, description, List.of(), formQuestions);

    Form generatedForm =
        new Form(
            username,
            uploaderUsername,
            uploadedAt,
            lastModifiedAt,
            formType,
            isTemplate,
            formMetadata,
            formBodyFlat,
            new ObjectId(),
            "");
    generatedForm.setFileId(new ObjectId(source.getString("fileId")));
    return generatedForm;
  }

  private Comparator<Form> getComparator() {
    return Comparator.comparing(Form::getId)
        .thenComparing(Form::getFileId)
        .thenComparing(Form::isTemplate)
        .thenComparing(Form::getUsername)
        .thenComparing(Form::getFormType)
        .thenComparing(Form::getMetadata)
        .thenComparing(Form::getCondition)
        .thenComparing(Form::getConditionalFieldId)
        .thenComparing(Form::getBody);
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
    return getComparator().compare(this, other) == 0;
  }
}
