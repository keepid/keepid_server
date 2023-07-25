package PDF;

import Config.Message;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.User.UserDao;
import File.IdCategoryType;
import PDF.Services.V2Services.DeletePDFServiceV2;
import PDF.Services.V2Services.UploadPDFServiceV2;
import Security.EncryptionController;
import User.User;
import User.UserMessage;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;

@Slf4j
public class PdfControllerV2 {
  private FormDao formDao;
  private FileDao fileDao;
  private UserDao userDao;

  // Temporarily needed for EncryptionController
  private MongoDatabase db;
  private EncryptionController encryptionController;

  public PdfControllerV2(FileDao fileDao, FormDao formDao, UserDao userDao, MongoDatabase db) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.userDao = userDao;
    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {
      log.error("Generating encryption controller failed");
    }
  }

  public Optional<User> getTargetUserFromRequestString(String req) {
    log.info("getTargetUserFromRequestString helper started");
    String username;
    Optional<User> user = Optional.empty();
    try {
      JSONObject reqJson = new JSONObject(req);
      if (reqJson.has("targetUser")) {
        username = reqJson.getString("targetUser");
        user = getTargetUserFromUsername(username);
      }
    } catch (JSONException e) {
      log.info("getTargetUserFromRequestString failed");
    }
    return user;
  }

  public Optional<User> getTargetUserFromUsername(String username) {
    log.info("getTargetUserFromUsername helper started");
    Optional<User> user = Optional.empty();
    if (username == null) {
      return user;
    }
    try {
      user = userDao.get(username);
    } catch (JSONException e) {
      log.info("getTargetUserFromUsername failed");
    }
    return user;
  }

  public Handler pdfDelete =
      ctx -> {
        log.info("Starting pdfDelete handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();
        JSONObject req = new JSONObject(ctx.body());

        Optional<User> targetUserOptional = getTargetUserFromRequestString(ctx.body());
        if (targetUserOptional.isEmpty() && req.has("targetUser")) {
          log.info("Target user not found");
          ctx.result(UserMessage.USER_NOT_FOUND.toResponseString());
          return;
        }

        Message setUserParamsErrorMessage =
            userParams.setParamsFromUserData(targetUserOptional, ctx);
        if (setUserParamsErrorMessage != null) {
          ctx.result(setUserParamsErrorMessage.toResponseString());
          return;
        }
        fileParams.setFileParamsDeletePDF(req);

        DeletePDFServiceV2 deletePDFServiceV2 =
            new DeletePDFServiceV2(fileDao, formDao, userParams, fileParams);
        ctx.result(deletePDFServiceV2.executeAndGetResponse().toResponseString());
      };

  public Handler pdfDownload = ctx -> {};

  public Handler pdfGetFilesInformation = ctx -> {};

  public Handler pdfUpload =
      ctx -> {
        log.info("Starting pdfUpload handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();
        UploadedFile file = ctx.uploadedFile("file");

        if (file == null) {
          log.info("File is null");
          ctx.result(PdfMessage.INVALID_PDF.toResponseString());
          return;
        }

        // targetUserName is null if targetUser parameter doesn't exist
        String targetUserName = ctx.formParam("targetUser");
        Optional<User> targetUserOptional = getTargetUserFromUsername(targetUserName);
        if (targetUserOptional.isEmpty() && targetUserName != null) {
          log.info("Target user not found");
          ctx.result(UserMessage.USER_NOT_FOUND.toResponseString());
          return;
        }

        Message setUserParamsErrorMessage =
            userParams.setParamsFromUserData(targetUserOptional, ctx);
        if (setUserParamsErrorMessage != null) {
          ctx.result(setUserParamsErrorMessage.toResponseString());
          return;
        }
        Message setFileParamsErrorMessage = fileParams.setFileParamsUploadPDF(ctx, file);
        if (setFileParamsErrorMessage != null) {
          ctx.result(setFileParamsErrorMessage.toResponseString());
        }

        UploadPDFServiceV2 uploadPDFServiceV2 =
            new UploadPDFServiceV2(fileDao, userParams, fileParams, encryptionController);
        ctx.result(uploadPDFServiceV2.executeAndGetResponse().toResponseString());
      };

  public Handler pdfUploadAnnotated = ctx -> {};

  public Handler pdfSignedUpload = ctx -> {};

  public Handler getApplicationQuestions = ctx -> {};

  public Handler fillPDFForm = ctx -> {};

  public static class UserParams {
    private String username;
    private String organizationName;
    private UserType privilegeLevel;

    public UserParams() {}

    public Message setParamsFromUserData(Optional<User> targetUserOptional, Context ctx) {
      // Target User
      if (targetUserOptional.isPresent()) {
        log.info("Target user found");
        User targetUser = targetUserOptional.get();
        return setParamsFromTargetUser(targetUser, ctx);
      } else { // Target User not in req, use session user info
        log.info("Getting session user data");
        return setParamsFromSessionUser(ctx);
      }
    }

    public Message setParamsFromTargetUser(User targetUser, Context ctx) {
      String targetUserOrg = targetUser.getOrganization();
      boolean sameOrg = targetUserOrg.equals(ctx.sessionAttribute("orgName"));
      // Check if target user is in same org as session user
      if (!sameOrg) {
        return UserMessage.CROSS_ORG_ACTION_DENIED;
      }
      this.username = targetUser.getUsername();
      this.organizationName = targetUserOrg;
      this.privilegeLevel = targetUser.getUserType();
      return null;
    }

    public Message setParamsFromSessionUser(Context ctx) {
      this.username = ctx.sessionAttribute("username");
      this.organizationName = ctx.sessionAttribute("orgName");
      this.privilegeLevel = ctx.sessionAttribute("privilegeLevel");
      return null;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getOrganizationName() {
      return organizationName;
    }

    public void setOrganizationName(String organizationName) {
      this.organizationName = organizationName;
    }

    public UserType getPrivilegeLevel() {
      return privilegeLevel;
    }

    public void setPrivilegeLevel(UserType privilegeLevel) {
      this.privilegeLevel = privilegeLevel;
    }
  }

  public static class FileParams {
    private String fileId;
    private PDFTypeV2 pdfType;
    private String fileName;
    private String fileContentType;

    private InputStream fileStream;
    private IdCategoryType idCategoryType;

    public FileParams() {}

    public void setFileParamsDeletePDF(JSONObject req) {
      this.pdfType = PDFTypeV2.createFromString(req.getString("pdfType"));
      this.fileId = req.getString("fileId");
    }

    public Message setFileParamsUploadPDF(Context ctx, UploadedFile file) {
      this.pdfType = PDFTypeV2.createFromString(Objects.requireNonNull(ctx.formParam("pdfType")));
      this.idCategoryType = IdCategoryType.NONE;
      if (ctx.formParam("idCategory") != null) {
        this.idCategoryType =
            IdCategoryType.createFromString(Objects.requireNonNull(ctx.formParam("idCategory")));
      }
      if (this.pdfType == PDFTypeV2.CLIENT_UPLOADED_DOCUMENT
          && this.idCategoryType == IdCategoryType.NONE) {
        log.info("Client uploaded document missing category");
        return PdfMessage.INVALID_ID_CATEGORY;
      }
      this.fileName = file.getFilename();
      this.fileContentType = file.getContentType();
      this.fileStream = file.getContent();
      return null;
    }

    public String getFileId() {
      return fileId;
    }

    public void setFileId(String fileId) {
      this.fileId = fileId;
    }

    public PDFTypeV2 getPdfType() {
      return pdfType;
    }

    public void setPdfType(PDFTypeV2 pdfType) {
      this.pdfType = pdfType;
    }

    public String getFileName() {
      return fileName;
    }

    public void setFileName(String fileName) {
      this.fileName = fileName;
    }

    public String getFileContentType() {
      return fileContentType;
    }

    public void setFileContentType(String fileContentType) {
      this.fileContentType = fileContentType;
    }

    public InputStream getFileStream() {
      return fileStream;
    }

    public void setFileStream(InputStream fileStream) {
      this.fileStream = fileStream;
    }

    public IdCategoryType getIdCategoryType() {
      return idCategoryType;
    }

    public void setIdCategoryType(IdCategoryType idCategoryType) {
      this.idCategoryType = idCategoryType;
    }
  }
}
