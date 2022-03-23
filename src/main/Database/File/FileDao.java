package Database.File;

import Database.Dao;
import File.File;
import File.FileType;
import com.google.api.client.util.DateTime;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.util.Optional;

public interface FileDao extends Dao<File> {
  Optional<File> get(ObjectId id);

  Optional<File> get(String username);

  Optional<File> getByFileId(ObjectId id);

  Optional<InputStream> getStream(ObjectId id);

  Optional<InputStream> getStream(String username);

  void save(String uploaderUsername, InputStream fileInputStream, FileType fileType);

  void save(
      String uploaderUsername, InputStream fileInputStream, FileType fileType, DateTime uploadedAt);

  void delete(String username);

  void delete(ObjectId id);
}
