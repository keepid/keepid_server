package Database.File;

import Config.DeploymentLevel;
import Config.MongoConfig;
import File.File;
import File.FileType;
import com.google.api.client.util.DateTime;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class FileDaoImpl implements FileDao {
  private MongoCollection<File> fileCollection;

  public FileDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    fileCollection = db.getCollection("file", File.class);
  }

  @Override
  public List<File> getAll() {
    return null;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public void clear() {}

  @Override
  public void delete(File file) {}

  @Override
  public void update(File file) {}

  @Override
  public void save(File file) {}

  @Override
  public Optional<File> get(ObjectId id) {
    return Optional.empty();
  }

  @Override
  public Optional<File> get(String username) {
    return Optional.empty();
  }

  @Override
  public Optional<File> getByFileId(ObjectId id) {
    return Optional.empty();
  }

  @Override
  public Optional<InputStream> getStream(ObjectId id) {
    return Optional.empty();
  }

  @Override
  public Optional<InputStream> getStream(String username) {
    return Optional.empty();
  }

  @Override
  public void delete(String username) {}

  @Override
  public void delete(ObjectId id) {}

  @Override
  public void save(
      String uploaderUsername,
      InputStream fileInputStream,
      FileType fileType,
      DateTime uploadedAt) {}

  @Override
  public void save(String uploaderUsername, InputStream fileInputStream, FileType fileType) {}
}
