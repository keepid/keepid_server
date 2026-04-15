package Database.Packet;

import static com.mongodb.client.model.Filters.eq;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Packet.Packet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.bson.Document;
import org.bson.types.ObjectId;

public class PacketDaoImpl implements PacketDao {
  private final MongoCollection<Packet> packetCollection;

  public PacketDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    packetCollection = db.getCollection("packet", Packet.class);
  }

  @Override
  public Optional<Packet> get(ObjectId id) {
    return Optional.ofNullable(packetCollection.find(eq("_id", id)).first());
  }

  @Override
  public Optional<Packet> getByApplicationFileId(ObjectId applicationFileId) {
    return Optional.ofNullable(packetCollection.find(eq("applicationFileId", applicationFileId)).first());
  }

  @Override
  public List<Packet> getAll() {
    return packetCollection.find().into(new ArrayList<>());
  }

  @Override
  public int size() {
    return (int) packetCollection.countDocuments();
  }

  @Override
  public void clear() {
    packetCollection.deleteMany(new Document());
  }

  @Override
  public void delete(@NonNull Packet packet) {
    packetCollection.deleteOne(eq("_id", packet.getId()));
  }

  @Override
  public void update(@NonNull Packet packet) {
    packetCollection.replaceOne(eq("_id", packet.getId()), packet);
  }

  @Override
  public void save(@NonNull Packet packet) {
    packetCollection.insertOne(packet);
  }
}
