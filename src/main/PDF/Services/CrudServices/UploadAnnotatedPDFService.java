package PDF.Services.CrudServices;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import PDF.PDFType;
import PDF.PdfMessage;
import PDF.Services.AnnotationServices.GetQuestionsPDFService;
import Security.EncryptionController;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import static com.mongodb.client.model.Filters.eq;

public class UploadAnnotatedPDFService implements Service {
  public static final int CHUNK_SIZE_BYTES = 100000;

  String uploader;
  String organizationName;
  UserType privilegeLevel;
  String fileIDStr;
  String filename;
  String fileContentType;
  InputStream fileStream;
  UserDao userDao;
  MongoDatabase db;
  EncryptionController encryptionController;

  public UploadAnnotatedPDFService(
      MongoDatabase db,
      UserDao userDao,
      String uploaderUsername,
      String organizationName,
      UserType privilegeLevel,
      String fileIDStr,
      String filename,
      String fileContentType,
      InputStream fileStream,
      EncryptionController encryptionController) {
    this.db = db;
    this.userDao = userDao;
    this.uploader = uploaderUsername;
    this.organizationName = organizationName;
    this.privilegeLevel = privilegeLevel;
    this.fileIDStr = fileIDStr;
    this.filename = filename;
    this.fileContentType = fileContentType;
    this.fileStream = fileStream;
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    if (fileStream == null) {
      return PdfMessage.INVALID_PDF;
    } else if (!fileContentType.equals("application/pdf")) {
      return PdfMessage.INVALID_PDF;
    } else {
      if (privilegeLevel == UserType.Developer) {
        try {
          return mongodbUploadAnnotatedForm();
        } catch (Exception e) {
          return PdfMessage.ENCRYPTION_ERROR;
        }
      } else {
        return PdfMessage.INSUFFICIENT_PRIVILEGE;
      }
    }
  }

  public Message mongodbUploadAnnotatedForm() throws IOException, GeneralSecurityException {
    ObjectId fileID = new ObjectId(fileIDStr);
    GridFSBucket gridBucket = GridFSBuckets.create(db, PDFType.BLANK_FORM.toString());
    GridFSFile grid_out = gridBucket.find(eq("_id", fileID)).first();
    if (grid_out == null || grid_out.getMetadata() == null) {
      return PdfMessage.NO_SUCH_FILE;
    }

    // Make sure form is properly annotated
    GetQuestionsPDFService getQuestionsPDFService =
        new GetQuestionsPDFService(userDao, privilegeLevel, uploader, fileStream);
    Message response = getQuestionsPDFService.executeAndGetResponse();
    if (response != PdfMessage.SUCCESS) {
      return response;
    }

    // Make metadata parameters the same as before
    Document previousMetadata = grid_out.getMetadata();
    previousMetadata.put("annotated", true);
    gridBucket.delete(fileID);

    // Forms don't need to be encrypted
    // InputStream inputStream = encryptionController.encryptFile(fileStream, uploader);
    fileStream.reset();
    InputStream inputStream = fileStream;

    GridFSUploadOptions options =
        new GridFSUploadOptions().chunkSizeBytes(CHUNK_SIZE_BYTES).metadata(previousMetadata);
    gridBucket.uploadFromStream(filename, inputStream, options);
    return PdfMessage.SUCCESS;
  }
}
