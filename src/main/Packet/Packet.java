package Packet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

public class Packet {
  @Getter @Setter private ObjectId id;
  @Getter @Setter private ObjectId organizationId;
  @Getter @Setter private ObjectId applicationFileId;
  @Getter @Setter private List<PacketPart> parts;
  @Getter @Setter private String createdBy;
  @Getter @Setter private Date createdAt;
  @Getter @Setter private Date updatedAt;

  public Packet() {}

  public Packet(ObjectId organizationId, ObjectId applicationFileId, String createdBy) {
    this.id = new ObjectId();
    this.organizationId = organizationId;
    this.applicationFileId = applicationFileId;
    this.parts = new ArrayList<>();
    this.createdBy = createdBy;
    Date now = new Date();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public JSONObject toJson() {
    JSONArray partsJson = new JSONArray();
    if (parts != null) {
      for (PacketPart part : parts) {
        partsJson.put(
            new JSONObject()
                .put("fileId", part.getFileId().toString())
                .put("partType", part.getPartType())
                .put("order", part.getOrder())
                .put("enabled", part.isEnabled()));
      }
    }

    JSONObject res =
        new JSONObject()
            .put("id", id.toString())
            .put("applicationFileId", applicationFileId.toString())
            .put("parts", partsJson)
            .put("createdBy", createdBy)
            .put("createdAt", createdAt)
            .put("updatedAt", updatedAt);
    if (organizationId != null) {
      res.put("organizationId", organizationId.toString());
    }
    return res;
  }
}
