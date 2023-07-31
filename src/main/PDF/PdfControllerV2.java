package PDF;

import static User.UserController.mergeJSON;

import Config.Message;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.User.UserDao;
import File.IdCategoryType;
import PDF.Services.V2Services.*;
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

  // Needed for EncryptionController
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

  public Handler pdfDelete =
      ctx -> {
        log.info("Starting pdfDelete handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();
        JSONObject req = new JSONObject(ctx.body());

        Message setUserParamsErrorMessage = userParams.setUserParamsFromCtxReq(ctx, req, userDao);
        if (setUserParamsErrorMessage != null) {
          ctx.result(setUserParamsErrorMessage.toResponseString());
          return;
        }
        fileParams.setFileParamsDeleteAndDownloadPDF(req);
        DeletePDFServiceV2 deletePDFServiceV2 =
            new DeletePDFServiceV2(fileDao, formDao, userParams, fileParams);
        ctx.result(deletePDFServiceV2.executeAndGetResponse().toResponseString());
      };

  public Handler pdfDownload =
      ctx -> {
        log.info("Starting pdfDownload handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();
        JSONObject req = new JSONObject(ctx.body());

        Message setUserParamsErrorMessage = userParams.setUserParamsFromCtxReq(ctx, req, userDao);
        if (setUserParamsErrorMessage != null) {
          ctx.result(setUserParamsErrorMessage.toResponseString());
          return;
        }
        fileParams.setFileParamsDeleteAndDownloadPDF(req);

        DownloadPDFServiceV2 downloadPDFServiceV2 =
            new DownloadPDFServiceV2(
                fileDao, formDao, userParams, fileParams, encryptionController);
        // NEED TO FINISH SERVICE
        //
        //
        Message response = downloadPDFServiceV2.executeAndGetResponse();
        if (response != PdfMessage.SUCCESS) {
          ctx.result(response.toResponseString());
          return;
        }
        ctx.header("Content-Type", "application/pdf");
        ctx.result(downloadPDFServiceV2.getDownloadedInputStream());
      };

  public Handler pdfGetFilesInformation =
      ctx -> {
        log.info("Starting pdfGetFilesInformation handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();
        JSONObject req = new JSONObject(ctx.body());

        Message setUserParamsErrorMessage = userParams.setUserParamsFromCtxReq(ctx, req, userDao);
        if (setUserParamsErrorMessage != null) {
          ctx.result(setUserParamsErrorMessage.toResponseString());
          return;
        }
        fileParams.setFileParamsGetFilesInformation(req);
        GetFileInformationPDFServiceV2 getFileInformationPDFServiceV2 =
            new GetFileInformationPDFServiceV2(fileDao, formDao, userParams, fileParams);
        // NEED TO FINISH SERVICE
        //
        //
        Message response = getFileInformationPDFServiceV2.executeAndGetResponse();
        if (response != PdfMessage.SUCCESS) {
          ctx.result(response.toResponseString());
          return;
        }
        JSONObject responseJSON = response.toJSON();
        responseJSON.put("documents", getFileInformationPDFServiceV2.getFiles());
        ctx.result(responseJSON.toString());
      };

  public Handler pdfUpload =
      ctx -> {
        log.info("Starting pdfUpload handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();

        Message setUserParamsErrorMessage = userParams.setUserParamsFromCtx(ctx, userDao);
        if (setUserParamsErrorMessage != null) {
          ctx.result(setUserParamsErrorMessage.toResponseString());
          return;
        }
        Message setFileParamsErrorMessage = fileParams.setFileParamsUploadPDF(ctx);
        if (setFileParamsErrorMessage != null) {
          ctx.result(setFileParamsErrorMessage.toResponseString());
          return;
        }

        UploadPDFServiceV2 uploadPDFServiceV2 =
            new UploadPDFServiceV2(fileDao, userParams, fileParams, encryptionController);
        ctx.result(uploadPDFServiceV2.executeAndGetResponse().toResponseString());
      };

  public Handler pdfUploadAnnotated =
      ctx -> {
        log.info("Starting pdfUploadAnnotated handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();

        userParams.setUserParamsUploadAnnotatedPDF(ctx);
        Message setFileParamsErrorMessage = fileParams.setFileParamsUploadAnnotatedPDF(ctx);
        if (setFileParamsErrorMessage != null) {
          ctx.result(setFileParamsErrorMessage.toResponseString());
          return;
        }
        // NEED TO FINISH SERVICE
        //
        //
        UploadAnnotatedPDFServiceV2 uploadAnnotatedPDFServiceV2 =
            new UploadAnnotatedPDFServiceV2(
                fileDao, formDao, userDao, userParams, fileParams, encryptionController);
        ctx.result(uploadAnnotatedPDFServiceV2.executeAndGetResponse().toResponseString());
      };

  public Handler pdfSignedUpload =
      ctx -> {
        log.info("Starting pdfSignedUpload handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();
        Message setUserParamsErrorMessage = userParams.setUserParamsUploadSignedPDF(ctx);
        if (setUserParamsErrorMessage != null) {
          ctx.result(setUserParamsErrorMessage.toResponseString());
          return;
        }
        Message setFileParamsErrorMessage = fileParams.setFileParamsUploadSignedPDF(ctx);
        if (setFileParamsErrorMessage != null) {
          ctx.result(setFileParamsErrorMessage.toResponseString());
          return;
        }
        // NEED TO FINISH SERVICE
        //
        //
        UploadSignedPDFServiceV2 uploadSignedPDFServiceV2 =
            new UploadSignedPDFServiceV2(
                fileDao, formDao, userParams, fileParams, encryptionController);
        ctx.result(uploadSignedPDFServiceV2.executeAndGetResponse().toResponseString());
      };

  public Handler getApplicationQuestions =
      ctx -> {
        log.info("Starting getApplicationQuestions handler");
        JSONObject req = new JSONObject(ctx.body());
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();
        userParams.setUserParamsGetApplicationQuestions(ctx, req);
        fileParams.setFileParamsGetApplicationQuestions(req);
        GetQuestionsPDFServiceV2 getQuestionsPDFServiceV2 =
            new GetQuestionsPDFServiceV2(formDao, userDao, userParams, fileParams);
        Message response = getQuestionsPDFServiceV2.executeAndGetResponse();
        if (response != PdfMessage.SUCCESS) {
          ctx.result(response.toResponseString());
          return;
        }
        // WRAP UP SERVICE
        //
        //
        JSONObject applicationInformation = getQuestionsPDFServiceV2.getApplicationInformation();
        ctx.result(mergeJSON(response.toJSON(), applicationInformation).toString());
      };

  public Handler fillPdfForm =
      ctx -> {
        log.info("Starting fillPdfForm handler");
        JSONObject req = new JSONObject(ctx.body());
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();
        userParams.setUserParamsFillPDFForm(ctx);
        fileParams.setFileParamsFillPDFForm(ctx, req);
        FillPDFServiceV2 fillPDFServiceV2 =
            new FillPDFServiceV2(fileDao, formDao, userParams, fileParams);
        // NEED TO FINISH SERVICE
        //
        //
        Message response = fillPDFServiceV2.executeAndGetResponse();
        if (response != PdfMessage.SUCCESS) {
          ctx.result(response.toResponseString());
          return;
        }
        ctx.result(fillPDFServiceV2.getFilledForm());
      };

  public static class UserParams {
    private String username;
    private String organizationName;
    private UserType privilegeLevel;

    public UserParams() {}

    public UserParams(String username, String organizationName, UserType privilegeLevel) {
      this.username = username;
      this.organizationName = organizationName;
      this.privilegeLevel = privilegeLevel;
    }

    public Optional<User> getTargetUserFromRequestString(String req, UserDao userDao) {
      log.info("getTargetUserFromRequestString helper started");
      String username;
      Optional<User> user = Optional.empty();
      try {
        JSONObject reqJson = new JSONObject(req);
        if (reqJson.has("targetUser")) {
          username = reqJson.getString("targetUser");
          user = getTargetUserFromUsername(username, userDao);
        }
      } catch (JSONException e) {
        log.info("getTargetUserFromRequestString failed");
      }
      return user;
    }

    public Optional<User> getTargetUserFromUsername(String username, UserDao userDao) {
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

    public Message setUserParamsUploadSignedPDF(Context ctx) {
      try {
        String sessionUsername = ctx.sessionAttribute("username");
        String clientUsernameParameter = Objects.requireNonNull(ctx.formParam("clientUsername"));
        this.username =
            clientUsernameParameter.equals("") ? sessionUsername : clientUsernameParameter;
      } catch (Exception e) {
        return PdfMessage.INVALID_PARAMETER;
      }
      this.privilegeLevel = ctx.sessionAttribute("privilegeLevel");
      this.organizationName = ctx.sessionAttribute("orgName");
      return null;
    }

    public void setUserParamsFillPDFForm(Context ctx) {
      this.privilegeLevel = ctx.sessionAttribute("privilegeLevel");
    }

    public void setUserParamsUploadAnnotatedPDF(Context ctx) {
      this.username = ctx.sessionAttribute("username");
      this.organizationName = ctx.sessionAttribute("orgName");
      this.privilegeLevel = UserType.Developer;
    }

    public void setUserParamsGetApplicationQuestions(Context ctx, JSONObject req) {
      String sessionUsername = ctx.sessionAttribute("username");
      String clientUsernameParameter = req.getString("clientUsername");
      this.username =
          clientUsernameParameter.equals("") ? sessionUsername : clientUsernameParameter;
      this.privilegeLevel = ctx.sessionAttribute("privilegeLevel");
    }

    public Message setUserParamsFromCtxReq(Context ctx, JSONObject req, UserDao userDao) {
      Optional<User> targetUserOptional = getTargetUserFromRequestString(ctx.body(), userDao);
      if (targetUserOptional.isEmpty() && req.has("targetUser")) {
        log.info("Target user not found");
        return UserMessage.USER_NOT_FOUND;
      }
      return setUserParamsFromUserData(targetUserOptional, ctx);
    }

    public Message setUserParamsFromCtx(Context ctx, UserDao userDao) {
      // targetUserName is null if targetUser parameter doesn't exist
      String targetUserName = ctx.formParam("targetUser");
      Optional<User> targetUserOptional = getTargetUserFromUsername(targetUserName, userDao);
      if (targetUserOptional.isEmpty() && targetUserName != null) {
        log.info("Target user not found");
        return UserMessage.USER_NOT_FOUND;
      }
      return setUserParamsFromUserData(targetUserOptional, ctx);
    }

    public Message setUserParamsFromUserData(Optional<User> targetUserOptional, Context ctx) {
      // Target User
      if (targetUserOptional.isPresent()) {
        log.info("Target user found");
        User targetUser = targetUserOptional.get();
        return setUserParamsFromTargetUser(targetUser, ctx);
      } else { // Target User not in req, use session user info
        log.info("Getting session user data");
        return setUserParamsFromSessionUser(ctx);
      }
    }

    public Message setUserParamsFromTargetUser(User targetUser, Context ctx) {
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

    public Message setUserParamsFromSessionUser(Context ctx) {
      this.username = ctx.sessionAttribute("username");
      this.organizationName = ctx.sessionAttribute("orgName");
      this.privilegeLevel = ctx.sessionAttribute("privilegeLevel");
      return null;
    }

    public String getUsername() {
      return username;
    }

    public UserParams setUsername(String username) {
      this.username = username;
      return this;
    }

    public String getOrganizationName() {
      return organizationName;
    }

    public UserParams setOrganizationName(String organizationName) {
      this.organizationName = organizationName;
      return this;
    }

    public UserType getPrivilegeLevel() {
      return privilegeLevel;
    }

    public UserParams setPrivilegeLevel(UserType privilegeLevel) {
      this.privilegeLevel = privilegeLevel;
      return this;
    }
  }

  public static class FileParams {
    private String fileId;
    private PDFTypeV2 pdfType;
    private String fileName;
    private String fileContentType;
    private InputStream fileStream;
    private IdCategoryType idCategoryType;
    private boolean annotated;
    private JSONObject formAnswers;
    private InputStream signatureStream;

    public FileParams() {}

    public FileParams(
        String fileId,
        PDFTypeV2 pdfType,
        String fileName,
        String fileContentType,
        InputStream fileStream,
        IdCategoryType idCategoryType,
        boolean annotated,
        JSONObject formAnswers,
        InputStream signatureStream) {
      this.fileId = fileId;
      this.pdfType = pdfType;
      this.fileName = fileName;
      this.fileContentType = fileContentType;
      this.fileStream = fileStream;
      this.idCategoryType = idCategoryType;
      this.annotated = annotated;
      this.formAnswers = formAnswers;
      this.signatureStream = signatureStream;
    }

    public Message setFileParamsUploadSignedPDF(Context ctx) {
      try {
        UploadedFile file = Objects.requireNonNull(ctx.uploadedFile("file"));
        UploadedFile signature = Objects.requireNonNull(ctx.uploadedFile("signature"));
        this.pdfType = PDFTypeV2.createFromString(Objects.requireNonNull(ctx.formParam("pdfType")));
        this.fileName = file.getFilename();
        this.fileContentType = file.getContentType();
        this.fileStream = file.getContent();
        this.signatureStream = signature.getContent();
      } catch (Exception e) {
        return PdfMessage.INVALID_PARAMETER;
      }
      return null;
    }

    public void setFileParamsFillPDFForm(Context ctx, JSONObject req) {
      this.fileId = req.getString("applicationId");
      this.formAnswers = req.getJSONObject("formAnswers");
    }

    public void setFileParamsGetFilesInformation(JSONObject req) {
      this.pdfType = PDFTypeV2.createFromString(req.getString("pdfType"));
      this.annotated = false;
      if (pdfType == PDFTypeV2.BLANK_APPLICATION) {
        this.annotated = req.getBoolean("annotated");
      }
    }

    public void setFileParamsDeleteAndDownloadPDF(JSONObject req) {
      this.pdfType = PDFTypeV2.createFromString(req.getString("pdfType"));
      this.fileId = req.getString("fileId");
    }

    public void setFileParamsGetApplicationQuestions(JSONObject req) {
      this.fileId = req.getString("applicationId");
    }

    public Message setFileParamsUploadPDF(Context ctx) {
      UploadedFile file = ctx.uploadedFile("file");
      if (file == null) {
        log.info("File is null");
        return PdfMessage.INVALID_PDF;
      }
      try {
        this.pdfType = PDFTypeV2.createFromString(Objects.requireNonNull(ctx.formParam("pdfType")));
        this.idCategoryType = IdCategoryType.NONE;
        if (ctx.formParam("idCategory") != null) {
          this.idCategoryType =
              IdCategoryType.createFromString(Objects.requireNonNull(ctx.formParam("idCategory")));
        }
      } catch (Exception e) {
        return PdfMessage.INVALID_PARAMETER;
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

    public Message setFileParamsUploadAnnotatedPDF(Context ctx) {
      UploadedFile file = ctx.uploadedFile("file");
      if (file == null) {
        log.info("File is null");
        return PdfMessage.INVALID_PDF;
      }
      this.fileId = ctx.formParam("fileId");
      this.fileName = file.getFilename();
      this.fileContentType = file.getContentType();
      this.fileStream = file.getContent();
      return null;
    }

    public String getFileId() {
      return fileId;
    }

    public FileParams setFileId(String fileId) {
      this.fileId = fileId;
      return this;
    }

    public PDFTypeV2 getPdfType() {
      return pdfType;
    }

    public FileParams setPdfType(PDFTypeV2 pdfType) {
      this.pdfType = pdfType;
      return this;
    }

    public String getFileName() {
      return fileName;
    }

    public FileParams setFileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public String getFileContentType() {
      return fileContentType;
    }

    public FileParams setFileContentType(String fileContentType) {
      this.fileContentType = fileContentType;
      return this;
    }

    public InputStream getFileStream() {
      return fileStream;
    }

    public FileParams setFileStream(InputStream fileStream) {
      this.fileStream = fileStream;
      return this;
    }

    public IdCategoryType getIdCategoryType() {
      return idCategoryType;
    }

    public FileParams setIdCategoryType(IdCategoryType idCategoryType) {
      this.idCategoryType = idCategoryType;
      return this;
    }

    public boolean getAnnotated() {
      return annotated;
    }

    public FileParams setAnnotated(boolean annotated) {
      this.annotated = annotated;
      return this;
    }

    public JSONObject getFormAnswers() {
      return formAnswers;
    }

    public FileParams setFormAnswers(JSONObject formAnswers) {
      this.formAnswers = formAnswers;
      return this;
    }

    public InputStream getSignatureStream() {
      return signatureStream;
    }

    public FileParams setSignatureStream(InputStream signatureStream) {
      this.signatureStream = signatureStream;
      return this;
    }
  }
}
