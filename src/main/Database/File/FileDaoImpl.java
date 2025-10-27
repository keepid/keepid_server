package Database.File;

import static com.mongodb.client.model.Filters.*;

import Config.DeploymentLevel;
import Config.MongoConfig;
import File.File;
import File.FileMessage;
import File.FileType;
import File.IdCategoryType;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

@Slf4j
public class FileDaoImpl implements FileDao {
  private MongoCollection<File> fileCollection;
  private GridFSBucket fileBucket;
  public static final int CHUNK_SIZE_BYTES = 100000;

  public FileDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    fileCollection = db.getCollection("file", File.class);
    fileBucket = GridFSBuckets.create(db, "file_fs");
  }

  @Override
  public List<File> getAll() {
    return fileCollection.find().into(new ArrayList<File>());
  }

  @Override
  public int size() {
    return (int) fileCollection.countDocuments();
  }

  @Override
  public void clear() {
    if(fileCollection != null) {
      fileCollection.drop();
    }
    if (fileBucket != null) {
      fileBucket.drop();
    }
  }

  @Override
  public void delete(@NonNull File file) {
    File fileToDelete = fileCollection.find(eq("_id", file.getId())).first();
    if (fileToDelete != null) {
      ObjectId fileId = fileToDelete.getFileId(); // id in gridFS bucket for actual file
      fileBucket.delete(fileId);
      fileCollection.deleteOne(eq("_id", file.getId()));
    }
  }

  @Override
  public void update(@NonNull File file) {}

  @Override
  public void save(@NonNull File file) {
    log.info("Attempting to save file!");

    GridFSUploadOptions options = new GridFSUploadOptions().chunkSizeBytes(CHUNK_SIZE_BYTES);
    ObjectId fileUploadId =
        fileBucket.uploadFromStream(file.getFilename(), file.getFileStream(), options);
    file.setFileId(fileUploadId);

    fileCollection.insertOne(file);
  }

  @Override
  public Optional<File> get(ObjectId id) {
    return Optional.ofNullable(fileCollection.find(eq("_id", id)).first());
  }

  @Override
  public List<File> getAll(String username) {
    return fileCollection.find(eq("username", username)).into(new ArrayList<File>());
  }

  @Override
  public List<File> getAll(Bson filter) {
    return fileCollection.find(filter).into(new ArrayList<File>());
  }

  @Override
  public List<File> getWeeklyUploadedIds() {
    LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
    Date oneWeekAgoDate = Date.from(oneWeekAgo.atZone(ZoneId.systemDefault()).toInstant());

    return fileCollection
        .find(and(eq("fileType", "IDENTIFICATION_PDF"), gte("uploadedAt", oneWeekAgoDate)))
        .into(new ArrayList<>());
  }

  @Override
  public Optional<InputStream> getStream(ObjectId id) {
    File file = fileCollection.find(eq("_id", id)).first();
    if (file != null) {
      GridFSFile grid_out = fileBucket.find(eq("_id", file.getFileId())).first();
      if (grid_out == null) {
        return Optional.empty();
      }
      return Optional.of(fileBucket.openDownloadStream(grid_out.getObjectId()));
    }
    return Optional.empty();
  }

  @Override
  public void delete(ObjectId id) {
    File file = fileCollection.find(eq("_id", id)).first();
    if (file != null) {
      ObjectId fileId = file.getFileId(); // id in gridFS bucket for actual file
      fileBucket.delete(fileId);
      fileCollection.deleteOne(eq("_id", id));
    }
  }

  @Override
  public FileMessage save(
      String uploaderUsername,
      InputStream fileInputStream,
      FileType fileType,
      IdCategoryType idCategory,
      Date uploadedAt,
      String organizationName,
      boolean annotated,
      String filename,
      String contentType) {
    File file =
        new File(
            uploaderUsername,
            uploadedAt,
            fileInputStream,
            fileType,
            idCategory,
            filename,
            organizationName,
            annotated,
            contentType);
    File foundFile =
        fileCollection
            .find(
                and(
                    Filters.eq("filename", filename),
                    // Filters.eq("fileType", fileType),
                    Filters.eq("username", uploaderUsername)))
            .first();
    if (foundFile != null) {
      FileMessage message = FileMessage.FILE_EXISTS;
      message.setFileId(foundFile.getFileId().toString());
      return message;
    }

    GridFSUploadOptions options = new GridFSUploadOptions().chunkSizeBytes(CHUNK_SIZE_BYTES);
    ObjectId fileUploadId = fileBucket.uploadFromStream(filename, fileInputStream, options);
    file.setFileId(fileUploadId);

    fileCollection.insertOne(file);

    FileMessage message = FileMessage.SUCCESS;
    message.setFileId(file.getId().toString());
    return message;
  }

  @Override
  public void save(String uploaderUsername, InputStream fileInputStream, FileType fileType) {}

  @Override
  public Optional<File> get(String uploaderUsername, FileType fileType) {
    File foundFile =
        fileCollection
            .find(and(eq("fileType", fileType.toString()), eq("username", uploaderUsername)))
            .first();

    return Optional.ofNullable(foundFile);
  }
}
