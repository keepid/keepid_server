package Database.File;

import Config.DeploymentLevel;
import File.File;
import File.FileMessage;
import File.FileType;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.util.*;

public class FileDaoTestImpl implements FileDao {
  Map<String, List<File>> fileMap;
  Map<ObjectId, File> objectIdFileMap;

  public FileDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in memory test database in production or staging");
    }

    fileMap = new LinkedHashMap<>();
    objectIdFileMap = new LinkedHashMap<>();
  }

  @Override
  public void save(File file) {
    List<File> userForms = fileMap.getOrDefault(file.getUsername(), new ArrayList<>());
    userForms.add(file);
    fileMap.put(file.getUsername(), userForms);
    objectIdFileMap.put(file.getId(), file);
  }

  @Override
  public FileMessage save(
      String uploaderUsername,
      InputStream fileInputStream,
      FileType fileType,
      Date uploadedAt,
      String organizationName,
      boolean annotated,
      String filename,
      String contentType) {
    return null;
  }

  @Override
  public Optional<File> get(ObjectId id) {
    return Optional.ofNullable(objectIdFileMap.get(id));
  }

  @Override
  public List<File> getAll(String username) {
    return fileMap.get(username);
  }

  @Override
  public List<File> getAll(Bson filter) {
    return null;
  }

  @Override
  public Optional<InputStream> getStream(ObjectId id) {
    return Optional.ofNullable(objectIdFileMap.get(id).getFileStream());
  }

  @Override
  public void save(String uploaderUsername, InputStream fileInputStream, FileType fileType) {
    //    File file = new File(uploaderUsername, new DateTime(new Date()), fileInputStream,
    // fileType);
    //    fileMap.put(uploaderUsername, file);
    //    objectIdFileMap.put(file.getId(), file);
  }

  @Override
  public void delete(ObjectId id) {
    File file = objectIdFileMap.get(id);
    objectIdFileMap.remove(id);
    String username = file.getUsername();

    List<File> userFiles = fileMap.get(username);
    File existingFile = null;
    for (File f : userFiles) {
      if (f.getId().equals(id)) {
        existingFile = f;
        break;
      }
    }
    if (existingFile == null) {
      return;
    }
    userFiles.remove(existingFile);
    fileMap.put(file.getUsername(), userFiles);
  }

  @Override
  public Optional<File> get(String uploaderUsername, FileType fileType) {
    return Optional.empty();
  }

  @Override
  public List<File> getAll() {
    return new ArrayList<>(objectIdFileMap.values());
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
  public void update(File newFile) {}
}
