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
  private EncryptionController encryptionController;

  public PdfControllerV2(
      FileDao fileDao,
      FormDao formDao,
      UserDao userDao,
      EncryptionController encryptionController) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.userDao = userDao;
    this.encryptionController = encryptionController;
  }

  public Handler deletePDF =
      ctx -> {
        log.info("Starting deletePDF handler");
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

  public Handler downloadPDF =
      ctx -> {
        log.info("Starting downloadPDF handler");
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
        Message response = downloadPDFServiceV2.executeAndGetResponse();
        if (response != PdfMessage.SUCCESS) {
          ctx.result(response.toResponseString());
          return;
        }
        ctx.header("Content-Type", "application/pdf");
        ctx.result(downloadPDFServiceV2.getDownloadedInputStream());
      };

  public Handler filterPDF =
      ctx -> {
        log.info("Starting filterPDF handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();
        JSONObject req = new JSONObject(ctx.body());

        Message setUserParamsErrorMessage = userParams.setUserParamsFromCtxReq(ctx, req, userDao);
        if (setUserParamsErrorMessage != null) {
          ctx.result(setUserParamsErrorMessage.toResponseString());
          return;
        }
        fileParams.setFileParamsGetFilesInformation(req);
        FilterPDFServiceV2 filterPDFServiceV2 =
            new FilterPDFServiceV2(fileDao, userParams, fileParams);
        Message response = filterPDFServiceV2.executeAndGetResponse();
        if (response != PdfMessage.SUCCESS) {
          ctx.result(response.toResponseString());
          return;
        }
        JSONObject responseJSON = response.toJSON();
        responseJSON.put("documents", filterPDFServiceV2.getFiles());
        ctx.result(responseJSON.toString());
      };

  public Handler uploadPDF =
      ctx -> {
        log.info("Starting uploadPDF handler");
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

  public Handler uploadAnnotatedPDF =
      ctx -> {
        log.info("Starting uploadAnnotatedPDF handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();

        userParams.setUserParamsUploadAnnotatedPDF(ctx);
        Message setFileParamsErrorMessage = fileParams.setFileParamsUploadAnnotatedPDF(ctx);
        if (setFileParamsErrorMessage != null) {
          ctx.result(setFileParamsErrorMessage.toResponseString());
          return;
        }
        UploadAnnotatedPDFServiceV2 uploadAnnotatedPDFServiceV2 =
            new UploadAnnotatedPDFServiceV2(
                fileDao, formDao, userDao, userParams, fileParams, encryptionController);
        ctx.result(uploadAnnotatedPDFServiceV2.executeAndGetResponse().toResponseString());
      };

  public Handler uploadSignedPDF =
      ctx -> {
        log.info("Starting uploadSignedPDF handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();
        Message setUserParamsErrorMessage = userParams.setUserParamsFillAndUploadSignedPDF(ctx);
        if (setUserParamsErrorMessage != null) {
          ctx.result(setUserParamsErrorMessage.toResponseString());
          return;
        }
        Message setFileParamsErrorMessage = fileParams.setfileParamsFillAndUploadSignedPDF(ctx);
        if (setFileParamsErrorMessage != null) {
          ctx.result(setFileParamsErrorMessage.toResponseString());
          return;
        }
        UploadSignedPDFServiceV2 uploadSignedPDFServiceV2 =
            new UploadSignedPDFServiceV2(
                fileDao, formDao, userParams, fileParams, encryptionController);
        ctx.result(uploadSignedPDFServiceV2.executeAndGetResponse().toResponseString());
      };

  public Handler getQuestions =
      ctx -> {
        log.info("Starting getQuestions handler");
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
        JSONObject applicationInformation = getQuestionsPDFServiceV2.getApplicationInformation();
        ctx.result(mergeJSON(response.toJSON(), applicationInformation).toString());
      };

  public Handler fillPDF =
      ctx -> {
        log.info("Starting fillPdfForm handler");
        UserParams userParams = new UserParams();
        FileParams fileParams = new FileParams();
        userParams.setUserParamsFillAndUploadSignedPDF(ctx);
        Message setFileParamsErrorMessage = fileParams.setfileParamsFillAndUploadSignedPDF(ctx);
        if (setFileParamsErrorMessage != null) {
          ctx.result(setFileParamsErrorMessage.toResponseString());
          return;
        }
        FillPDFServiceV2 fillPDFServiceV2 =
            new FillPDFServiceV2(fileDao, formDao, userParams, fileParams, encryptionController);
        Message response = fillPDFServiceV2.executeAndGetResponse();
        if (response != PdfMessage.SUCCESS) {
          ctx.result(response.toResponseString());
          return;
        }
        ctx.result(fillPDFServiceV2.getFilledFileStream());
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

    public Message setUserParamsFillAndUploadSignedPDF(Context ctx) {
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
    private String fileOrgName;

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
        InputStream signatureStream,
        String fileOrgName) {
      this.fileId = fileId;
      this.pdfType = pdfType;
      this.fileName = fileName;
      this.fileContentType = fileContentType;
      this.fileStream = fileStream;
      this.idCategoryType = idCategoryType;
      this.annotated = annotated;
      this.formAnswers = formAnswers;
      this.signatureStream = signatureStream;
      this.fileOrgName = fileOrgName;
    }

    public Message setfileParamsFillAndUploadSignedPDF(Context ctx) {
      try {
        UploadedFile signature = Objects.requireNonNull(ctx.uploadedFile("signature"));
        this.fileId = Objects.requireNonNull(ctx.formParam("applicationId"));
        this.formAnswers = new JSONObject(Objects.requireNonNull(ctx.formParam("formAnswers")));
        this.signatureStream = signature.getContent();
      } catch (Exception e) {
        return PdfMessage.INVALID_PARAMETER;
      }
      return null;
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
      //      this.fileOrgName = ctx.formParam("fileOrgName");
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

    public String getFileOrgName() {
      return fileOrgName;
    }

    public FileParams setFileOrgName(String fileOrgName) {
      this.fileOrgName = fileOrgName;
      return this;
    }
  }
}
