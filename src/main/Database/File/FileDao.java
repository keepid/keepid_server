package Database.File;

import Database.Dao;
import File.File;
import File.FileMessage;
import File.FileType;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface FileDao extends Dao<File> {
  Optional<File> get(ObjectId id);

  List<File> getAll(String username);

  List<File> getAll(Bson filter);

  Optional<InputStream> getStream(ObjectId id);

  void save(String uploaderUsername, InputStream fileInputStream, FileType fileType);

  void save(File file);

  FileMessage save(
      String uploaderUsername,
      InputStream fileInputStream,
      FileType fileType,
      Date uploadedAt,
      String organizationName,
      boolean annotated,
      String filename,
      String contentType);

  void delete(ObjectId id);

  Optional<File> get(String uploaderUsername, FileType fileType);
}
