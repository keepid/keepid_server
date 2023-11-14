package File;

import static User.UserController.mergeJSON;
import static com.mongodb.client.model.Filters.eq;

import Config.Message;
import Database.File.FileDao;
import Database.User.UserDao;
import File.Services.*;
import Security.EncryptionController;
import User.Services.GetUserInfoService;
import User.User;
import User.UserMessage;
import User.UserType;
import com.mongodb.client.MongoCollection;
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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;

@Slf4j
public class FileController {
  private UserDao userDao;
  private FileDao fileDao;
  private EncryptionController encryptionController;

  public FileController(MongoDatabase db, UserDao userDao, FileDao fileDao) {
    this.userDao = userDao;
    this.fileDao = fileDao;
    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {
      e.printStackTrace();
    }
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
        JSONObject req;
        String body = ctx.body();
        try {
          req = new JSONObject(body);
        } catch (JSONException e) {
          req = null;
        }

        User check = GetUserInfoService.getUserFromRequest(this.userDao, body);
        if (req != null && req.has("targetUser") && check == null) {
          log.info("Target user could not be found in the database");
          response = UserMessage.USER_NOT_FOUND;
        } else {
          boolean orgFlag;
          if (req != null && req.has("targetUser") && check != null) {
            log.info("Target user found, setting parameters.");
            username = check.getUsername();
            organizationName = check.getOrganization();
            privilegeLevel = check.getUserType();
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
              String title = null;
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

              switch (fileType) {
                case APPLICATION_PDF:
                case IDENTIFICATION_PDF:
                case FORM:
                  log.info("Got PDF file to upload!");
                  try {
                    InputStream content = file.getContent();
                    PDDocument pdfDocument = Loader.loadPDF(content);
                    title = getPDFTitle(file.getFilename(), pdfDocument);
                    content.reset();
                    pdfDocument.close();
                  } catch (Exception exception) {
                    ctx.result(FileMessage.INVALID_FILE.toResponseString());
                  }

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
                          file.getContent(),
                          fileType,
                          idCategory,
                          file.getFilename(),
                          organizationName,
                          annotated,
                          file.getContentType());
                  UploadFileService uploadService =
                      new UploadFileService(
                          fileDao,
                          fileToUpload,
                          Optional.ofNullable(privilegeLevel),
                          Optional.ofNullable(fileId),
                          toSign,
                          signature == null
                              ? Optional.empty()
                              : Optional.ofNullable(signature.getContent()),
                          Optional.ofNullable(encryptionController));
                  response = uploadService.executeAndGetResponse();
                  break;
                case PROFILE_PICTURE:
                  log.info("Got profile picture to upload!");
                  fileToUpload =
                      new File(
                          username,
                          uploadDate,
                          file.getContent(),
                          fileType,
                          idCategory,
                          file.getFilename(),
                          organizationName,
                          annotated,
                          file.getContentType());
                  uploadService =
                      new UploadFileService(
                          fileDao,
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
                          file.getContent(),
                          fileType,
                          idCategory,
                          file.getFilename(),
                          organizationName,
                          annotated,
                          file.getContentType());
                  uploadService =
                      new UploadFileService(
                          fileDao,
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
        User check = GetUserInfoService.getUserFromRequest(this.userDao, ctx.body());
        if (check == null && req.has("targetUser")) {
          log.info("Target User not Found");
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          boolean orgFlag;
          if (check != null && req.has("targetUser")) {
            log.info("Target user found");
            username = check.getUsername();
            orgName = check.getOrganization();
            userType = check.getUserType();
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
        User check = GetUserInfoService.getUserFromRequest(this.userDao, ctx.body());
        if (check == null && req.has("targetUser")) {
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          boolean orgFlag;
          if (check != null && req.has("targetUser")) {
            log.info("Target user found");
            username = check.getUsername();
            orgName = check.getOrganization();
            userType = check.getUserType();
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
                new DeleteFileService(fileDao, username, orgName, userType, fileType, fileIDStr);
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
        User check = GetUserInfoService.getUserFromRequest(this.userDao, reqBody);
        if (check == null && req.has("targetUser")) {
          log.info("Target User not Found");
          responseJSON = UserMessage.USER_NOT_FOUND.toJSON();
        } else {
          boolean orgFlag;
          if (check != null && req.has("targetUser")) {
            log.info("Target user found");
            username = check.getUsername();
            orgName = check.getOrganization();
            userType = check.getUserType();
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
                    fileDao, username, orgName, userType, fileType, annotated);
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

    public Handler fileMail =
        ctx -> {
            String username;
            UserType userType;
            JSONObject req = new JSONObject(ctx.body());
            username = ctx.sessionAttribute("username");
            userType = ctx.sessionAttribute("privilegeLevel");
            //file ID is still not working. Not used
            String fileIDStr = req.getString("fileId");
            ObjectId fileId = new ObjectId(fileIDStr);
            Optional<File> maybefile = fileDao.get(fileId);
            String description = req.getString("description");

            MailFileService mailFileService =
                    new MailFileService(fileDao, fileId, username, userType, description);
            ctx.result(mailFileService.executeAndGetResponse().toResponseString());
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
