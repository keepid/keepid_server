package File;

import static User.UserController.mergeJSON;

import Config.Message;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import Database.User.UserDao;
import File.Services.*;
import PDF.PdfMessage;
import PDF.Services.CrudServices.ImageToPDFService;
import Security.EncryptionController;
import User.Services.GetUserInfoService;
import User.User;
import User.UserMessage;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.JSONObject;

@Slf4j
public class FileController {
  private UserDao userDao;
  private FileDao fileDao;
  private ActivityDao activityDao;
  private EncryptionController encryptionController;

  public FileController(
      MongoDatabase db,
      UserDao userDao,
      FileDao fileDao,
      ActivityDao activityDao,
      EncryptionController encryptionController) {
    this.userDao = userDao;
    this.fileDao = fileDao;
    this.activityDao = activityDao;
    this.encryptionController = encryptionController;
  }

  /*
  Multipart body with following fields:
  - "fileType": String giving the file type (see FileType enum)
  - if "fileType" is a PDF type
    - "toSign": boolean indicating whether or not the PDF needs signing
    - if "toSign" is True
      - "signature": the signature image to place in file
  - if "fileType" is of FORM_PDF
    - "annotated": boolean for setting whether the PDF is annotated (default false)
    - if "annotated" is True
      - "fileID": the fileID to replace
  - "file": the file to be uploaded
  - "targetUser": the user the file is being uploaded for
   */
  public Handler fileUpload =
      ctx -> {
        log.info("Uploading file...");
        String username;
        String organizationName;
        UserType privilegeLevel;
        Message response = null;
        UploadedFile file = ctx.uploadedFile("file");
        JSONObject req = new JSONObject();
        String body = null;
        try {
          req.put("targetUser", ctx.formParam("targetUser"));
          req.put("idCategory", ctx.formParam("idCategory"));
          req.put("fileType", ctx.formParam("fileType"));
          body = req.toString();
        } catch (Exception e) {
          System.out.println(e);
          req = null;
        }
        Optional<User> maybeTargetUser = GetUserInfoService.getUserFromRequest(this.userDao, body);
        if (maybeTargetUser.isEmpty() && req.has("targetUser")) {
          log.info("Target user could not be found in the database");
          response = UserMessage.USER_NOT_FOUND;
        } else {
          boolean orgFlag;
          if (req != null && req.has("targetUser") && maybeTargetUser.isPresent()) {
            log.info("Target user found, setting parameters.");
            username = maybeTargetUser.get().getUsername();
            organizationName = maybeTargetUser.get().getOrganization();
            privilegeLevel = maybeTargetUser.get().getUserType();
            orgFlag = organizationName.equals(ctx.sessionAttribute("orgName"));
          } else {
            log.info("Checking session for user.");
            username = ctx.sessionAttribute("username");
            organizationName = ctx.sessionAttribute("orgName");
            privilegeLevel = ctx.sessionAttribute("privilegeLevel");
            orgFlag = true;
          }
          if (orgFlag) {
            if (file == null) {
              log.info("File is null, invalid file!");
              response = FileMessage.INVALID_FILE;
            } else {
              FileType fileType =
                  FileType.createFromString(Objects.requireNonNull(ctx.formParam("fileType")));
              log.info("Received file type of {}", fileType.toString());
              boolean annotated = false;
              IdCategoryType idCategory = IdCategoryType.NONE;
              boolean toSign = false;
              if (ctx.formParam("annotated") != null) {
                annotated = Boolean.parseBoolean(ctx.formParam("annotated"));
              }
              if (ctx.formParam("idCategory") != null) {
                idCategory = IdCategoryType.createFromString(ctx.formParam("idCategory"));
              }
              if (ctx.formParam("toSign") != null) {
                toSign = Boolean.parseBoolean(ctx.formParam("toSign"));
              }
              String fileId = null;
              UploadedFile signature = null;
              Date uploadDate =
                  Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
              InputStream filestreamToUpload = file.getContent();
              String filenameToUpload = file.getFilename();
              switch (fileType) {
                case APPLICATION_PDF:
                case IDENTIFICATION_PDF:
                case FORM:
                  log.info("Got PDF file to upload!");
                  if (file.getContentType().startsWith("image")) {
                    ImageToPDFService imageToPDFService = new ImageToPDFService(filestreamToUpload);
                    Message imageToPdfServiceResponse = imageToPDFService.executeAndGetResponse();
                    if (imageToPdfServiceResponse == PdfMessage.INVALID_PDF) {
                      ctx.result(imageToPdfServiceResponse.toResponseString());
                    }
                    filestreamToUpload = imageToPDFService.getFileStream();
                    filenameToUpload =
                        file.getFilename().substring(0, file.getFilename().lastIndexOf("."))
                            + ".pdf";
                  }
                  filestreamToUpload.reset();
                  //                    PDDocument pdfDocument = Loader.loadPDF(filestreamToUpload);
                  //                    title = getPDFTitle(file.getFilename(), pdfDocument);
                  //                    pdfDocument.close();

                  if (toSign) {
                    signature = Objects.requireNonNull(ctx.uploadedFile("signature"));
                  }

                  if (fileType == FileType.FORM && annotated) {
                    fileId = Objects.requireNonNull(ctx.formParam("fileID"));
                  }
                  File fileToUpload =
                      new File(
                          username,
                          uploadDate,
                          filestreamToUpload,
                          fileType,
                          idCategory,
                          filenameToUpload,
                          organizationName,
                          annotated,
                          file.getContentType());
                  UploadFileService uploadService =
                      new UploadFileService(
                          fileDao,
                          activityDao,
                          fileToUpload,
                          Optional.ofNullable(privilegeLevel),
                          Optional.ofNullable(fileId),
                          toSign,
                          signature == null
                              ? Optional.empty()
                              : Optional.of(signature.getContent()),
                          Optional.ofNullable(encryptionController));
                  response = uploadService.executeAndGetResponse();
                  break;
                case PROFILE_PICTURE:
                  log.info("Got profile picture to upload!");
                  fileToUpload =
                      new File(
                          username,
                          uploadDate,
                          filestreamToUpload,
                          fileType,
                          idCategory,
                          filenameToUpload,
                          organizationName,
                          annotated,
                          file.getContentType());
                  uploadService =
                      new UploadFileService(
                          fileDao,
                          activityDao,
                          fileToUpload,
                          Optional.ofNullable(privilegeLevel),
                          Optional.ofNullable(fileId),
                          toSign,
                          Optional.empty(),
                          Optional.ofNullable(encryptionController));
                  response = uploadService.executeAndGetResponse();
                  break;
                case MISC:
                  log.info("Got miscellaneous file to upload!");
                  fileToUpload =
                      new File(
                          username,
                          uploadDate,
                          filestreamToUpload,
                          fileType,
                          idCategory,
                          filenameToUpload,
                          organizationName,
                          annotated,
                          file.getContentType());
                  uploadService =
                      new UploadFileService(
                          fileDao,
                          activityDao,
                          fileToUpload,
                          Optional.ofNullable(privilegeLevel),
                          Optional.ofNullable(fileId),
                          toSign,
                          Optional.empty(),
                          Optional.ofNullable(encryptionController));
                  response = uploadService.executeAndGetResponse();
                  break;
              }
            }
          } else {
            response = UserMessage.CROSS_ORG_ACTION_DENIED;
          }
        }

        ctx.result(response.toResponseString());
      };

  /*
  REQUIRES JSON Body:
    - "fileType": String giving File Type (see FileType enum)
    - "fileId": String giving id of file to be downloaded
    - OPTIONAL- "targetUser": User whose file you want to access.
        - If left empty, defaults to original username.
  */
  public Handler fileDownload =
      ctx -> {
        String username;
        String orgName;
        UserType userType;
        JSONObject req = new JSONObject(ctx.body());
        Optional<User> maybeTargetUser =
            GetUserInfoService.getUserFromRequest(this.userDao, ctx.body());
        if (maybeTargetUser.isEmpty() && req.has("targetUser")) {
          log.info("Target User not Found");
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          boolean orgFlag;
          if (maybeTargetUser.isPresent() && req.has("targetUser")) {
            log.info("Target user found");
            username = maybeTargetUser.get().getUsername();
            orgName = maybeTargetUser.get().getOrganization();
            userType = maybeTargetUser.get().getUserType();
            orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
          } else {
            username = ctx.sessionAttribute("username");
            orgName = ctx.sessionAttribute("orgName");
            userType = ctx.sessionAttribute("privilegeLevel");
            orgFlag = true;
          }

          if (orgFlag) {
            String fileIDStr = req.getString("fileId");
            String fileTypeStr = req.getString("fileType");
            FileType fileType = FileType.createFromString(fileTypeStr);
            DownloadFileService downloadFileService =
                new DownloadFileService(
                    fileDao,
                    username,
                    Optional.ofNullable(orgName),
                    Optional.ofNullable(userType),
                    fileType,
                    Optional.ofNullable(fileIDStr),
                    Optional.ofNullable(encryptionController));
            Message response = downloadFileService.executeAndGetResponse();
            if (response == FileMessage.SUCCESS) {
              ctx.header("Content-Type", downloadFileService.getContentType());
              ctx.result(downloadFileService.getInputStream());
            } else {
              ctx.result(response.toResponseString());
            }
          } else {
            ctx.result(UserMessage.CROSS_ORG_ACTION_DENIED.toResponseString());
          }
        }
      };

  /*
  REQUIRES JSON Body with:
    - "fileType": String giving File Type (see FileType enum)
    - "fileId": String giving id of file to be deleted
    - OPTIONAL- "targetUser": User whose file you want to access.
        - If left empty, defaults to original username.
  */
  public Handler fileDelete =
      ctx -> {
        String username;
        String orgName;
        UserType userType;
        JSONObject req = new JSONObject(ctx.body());
        Optional<User> maybeTargetUser =
            GetUserInfoService.getUserFromRequest(this.userDao, ctx.body());
        if (maybeTargetUser.isEmpty() && req.has("targetUser")) {
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          boolean orgFlag;
          if (maybeTargetUser.isPresent() && req.has("targetUser")) {
            log.info("Target user found");
            username = maybeTargetUser.get().getUsername();
            orgName = maybeTargetUser.get().getOrganization();
            userType = maybeTargetUser.get().getUserType();
            orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
          } else {
            username = ctx.sessionAttribute("username");
            orgName = ctx.sessionAttribute("orgName");
            userType = ctx.sessionAttribute("privilegeLevel");
            // User is in same org as themselves
            orgFlag = true;
          }

          if (orgFlag) {
            String fileIDStr = req.getString("fileId");
            String fileTypeStr = req.getString("fileType");
            FileType fileType = FileType.createFromString(fileTypeStr);

            DeleteFileService deleteFileService =
                new DeleteFileService(
                    fileDao, activityDao, username, orgName, userType, fileType, fileIDStr);
            ctx.result(deleteFileService.executeAndGetResponse().toResponseString());
          } else {
            ctx.result(UserMessage.CROSS_ORG_ACTION_DENIED.toResponseString());
          }
        }
      };

  /*
  REQUIRES JSON Body:
    - Body
      - "fileType": String giving File Type (See FileType enum)
      - if "fileType" is "FORM_PDF"
        - "annotated": boolean for retrieving EITHER annotated forms OR unannotated forms
      - OPTIONAL- "targetUser": User whose file you want to access.
        - If left empty, defaults to original username.
  */
  public Handler getFiles =
      ctx -> {
        log.info("Starting pdfGetDocuments");
        String username;
        String orgName;
        UserType userType;
        String reqBody = ctx.body();
        JSONObject req = new JSONObject(reqBody);
        JSONObject responseJSON;
        System.out.println("REQ: " + req);
        Optional<User> maybeTargetUser =
            GetUserInfoService.getUserFromRequest(this.userDao, reqBody);
        System.out.println("filetype: " + req.getString("fileType"));
        if (maybeTargetUser.isEmpty() && req.has("targetUser")) {
          log.info("Target User not Found");
          responseJSON = UserMessage.USER_NOT_FOUND.toJSON();
        } else {
          boolean orgFlag;
          if (maybeTargetUser.isPresent() && req.has("targetUser")) {
            log.info("Target user found");
            username = maybeTargetUser.get().getUsername();
            orgName = maybeTargetUser.get().getOrganization();
            userType = maybeTargetUser.get().getUserType();
            orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
          } else {
            username = ctx.sessionAttribute("username");
            orgName = ctx.sessionAttribute("orgName");
            userType = ctx.sessionAttribute("privilegeLevel");
            orgFlag = true;
          }
          if (orgFlag) {
            FileType fileType = FileType.createFromString(req.getString("fileType"));
            boolean annotated = false;
            if (fileType == FileType.FORM) {
              annotated = Objects.requireNonNull(req.getBoolean("annotated"));
            }
            GetFilesInformationService getFilesInformationService =
                new GetFilesInformationService(
                    fileDao, activityDao, username, orgName, userType, fileType, annotated);
            Message response = getFilesInformationService.executeAndGetResponse();
            responseJSON = response.toJSON();

            if (response == FileMessage.SUCCESS) {
              responseJSON.put("documents", getFilesInformationService.getFilesJSON());
            }
          } else {
            responseJSON = UserMessage.CROSS_ORG_ACTION_DENIED.toJSON();
          }
        }
        ctx.result(responseJSON.toString());
      };

  /*
  REQUIRES JSON Body:
    - "applicationId": String giving id of application to get questions from
   */
  public Handler getApplicationQuestions =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String applicationId = req.getString("applicationId");
        String username = ctx.sessionAttribute("username");
        String organizationName = ctx.sessionAttribute("orgName");
        UserType privilegeLevel = ctx.sessionAttribute("privilegeLevel");
        DownloadFileService downloadFileService =
            new DownloadFileService(
                fileDao,
                username,
                Optional.ofNullable(organizationName),
                Optional.ofNullable(privilegeLevel),
                FileType.FORM,
                Optional.ofNullable(applicationId),
                Optional.ofNullable(encryptionController));
        Message responseDownload = downloadFileService.executeAndGetResponse();
        if (responseDownload == FileMessage.SUCCESS) {
          InputStream inputStream = downloadFileService.getInputStream();
          GetQuestionsPDFFileService getQuestionsPDFFileService =
              new GetQuestionsPDFFileService(userDao, privilegeLevel, username, inputStream);
          Message response = getQuestionsPDFFileService.executeAndGetResponse();
          if (response == FileMessage.SUCCESS) {
            JSONObject information = getQuestionsPDFFileService.getApplicationInformation();
            ctx.result(mergeJSON(response.toJSON(), information).toString());
          } else {
            ctx.result(response.toJSON().toString());
          }
        } else {
          ctx.result(responseDownload.toResponseString());
        }
      };

  /*
  REQUIRES JSON Body:
    - "applicationId": String giving id of application to fill out
    - "formAnswers": JSON with format
      {
        "Field1 Name": Field 1's Answer,
        "Field2 Name": Field 2's Answer,
        ...
      }
   */
  public Handler fillPDFForm =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String applicationId = req.getString("applicationId");
        String username = ctx.sessionAttribute("username");
        String organizationName = ctx.sessionAttribute("orgName");
        UserType privilegeLevel = ctx.sessionAttribute("privilegeLevel");
        JSONObject formAnswers = req.getJSONObject("formAnswers");

        DownloadFileService downloadFileService =
            new DownloadFileService(
                fileDao,
                username,
                Optional.ofNullable(organizationName),
                Optional.ofNullable(privilegeLevel),
                FileType.FORM,
                Optional.ofNullable(applicationId),
                Optional.ofNullable(encryptionController));
        Message responseDownload = downloadFileService.executeAndGetResponse();
        if (responseDownload == FileMessage.SUCCESS) {
          InputStream inputStream = downloadFileService.getInputStream();
          FillPDFFileService fillPDFFileService =
              new FillPDFFileService(privilegeLevel, inputStream, formAnswers);
          Message response = fillPDFFileService.executeAndGetResponse();
          if (response == FileMessage.SUCCESS) {
            ctx.header("Content-Type", "application/pdf");
            ctx.result(fillPDFFileService.getCompletedForm());
          } else {
            ctx.result(response.toResponseString());
          }
        } else {
          ctx.result(responseDownload.toResponseString());
        }
      };

  public static String getPDFTitle(String fileName, PDDocument pdfDocument) {
    String title = fileName;
    pdfDocument.setAllSecurityToBeRemoved(true);
    String titleTmp = pdfDocument.getDocumentInformation().getTitle();
    if (titleTmp != null) {
      title = titleTmp;
    }
    return title;
  }
}
