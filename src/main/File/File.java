package File;

import com.google.api.client.util.DateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import java.io.InputStream;

@Slf4j
public class File {
  @Getter @Setter private ObjectId id;
  @Getter @Setter private ObjectId fileId;
  @Getter @Setter private InputStream fileStream;
  @Getter @Setter private FileType fileType;
  @Getter @Setter private DateTime uploadedAt;
  @Getter @Setter private String username;
  @Getter @Setter private String organizationName;
  @Getter @Setter private boolean isAnnotated;

  public File() {}

  public File(String username, DateTime uploadedAt, InputStream fileStream, FileType fileType) {
    this.id = new ObjectId();
    this.fileId = new ObjectId();
    this.username = username;
    this.uploadedAt = uploadedAt;
    this.fileStream = fileStream;
    this.fileType = fileType;
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
