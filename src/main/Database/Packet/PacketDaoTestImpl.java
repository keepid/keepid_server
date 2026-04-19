package Database.Packet;

import Config.DeploymentLevel;
import Packet.Packet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.bson.types.ObjectId;

public class PacketDaoTestImpl implements PacketDao {
  private final Map<ObjectId, Packet> packetMap;

  public PacketDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in memory test database in production or staging");
    }
    packetMap = new LinkedHashMap<>();
  }

  @Override
  public Optional<Packet> get(ObjectId id) {
    return Optional.ofNullable(packetMap.get(id));
  }

  @Override
  public Optional<Packet> getByApplicationFileId(ObjectId applicationFileId) {
    return packetMap.values().stream()
        .filter(packet -> applicationFileId.equals(packet.getApplicationFileId()))
        .findFirst();
  }

  @Override
  public List<Packet> getAll() {
    return new ArrayList<>(packetMap.values());
  }

  @Override
  public int size() {
    return packetMap.size();
  }

  @Override
  public void clear() {
    packetMap.clear();
  }

  @Override
  public void delete(@NonNull Packet packet) {
    packetMap.remove(packet.getId());
  }

  @Override
  public void update(@NonNull Packet packet) {
    packetMap.put(packet.getId(), packet);
  }

  @Override
  public void save(@NonNull Packet packet) {
    packetMap.put(packet.getId(), packet);
  }
}
