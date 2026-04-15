package Database.Packet;

import Database.Dao;
import Packet.Packet;
import java.util.Optional;
import org.bson.types.ObjectId;

public interface PacketDao extends Dao<Packet> {
  Optional<Packet> get(ObjectId id);

  Optional<Packet> getByApplicationFileId(ObjectId applicationFileId);
}
