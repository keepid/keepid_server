package Form;

import java.time.LocalDateTime;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

public class InteractiveFormConfig {

  @BsonId private ObjectId id;

  @BsonProperty("fileId")
  private ObjectId fileId;

  @BsonProperty("jsonSchema")
  private String jsonSchema;

  @BsonProperty("uiSchema")
  private String uiSchema;

  @BsonProperty("builderState")
  private String builderState;

  @BsonProperty("createdAt")
  private LocalDateTime createdAt;

  @BsonProperty("lastModifiedAt")
  private LocalDateTime lastModifiedAt;

  public InteractiveFormConfig() {}

  public InteractiveFormConfig(ObjectId fileId, String jsonSchema, String uiSchema) {
    this(fileId, jsonSchema, uiSchema, null);
  }

  public InteractiveFormConfig(
      ObjectId fileId, String jsonSchema, String uiSchema, String builderState) {
    this.id = new ObjectId();
    this.fileId = fileId;
    this.jsonSchema = jsonSchema;
    this.uiSchema = uiSchema;
    this.builderState = builderState;
    this.createdAt = LocalDateTime.now();
    this.lastModifiedAt = LocalDateTime.now();
  }

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public ObjectId getFileId() {
    return fileId;
  }

  public void setFileId(ObjectId fileId) {
    this.fileId = fileId;
  }

  public String getJsonSchema() {
    return jsonSchema;
  }

  public void setJsonSchema(String jsonSchema) {
    this.jsonSchema = jsonSchema;
  }

  public String getUiSchema() {
    return uiSchema;
  }

  public void setUiSchema(String uiSchema) {
    this.uiSchema = uiSchema;
  }

  public String getBuilderState() {
    return builderState;
  }

  public void setBuilderState(String builderState) {
    this.builderState = builderState;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getLastModifiedAt() {
    return lastModifiedAt;
  }

  public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
    this.lastModifiedAt = lastModifiedAt;
  }
}
