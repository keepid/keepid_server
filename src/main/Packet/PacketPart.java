package Packet;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

public class PacketPart {
  @Getter @Setter private ObjectId fileId;
  @Getter @Setter private String partType;
  @Getter @Setter private int order;
  @Getter @Setter private boolean enabled;

  public PacketPart() {}

  public PacketPart(ObjectId fileId, String partType, int order, boolean enabled) {
    this.fileId = fileId;
    this.partType = partType;
    this.order = order;
    this.enabled = enabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PacketPart)) return false;
    PacketPart packetPart = (PacketPart) o;
    return order == packetPart.order
        && enabled == packetPart.enabled
        && Objects.equals(fileId, packetPart.fileId)
        && Objects.equals(partType, packetPart.partType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, partType, order, enabled);
  }
}
