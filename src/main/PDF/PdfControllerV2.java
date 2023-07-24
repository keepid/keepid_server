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
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import java.io.InputStream;
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

  public PdfControllerV2(FileDao fileDao, FormDao formDao, UserDao userDao) {
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
        user = userDao.get(username);
      }
    } catch (JSONException e) {
      log.info("getTargetUserFromRequestString failed");
    }
    log.info("getTargetUserFromRequestString done");
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
    log.info("getTargetUserFromUsername done");
    return user;
  }

  public Handler pdfDelete =
      ctx -> {
        log.info("Starting pdfDelete handler");
        String username;
        String organizationName;
        UserType privilegeLevel;
        JSONObject req = new JSONObject(ctx.body());
        Optional<User> targetUserOptional = getTargetUserFromRequestString(ctx.body());
        if (targetUserOptional.isEmpty() && req.has("targetUser")) {
          log.info("Target user not found");
          ctx.result(UserMessage.USER_NOT_FOUND.toResponseString());
          return;
        }

        // Target User
        if (targetUserOptional.isPresent() && req.has("targetUser")) {
          log.info("Target user found");
          User targetUser = targetUserOptional.get();
          username = targetUser.getUsername();
          organizationName = targetUser.getOrganization();
          privilegeLevel = targetUser.getUserType();
          boolean sameOrg = organizationName.equals(ctx.sessionAttribute("orgName"));
          // Check if target user is in same org as session user
          if (!sameOrg) {
            ctx.result(UserMessage.CROSS_ORG_ACTION_DENIED.toResponseString());
            return;
          }
        } else { // Target User not in req, use session user info
          username = ctx.sessionAttribute("username");
          organizationName = ctx.sessionAttribute("orgName");
          privilegeLevel = ctx.sessionAttribute("privilegeLevel");
        }
        PDFTypeV2 pdfType = PDFTypeV2.createFromString(req.getString("pdfType"));
        String fileId = req.getString("fileId");
        DeletePDFServiceV2 deletePDFServiceV2 =
            new DeletePDFServiceV2(
                fileDao, formDao, username, organizationName, privilegeLevel, pdfType, fileId);
        ctx.result(deletePDFServiceV2.executeAndGetResponse().toResponseString());
      };

  public Handler pdfDownload = ctx -> {};

  public Handler pdfGetFilesInformation = ctx -> {};

  public Handler pdfUpload =
      ctx -> {
        log.info("Starting pdfUpload handler");
        String username;
        String organizationName;
        UserType privilegeLevel;
        Message response;
        IdCategoryType idCategoryType = IdCategoryType.NONE;
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
        // Target User
        if (targetUserOptional.isPresent() && targetUserName != null) {
          log.info("Target user found");
          User targetUser = targetUserOptional.get();
          username = targetUser.getUsername();
          organizationName = targetUser.getOrganization();
          privilegeLevel = targetUser.getUserType();
          boolean sameOrg = organizationName.equals(ctx.sessionAttribute("orgName"));
          // Check if target user is in same org as session user
          if (!sameOrg) {
            ctx.result(UserMessage.CROSS_ORG_ACTION_DENIED.toResponseString());
            return;
          }
        } else { // Target User not in req, use session user info
          username = ctx.sessionAttribute("username");
          organizationName = ctx.sessionAttribute("orgName");
          privilegeLevel = ctx.sessionAttribute("privilegeLevel");
        }
        PDFTypeV2 pdfType = PDFTypeV2.createFromString(ctx.formParam("pdfType"));
        if (ctx.formParam("idCategory") != null) {
          idCategoryType = IdCategoryType.createFromString(ctx.formParam("idCategory"));
        }
        if (pdfType == PDFTypeV2.CLIENT_UPLOADED_DOCUMENT
            && idCategoryType == IdCategoryType.NONE) {
          log.info("Client uploaded document missing category");
          ctx.result(PdfMessage.INVALID_ID_CATEGORY.toResponseString());
          return;
        }
        String fileName = file.getFilename();
        String fileContentType = file.getContentType();
        InputStream fileStream = file.getContent();
        UploadPDFServiceV2 uploadPDFServiceV2 =
            new UploadPDFServiceV2(
                fileDao,
                username,
                organizationName,
                privilegeLevel,
                pdfType,
                fileName,
                fileContentType,
                fileStream,
                idCategoryType,
                encryptionController);
        ctx.result(uploadPDFServiceV2.executeAndGetResponse().toResponseString());
      };

  public Handler pdfUploadAnnotated = ctx -> {};

  public Handler pdfSignedUpload = ctx -> {};

  public Handler getApplicationQuestions = ctx -> {};

  public Handler fillPDFForm = ctx -> {};
}
