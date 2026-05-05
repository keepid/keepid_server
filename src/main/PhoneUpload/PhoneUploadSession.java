package PhoneUpload;

import File.IdCategoryType;
import java.util.Date;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.json.JSONObject;

public class PhoneUploadSession {
  private ObjectId id;

  @BsonProperty(value = "tokenHash")
  private String tokenHash;

  @BsonProperty(value = "actorUsername")
  private String actorUsername;

  @BsonProperty(value = "targetClientUsername")
  private String targetClientUsername;

  @BsonProperty(value = "targetClientDisplayName")
  private String targetClientDisplayName;

  @BsonProperty(value = "organizationName")
  private String organizationName;

  @BsonProperty(value = "organizationId")
  private ObjectId organizationId;

  @BsonProperty(value = "idCategory")
  private String idCategory;

  @BsonProperty(value = "customIdCategory")
  private String customIdCategory;

  @BsonProperty(value = "createdAt")
  private Date createdAt;

  @BsonProperty(value = "expiresAt")
  private Date expiresAt;

  @BsonProperty(value = "closedAt")
  private Date closedAt;

  public PhoneUploadSession() {}

  public ObjectId getId() {
    return id;
  }

  public PhoneUploadSession setId(ObjectId id) {
    this.id = id;
    return this;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public PhoneUploadSession setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
    return this;
  }

  public String getActorUsername() {
    return actorUsername;
  }

  public PhoneUploadSession setActorUsername(String actorUsername) {
    this.actorUsername = actorUsername;
    return this;
  }

  public String getTargetClientUsername() {
    return targetClientUsername;
  }

  public PhoneUploadSession setTargetClientUsername(String targetClientUsername) {
    this.targetClientUsername = targetClientUsername;
    return this;
  }

  public String getTargetClientDisplayName() {
    return targetClientDisplayName;
  }

  public PhoneUploadSession setTargetClientDisplayName(String targetClientDisplayName) {
    this.targetClientDisplayName = targetClientDisplayName;
    return this;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public PhoneUploadSession setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
    return this;
  }

  public ObjectId getOrganizationId() {
    return organizationId;
  }

  public PhoneUploadSession setOrganizationId(ObjectId organizationId) {
    this.organizationId = organizationId;
    return this;
  }

  public String getIdCategory() {
    return idCategory;
  }

  public PhoneUploadSession setIdCategory(String idCategory) {
    this.idCategory = idCategory;
    return this;
  }

  public String getCustomIdCategory() {
    return customIdCategory;
  }

  public PhoneUploadSession setCustomIdCategory(String customIdCategory) {
    this.customIdCategory = customIdCategory;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public PhoneUploadSession setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getExpiresAt() {
    return expiresAt;
  }

  public PhoneUploadSession setExpiresAt(Date expiresAt) {
    this.expiresAt = expiresAt;
    return this;
  }

  public Date getClosedAt() {
    return closedAt;
  }

  public PhoneUploadSession setClosedAt(Date closedAt) {
    this.closedAt = closedAt;
    return this;
  }

  public boolean isExpired(Date now) {
    return expiresAt == null || !expiresAt.after(now);
  }

  public boolean isClosed() {
    return closedAt != null;
  }

  public IdCategoryType getResolvedIdCategory() {
    return IdCategoryType.createFromString(idCategory);
  }

  public JSONObject toClientContextJson() {
    JSONObject json = new JSONObject();
    json.put("targetUser", targetClientUsername);
    json.put("targetClientDisplayName", targetClientDisplayName);
    json.put("idCategory", idCategory);
    if (customIdCategory != null && !customIdCategory.isBlank()) {
      json.put("customIdCategory", customIdCategory);
    } else {
      json.put("customIdCategory", JSONObject.NULL);
    }
    json.put("expiresAt", expiresAt != null ? expiresAt.getTime() : JSONObject.NULL);
    return json;
  }
}
