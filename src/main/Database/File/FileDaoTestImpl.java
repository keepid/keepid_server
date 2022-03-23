package Database.File;

import Config.DeploymentLevel;
import File.File;
import File.FileType;
import com.google.api.client.util.DateTime;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.util.*;

public class FileDaoTestImpl implements FileDao {
  Map<String, File> fileMap;
  Map<ObjectId, File> objectIdFileMap;

  public FileDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in memory test database in production or staging");
    }

    fileMap = new LinkedHashMap<>();
  }

  @Override
  public void save(File file) {}

  @Override
  public Optional<File> get(ObjectId id) {
    return Optional.ofNullable(objectIdFileMap.get(id));
  }

  @Override
  public Optional<File> get(String username) {
    return Optional.ofNullable(fileMap.get(username));
  }

  @Override
  public Optional<InputStream> getStream(ObjectId id) {
    return Optional.ofNullable(objectIdFileMap.get(id).getFileStream());
  }

  @Override
  public Optional<InputStream> getStream(String username) {
    return Optional.ofNullable(fileMap.get(username).getFileStream());
  }

  @Override
  public Optional<File> getByFileId(ObjectId fileId) {
    return fileMap.values().stream().filter(x -> x.getFileId() == fileId).findFirst();
  }

  @Override
  public void save(String uploaderUsername, InputStream fileInputStream, FileType fileType) {
    File file = new File(uploaderUsername, new DateTime(new Date()), fileInputStream, fileType);
    fileMap.put(uploaderUsername, file);
    objectIdFileMap.put(file.getId(), file);
  }

  @Override
  public void save(
      String uploaderUsername,
      InputStream fileInputStream,
      FileType fileType,
      DateTime uploadedAt) {
    File file = new File(uploaderUsername, uploadedAt, fileInputStream, fileType);
    fileMap.put(uploaderUsername, file);
    objectIdFileMap.put(file.getId(), file);
  }

  @Override
  public void delete(String username) {
    File file = fileMap.remove(username);
    objectIdFileMap.remove(file.getId());
  }

  @Override
  public void delete(ObjectId id) {
    File file = objectIdFileMap.remove(id);
    fileMap.remove(file.getUsername());
  }

  @Override
  public List<File> getAll() {
    return new ArrayList<>(fileMap.values());
  }

  @Override
  public int size() {
    return fileMap.size();
  }

  @Override
  public void clear() {
    fileMap.clear();
    objectIdFileMap.clear();
  }

  @Override
  public void delete(File file) {
    fileMap.remove(file.getUsername());
    objectIdFileMap.remove(file.getId());
  }

  @Override
  public void update(File newFile) {
    fileMap.put(newFile.getUsername(), newFile);
    objectIdFileMap.put(newFile.getId(), newFile);
  }
}
