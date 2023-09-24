package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.User.UserDao;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import Security.EncryptionController;
import User.UserType;
import Validation.ValidationUtils;
import java.io.InputStream;
import org.bson.types.ObjectId;

public class UploadAnnotatedPDFServiceV2 implements Service {
  public static final int CHUNK_SIZE_BYTES = 100000;
  private FileDao fileDao;
  private FormDao formDao;
  private UserDao userDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private String fileId;
  private String fileName;
  private String fileContentType;
  private InputStream fileStream;
  private EncryptionController encryptionController;

  public UploadAnnotatedPDFServiceV2(
      FileDao fileDao,
      FormDao formDao,
      UserDao userDao,
      UserParams userParams,
      FileParams fileParams,
      EncryptionController encryptionController) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.userDao = userDao;
    this.username = userParams.getUsername();
    this.organizationName = userParams.getOrganizationName();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.fileId = fileParams.getFileId();
    this.fileName = fileParams.getFileName();
    this.fileContentType = fileParams.getFileContentType();
    this.fileStream = fileParams.getFileStream();
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    Message uploadConditionsErrorMessage = checkUploadConditions();
    if (uploadConditionsErrorMessage != null) {
      return uploadConditionsErrorMessage;
    }
    return upload();
  }

  public Message checkUploadConditions() {
    if (!ValidationUtils.isValidObjectId(fileId)) {
      return PdfMessage.INVALID_PARAMETER;
    }
    if (fileStream == null || !fileContentType.equals("application/pdf")) {
      return PdfMessage.INVALID_PDF;
    }
    if (privilegeLevel != UserType.Developer) {
      return PdfMessage.INSUFFICIENT_PRIVILEGE;
    }
    return null;
  }

  public Message upload() {
    ObjectId fileObjectId = new ObjectId(fileId);
    return null;
    // NEED TO FINISH GETQUESTIONSPDFSERVICE?
    //
    //
  }
}
