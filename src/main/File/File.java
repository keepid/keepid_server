package File;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

@Slf4j
public class File {
  @Getter @Setter private ObjectId id; // id of file in collection (metadata)
  @Getter @Setter private ObjectId fileId; // id of file in bucket (actual file bytes)
  @Getter @Setter @BsonIgnore private InputStream fileStream;
  @Getter @Setter private String filename;
  @Getter @Setter private FileType fileType;
  @Getter @Setter private Date uploadedAt;
  @Getter @Setter private String username;
  @Getter @Setter private String organizationName;
  @Getter @Setter private LocalDateTime lastMailedAt;

  @Getter
  @Setter
  @BsonProperty(value = "annotated")
  private boolean isAnnotated;

  @Getter
  @Setter
  private IdCategoryType idCategory;

  @Getter @Setter private String contentType;

  public File() {}

  public File(
      ObjectId id,
      String filename,
      FileType fileType,
      Date uploadedAt,
      String username,
      String organizationName,
      String contentType) {
    this.id = id;
    this.filename = filename;
    this.fileType = fileType;
    this.uploadedAt = uploadedAt;
    this.username = username;
    this.organizationName = organizationName;
    this.contentType = contentType;
  }

  public File(
      String username,
      Date uploadedAt,
      InputStream fileStream,
      FileType fileType,
      IdCategoryType idCategory,
      String fileName,
      String organizationName,
      boolean isAnnotated,
      String contentType) {
    this.id = new ObjectId();
    this.username = username;
    this.uploadedAt = uploadedAt;
    this.fileStream = fileStream;
    this.fileType = fileType;
    this.idCategory = idCategory;
    this.filename = fileName;
    this.organizationName = organizationName;
    this.isAnnotated = isAnnotated;
    this.contentType = contentType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    File file = (File) o;
    return isAnnotated == file.isAnnotated
        && id.equals(file.id)
        && fileId.equals(file.fileId)
        && filename.equals(file.filename)
        && fileType == file.fileType
        && idCategory == file.idCategory
        && uploadedAt.equals(file.uploadedAt)
        && username.equals(file.username)
        && organizationName.equals(file.organizationName)
        && contentType.equals(file.contentType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        fileId,
        filename,
        fileType,
        idCategory,
        uploadedAt,
        username,
        organizationName,
        isAnnotated,
        contentType);
  }

  @Override
  public String toString() {
    return "File{"
        + "id="
        + id
        + ", fileId="
        + fileId
        + ", filename='"
        + filename
        + '\''
        + ", fileType="
        + fileType
        + ", idCategory="
        + idCategory
        + ", uploadedAt="
        + uploadedAt
        + ", username='"
        + username
        + '\''
        + ", organizationName='"
        + organizationName
        + '\''
        + ", isAnnotated="
        + isAnnotated
        + ", contentType='"
        + contentType
        + '\''
        + '}';
  }

  /*
   Metadata fields from database:
   filetype (str/enum) DONE
   upload_date (date) DONE
   uploader (str) DONE
   organization_name (str) DONE
   annotated (bool) DONE

   hash of file and metadata for integrity checks
   https://developers.google.com/tink?authuser=2
  */
}
