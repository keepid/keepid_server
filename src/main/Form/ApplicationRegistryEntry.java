package Form;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

public class ApplicationRegistryEntry {

  @BsonId private ObjectId id;

  @BsonProperty("idCategoryType")
  private String idCategoryType;

  @BsonProperty("state")
  private String state;

  @BsonProperty("applicationSubtype")
  private String applicationSubtype;

  @BsonProperty("pidlSubtype")
  private String pidlSubtype;

  @BsonProperty("amount")
  private String amount;

  @BsonProperty("numWeeks")
  private int numWeeks;

  @BsonProperty("orgMappings")
  private List<OrgMapping> orgMappings;

  @BsonProperty("createdAt")
  private LocalDateTime createdAt;

  @BsonProperty("lastModifiedAt")
  private LocalDateTime lastModifiedAt;

  public ApplicationRegistryEntry() {}

  public ApplicationRegistryEntry(
      String idCategoryType,
      String state,
      String applicationSubtype,
      String pidlSubtype,
      BigDecimal amount,
      int numWeeks,
      List<OrgMapping> orgMappings) {
    this.id = new ObjectId();
    this.idCategoryType = idCategoryType;
    this.state = state;
    this.applicationSubtype = applicationSubtype;
    this.pidlSubtype = pidlSubtype;
    this.amount = amount.toPlainString();
    this.numWeeks = numWeeks;
    this.orgMappings = orgMappings != null ? orgMappings : new ArrayList<>();
    this.createdAt = LocalDateTime.now();
    this.lastModifiedAt = LocalDateTime.now();
  }

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public String getIdCategoryType() {
    return idCategoryType;
  }

  public void setIdCategoryType(String idCategoryType) {
    this.idCategoryType = idCategoryType;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getApplicationSubtype() {
    return applicationSubtype;
  }

  public void setApplicationSubtype(String applicationSubtype) {
    this.applicationSubtype = applicationSubtype;
  }

  public String getPidlSubtype() {
    return pidlSubtype;
  }

  public void setPidlSubtype(String pidlSubtype) {
    this.pidlSubtype = pidlSubtype;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public int getNumWeeks() {
    return numWeeks;
  }

  public void setNumWeeks(int numWeeks) {
    this.numWeeks = numWeeks;
  }

  public List<OrgMapping> getOrgMappings() {
    return orgMappings;
  }

  public void setOrgMappings(List<OrgMapping> orgMappings) {
    this.orgMappings = orgMappings;
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

  /**
   * Returns the file ID for a given organization, or null if no mapping exists. This is the
   * file._id in the "file" collection, NOT form._id.
   */
  public ObjectId getFileIdForOrg(String orgName) {
    if (orgMappings == null) return null;
    return orgMappings.stream()
        .filter(m -> m.getOrgName().equals(orgName))
        .map(OrgMapping::getFileId)
        .findFirst()
        .orElse(null);
  }

  public static class OrgMapping {
    @BsonProperty("orgName")
    private String orgName;

    @BsonProperty("fileId")
    private ObjectId fileId;

    public OrgMapping() {}

    public OrgMapping(String orgName, ObjectId fileId) {
      this.orgName = orgName;
      this.fileId = fileId;
    }

    public String getOrgName() {
      return orgName;
    }

    public void setOrgName(String orgName) {
      this.orgName = orgName;
    }

    public ObjectId getFileId() {
      return fileId;
    }

    public void setFileId(ObjectId fileId) {
      this.fileId = fileId;
    }
  }
}
