package File;

import static User.UserController.mergeJSON;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import Config.Message;
import Database.Activity.ActivityDao;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.Packet.PacketDao;
import Database.User.UserDao;
import File.Jobs.GetWeeklyUploadedIdsJob;
import File.Services.*;
import Packet.Packet;
import Packet.PacketMessage;
import Packet.PacketPart;
import Packet.Services.RenderPacketPdfService;
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
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.bson.types.ObjectId;
import org.json.JSONObject;

@Slf4j
public class FileController {

  private static void setFileOrganizationId(
      File file, io.javalin.http.Context ctx, Optional<User> targetOrgUser) {
    if (targetOrgUser.isPresent() && targetOrgUser.get().getOrganizationId() != null) {
      file.setOrganizationId(targetOrgUser.get().getOrganizationId());
      return;
    }
    SessionOrganizationId.fromContext(ctx).ifPresent(file::setOrganizationId);
  }

  private UserDao userDao;
  private FileDao fileDao;
  private ActivityDao activityDao;
  private FormDao formDao;
  private PacketDao packetDao;
  private EncryptionController encryptionController;

  public FileController(
      MongoDatabase db,
      UserDao userDao,
      FileDao fileDao,
      ActivityDao activityDao,
      FormDao formDao,
      PacketDao packetDao,
      EncryptionController encryptionController) {
    this.userDao = userDao;
    this.fileDao = fileDao;
    this.activityDao = activityDao;
    this.formDao = formDao;
    this.packetDao = packetDao;
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
        String usernameOfInvoker;
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
          usernameOfInvoker = ctx.sessionAttribute("username");
          if (req != null && req.has("targetUser") && maybeTargetUser.isPresent()) {
            log.info("Target user found, setting parameters.");
            User target = maybeTargetUser.get();
            username = target.getUsername();
            organizationName = target.getOrganization();
            privilegeLevel = target.getUserType();
            Optional<ObjectId> sessionOid = SessionOrganizationId.fromContext(ctx);
            if (sessionOid.isPresent() && target.getOrganizationId() != null) {
              orgFlag = sessionOid.get().equals(target.getOrganizationId());
            } else {
              orgFlag = organizationName.equals(ctx.sessionAttribute("orgName"));
            }
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
              String customIdCategory = null;
              boolean toSign = false;
              if (ctx.formParam("annotated") != null) {
                annotated = Boolean.parseBoolean(ctx.formParam("annotated"));
              }
              if (ctx.formParam("idCategory") != null) {
                idCategory = IdCategoryType.createFromString(ctx.formParam("idCategory"));
              }
              if (ctx.formParam("customIdCategory") != null) {
                String customIdCategoryRaw = ctx.formParam("customIdCategory");
                if (customIdCategoryRaw != null) {
                  customIdCategory = customIdCategoryRaw.trim();
                  if (customIdCategory.isEmpty()) {
                    customIdCategory = null;
                  }
                }
              }
              if (ctx.formParam("toSign") != null) {
                toSign = Boolean.parseBoolean(ctx.formParam("toSign"));
              }
              if (fileType != FileType.IDENTIFICATION_PDF) {
                customIdCategory = null;
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
                  if (fileType == FileType.IDENTIFICATION_PDF) {
                    if (idCategory == IdCategoryType.NONE) {
                      response = FileMessage.INVALID_PARAMETER;
                      break;
                    }
                    if (idCategory == IdCategoryType.OTHER && customIdCategory == null) {
                      response = FileMessage.INVALID_PARAMETER;
                      break;
                    }
                    if (idCategory != IdCategoryType.OTHER) {
                      customIdCategory = null;
                    }
                  } else {
                    customIdCategory = null;
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
                  fileToUpload.setCustomIdCategory(customIdCategory);
                  setFileOrganizationId(fileToUpload, ctx, maybeTargetUser);
                  UploadFileService uploadService =
                      new UploadFileService(
                          fileDao,
                          activityDao,
                          usernameOfInvoker,
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
                case ORG_DOCUMENT:
                  log.info("Got Org Document to upload via standard fileUpload handler!");
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
                  fileToUpload.setCustomIdCategory(customIdCategory);
                  setFileOrganizationId(fileToUpload, ctx, maybeTargetUser);
                  uploadService =
                      new UploadFileService(
                          fileDao,
                          activityDao,
                          usernameOfInvoker,
                          fileToUpload,
                          Optional.ofNullable(privilegeLevel),
                          Optional.ofNullable(fileId),
                          toSign,
                          Optional.empty(),
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
                  fileToUpload.setCustomIdCategory(customIdCategory);
                  uploadService =
                      new UploadFileService(
                          fileDao,
                          activityDao,
                          usernameOfInvoker,
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
                  fileToUpload.setCustomIdCategory(customIdCategory);
                  uploadService =
                      new UploadFileService(
                          fileDao,
                          activityDao,
                          usernameOfInvoker,
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
        String usernameOfInvoker;
        String orgName;
        UserType userType;
        JSONObject req = new JSONObject(ctx.body());
        Optional<User> maybeTargetUser =
            GetUserInfoService.getUserFromRequest(this.userDao, ctx.body());
        if (maybeTargetUser.isEmpty() && req.has("targetUser")) {
          log.info("Target User not Found");
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          usernameOfInvoker = ctx.sessionAttribute("username");
          boolean orgFlag;
          if (maybeTargetUser.isPresent() && req.has("targetUser")) {
            log.info("Target user found");
            User target = maybeTargetUser.get();
            username = target.getUsername();
            orgName = target.getOrganization();
            userType = target.getUserType();
            Optional<ObjectId> sessionOid = SessionOrganizationId.fromContext(ctx);
            if (sessionOid.isPresent() && target.getOrganizationId() != null) {
              orgFlag = sessionOid.get().equals(target.getOrganizationId());
            } else {
              orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
            }
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
                    activityDao,
                    usernameOfInvoker,
                    username,
                    Optional.ofNullable(orgName),
                    Optional.ofNullable(userType),
                    fileType,
                    Optional.ofNullable(fileIDStr),
                    Optional.ofNullable(encryptionController),
                    SessionOrganizationId.fromContext(ctx),
                    formDao);
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
        String usernameOfInvoker;
        String orgName;
        UserType userType;
        JSONObject req = new JSONObject(ctx.body());
        Optional<User> maybeTargetUser =
            GetUserInfoService.getUserFromRequest(this.userDao, ctx.body());
        if (maybeTargetUser.isEmpty() && req.has("targetUser")) {
          ctx.result(UserMessage.USER_NOT_FOUND.toJSON().toString());
        } else {
          boolean orgFlag;
          usernameOfInvoker = ctx.sessionAttribute("username");
          if (maybeTargetUser.isPresent() && req.has("targetUser")) {
            log.info("Target user found");
            User target = maybeTargetUser.get();
            username = target.getUsername();
            orgName = target.getOrganization();
            userType = ctx.sessionAttribute("privilegeLevel");
            Optional<ObjectId> sessionOid = SessionOrganizationId.fromContext(ctx);
            if (sessionOid.isPresent() && target.getOrganizationId() != null) {
              orgFlag = sessionOid.get().equals(target.getOrganizationId());
            } else {
              orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
            }
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
                    fileDao,
                    activityDao,
                    usernameOfInvoker,
                    username,
                    orgName,
                    userType,
                    fileType,
                    fileIDStr,
                    SessionOrganizationId.fromContext(ctx));
            ctx.result(deleteFileService.executeAndGetResponse().toResponseString());
          } else {
            ctx.result(UserMessage.CROSS_ORG_ACTION_DENIED.toResponseString());
          }
        }
      };

  /*
  REQUIRES JSON Body with:
    - "fileId": String giving id of file to be renamed
    - "newFilename": String giving the new filename
    - OPTIONAL- "targetUser": User whose file you want to access.
        - If left empty, defaults to original username.
  */
  public Handler fileRename =
      ctx -> {
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
            User target = maybeTargetUser.get();
            orgName = target.getOrganization();
            userType = target.getUserType();
            Optional<ObjectId> sessionOid = SessionOrganizationId.fromContext(ctx);
            if (sessionOid.isPresent() && target.getOrganizationId() != null) {
              orgFlag = sessionOid.get().equals(target.getOrganizationId());
            } else {
              orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
            }
          } else {
            orgName = ctx.sessionAttribute("orgName");
            userType = ctx.sessionAttribute("privilegeLevel");
            orgFlag = true;
          }

          if (orgFlag) {
            String fileIDStr = req.getString("fileId");
            String newFilename = req.getString("newFilename");

            RenameFileService renameFileService =
                new RenameFileService(
                    fileDao,
                    fileIDStr,
                    newFilename,
                    orgName,
                    userType,
                    SessionOrganizationId.fromContext(ctx));
            ctx.result(renameFileService.executeAndGetResponse().toResponseString());
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
            User target = maybeTargetUser.get();
            username = target.getUsername();
            orgName = target.getOrganization();
            userType = target.getUserType();
            Optional<ObjectId> sessionOid = SessionOrganizationId.fromContext(ctx);
            if (sessionOid.isPresent() && target.getOrganizationId() != null) {
              orgFlag = sessionOid.get().equals(target.getOrganizationId());
            } else {
              orgFlag = orgName.equals(ctx.sessionAttribute("orgName"));
            }
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
            ObjectId organizationIdForQuery =
                maybeTargetUser.isPresent() && req.has("targetUser")
                    ? maybeTargetUser.get().getOrganizationId()
                    : SessionOrganizationId.fromContext(ctx).orElse(null);
            GetFilesInformationService getFilesInformationService =
                new GetFilesInformationService(
                    fileDao,
                    username,
                    orgName,
                    organizationIdForQuery,
                    userType,
                    fileType,
                    annotated,
                    formDao);
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

  private Optional<File> getApplicationFileForPacket(
      JSONObject req, String orgName, Optional<ObjectId> sessionOrganizationId) {
    if (!req.has("applicationId")) {
      return Optional.empty();
    }
    String applicationId = req.getString("applicationId");
    if (!ObjectId.isValid(applicationId)) {
      return Optional.empty();
    }

    Optional<File> applicationFileOpt = fileDao.get(new ObjectId(applicationId));
    if (applicationFileOpt.isEmpty()) {
      return Optional.empty();
    }
    File applicationFile = applicationFileOpt.get();
    if (applicationFile.getFileType() != FileType.APPLICATION_PDF) {
      return Optional.empty();
    }
    if (sessionOrganizationId.isPresent() && applicationFile.getOrganizationId() != null) {
      if (!sessionOrganizationId.get().equals(applicationFile.getOrganizationId())) {
        return Optional.empty();
      }
      return Optional.of(applicationFile);
    }
    if (!Objects.equals(orgName, applicationFile.getOrganizationName())) {
      return Optional.empty();
    }
    return Optional.of(applicationFile);
  }

  private Packet buildLazyPacket(File applicationFile, String username, Optional<ObjectId> sessionOrganizationId) {
    Packet packet =
        new Packet(
            sessionOrganizationId.orElse(applicationFile.getOrganizationId()),
            applicationFile.getId(),
            username);
    List<PacketPart> parts = new ArrayList<>();
    parts.add(new PacketPart(applicationFile.getId(), "APPLICATION_BASE", 0, true));
    packet.setParts(parts);
    return packet;
  }

  private Packet sortAndNormalizeParts(Packet packet) {
    List<PacketPart> sourceParts = packet.getParts() == null ? new ArrayList<>() : packet.getParts();
    List<PacketPart> sorted = new ArrayList<>(sourceParts);
    sorted.sort(Comparator.comparingInt(PacketPart::getOrder));
    for (int i = 0; i < sorted.size(); i += 1) {
      sorted.get(i).setOrder(i);
    }
    packet.setParts(sorted);
    packet.setUpdatedAt(new Date());
    return packet;
  }

  private boolean isSameOrganization(
      File file, String orgName, Optional<ObjectId> sessionOrganizationId) {
    if (sessionOrganizationId.isPresent() && file.getOrganizationId() != null) {
      return sessionOrganizationId.get().equals(file.getOrganizationId());
    }
    return Objects.equals(orgName, file.getOrganizationName());
  }

  private boolean canEditApplicationAttachment(UserType privilegeLevel) {
    return privilegeLevel == UserType.Worker
        || privilegeLevel == UserType.Admin
        || privilegeLevel == UserType.Director;
  }

  private Optional<File> findExistingAttachmentClone(
      File applicationFile, ObjectId sourceOrgDocumentId, String orgName, Optional<ObjectId> sessionOrganizationId) {
    List<File> existingClones =
        fileDao.getAll(
            and(
                eq("fileType", FileType.ORG_DOCUMENT.toString()),
                eq("applicationScopedAttachment", true),
                eq("attachedApplicationId", applicationFile.getId()),
                eq("sourceOrgDocumentId", sourceOrgDocumentId)));
    for (File clone : existingClones) {
      if (isSameOrganization(clone, orgName, sessionOrganizationId)) {
        return Optional.of(clone);
      }
    }
    return Optional.empty();
  }

  private Optional<File> cloneAttachmentForApplication(
      File sourceFile, File applicationFile, String orgName, Optional<ObjectId> sessionOrganizationId) {
    Optional<InputStream> sourceStreamOpt = fileDao.getStream(sourceFile.getId());
    if (sourceStreamOpt.isEmpty()) {
      return Optional.empty();
    }
    File clonedFile =
        new File(
            applicationFile.getUsername(),
            new Date(),
            sourceStreamOpt.get(),
            FileType.ORG_DOCUMENT,
            sourceFile.getIdCategory(),
            sourceFile.getFilename(),
            applicationFile.getOrganizationName(),
            sourceFile.isAnnotated(),
            sourceFile.getContentType());
    clonedFile.setCustomIdCategory(sourceFile.getCustomIdCategory());
    clonedFile.setApplicationScopedAttachment(true);
    clonedFile.setAttachedApplicationId(applicationFile.getId());
    clonedFile.setSourceOrgDocumentId(sourceFile.getId());
    if (sourceFile.getOrganizationId() != null) {
      clonedFile.setOrganizationId(sourceFile.getOrganizationId());
    } else {
      sessionOrganizationId.ifPresent(clonedFile::setOrganizationId);
    }
    if (!isSameOrganization(clonedFile, orgName, sessionOrganizationId)) {
      return Optional.empty();
    }
    fileDao.save(clonedFile);
    return Optional.of(clonedFile);
  }

  private boolean refreshAttachmentCloneFromSource(
      File sourceFile, File existingClone, String orgName, Optional<ObjectId> sessionOrganizationId) {
    Optional<InputStream> sourceStreamOpt = fileDao.getStream(sourceFile.getId());
    if (sourceStreamOpt.isEmpty()) {
      return false;
    }
    if (!isSameOrganization(existingClone, orgName, sessionOrganizationId)) {
      return false;
    }
    existingClone.setFileStream(sourceStreamOpt.get());
    existingClone.setFilename(sourceFile.getFilename());
    existingClone.setContentType(sourceFile.getContentType());
    existingClone.setIdCategory(sourceFile.getIdCategory());
    existingClone.setCustomIdCategory(sourceFile.getCustomIdCategory());
    existingClone.setAnnotated(sourceFile.isAnnotated());
    existingClone.setUploadedAt(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
    fileDao.update(existingClone);
    return true;
  }

  private JSONObject packetToJsonWithAttachmentMetadata(Packet packet) {
    JSONObject packetJson = packet.toJson();
    org.json.JSONArray partsJson = packetJson.getJSONArray("parts");
    for (int i = 0; i < partsJson.length(); i += 1) {
      JSONObject partJson = partsJson.getJSONObject(i);
      if (!"ORG_ATTACHMENT".equals(partJson.optString("partType"))) {
        continue;
      }
      String partFileId = partJson.optString("fileId", "");
      if (!ObjectId.isValid(partFileId)) {
        continue;
      }
      Optional<File> partFileOpt = fileDao.get(new ObjectId(partFileId));
      if (partFileOpt.isPresent()) {
        File partFile = partFileOpt.get();
        if (partFile.isApplicationScopedAttachment() && partFile.getSourceOrgDocumentId() != null) {
          partJson.put("sourceFileId", partFile.getSourceOrgDocumentId().toString());
        }
      }
    }
    return packetJson;
  }

  public Handler getPacketForApplication =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        String orgName = ctx.sessionAttribute("orgName");
        Optional<ObjectId> sessionOrganizationId = SessionOrganizationId.fromContext(ctx);
        Optional<File> applicationFileOpt =
            getApplicationFileForPacket(req, orgName, sessionOrganizationId);
        if (applicationFileOpt.isEmpty()) {
          ctx.result(PacketMessage.NO_SUCH_FILE.toResponseString());
          return;
        }

        File applicationFile = applicationFileOpt.get();
        if (applicationFile.getPacketId() == null) {
          JSONObject response = PacketMessage.SUCCESS.toJSON();
          response.put("packet", JSONObject.NULL);
          ctx.result(response.toString());
          return;
        }

        Optional<Packet> packetOpt = packetDao.get(applicationFile.getPacketId());
        if (packetOpt.isEmpty()) {
          JSONObject response = PacketMessage.SUCCESS.toJSON();
          response.put("packet", JSONObject.NULL);
          ctx.result(response.toString());
          return;
        }

        JSONObject response = PacketMessage.SUCCESS.toJSON();
        response.put("packet", packetToJsonWithAttachmentMetadata(sortAndNormalizeParts(packetOpt.get())));
        ctx.result(response.toString());
      };

  public Handler attachPacketPart =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        if (!req.has("applicationId") || !req.has("fileId")) {
          ctx.result(PacketMessage.INVALID_PARAMETER.toResponseString());
          return;
        }
        if (!ObjectId.isValid(req.getString("fileId"))) {
          ctx.result(PacketMessage.INVALID_PARAMETER.toResponseString());
          return;
        }

        String username = ctx.sessionAttribute("username");
        String orgName = ctx.sessionAttribute("orgName");
        Optional<ObjectId> sessionOrganizationId = SessionOrganizationId.fromContext(ctx);
        Optional<File> applicationFileOpt =
            getApplicationFileForPacket(req, orgName, sessionOrganizationId);
        if (applicationFileOpt.isEmpty()) {
          ctx.result(PacketMessage.NO_SUCH_FILE.toResponseString());
          return;
        }

        ObjectId requestedFileId = new ObjectId(req.getString("fileId"));
        Optional<File> partFileOpt = fileDao.get(requestedFileId);
        if (partFileOpt.isEmpty() || partFileOpt.get().getFileType() != FileType.ORG_DOCUMENT) {
          ctx.result(PacketMessage.INVALID_FILE_TYPE.toResponseString());
          return;
        }
        File partFile = partFileOpt.get();
        if (!isSameOrganization(partFile, orgName, sessionOrganizationId)) {
          ctx.result(PacketMessage.INSUFFICIENT_PRIVILEGE.toResponseString());
          return;
        }

        File applicationFile = applicationFileOpt.get();
        Packet packet;
        if (applicationFile.getPacketId() == null) {
          packet = buildLazyPacket(applicationFile, username, sessionOrganizationId);
        } else {
          Optional<Packet> existingPacketOpt = packetDao.get(applicationFile.getPacketId());
          packet = existingPacketOpt.orElseGet(() -> buildLazyPacket(applicationFile, username, sessionOrganizationId));
        }

        File attachmentFileForPacket;
        if (partFile.isApplicationScopedAttachment()) {
          if (partFile.getAttachedApplicationId() == null
              || !partFile.getAttachedApplicationId().equals(applicationFile.getId())) {
            ctx.result(PacketMessage.INSUFFICIENT_PRIVILEGE.toResponseString());
            return;
          }
          attachmentFileForPacket = partFile;
        } else {
          Optional<File> existingCloneOpt =
              findExistingAttachmentClone(applicationFile, partFile.getId(), orgName, sessionOrganizationId);
          if (existingCloneOpt.isPresent()) {
            attachmentFileForPacket = existingCloneOpt.get();
            if (!refreshAttachmentCloneFromSource(
                partFile, attachmentFileForPacket, orgName, sessionOrganizationId)) {
              ctx.result(PacketMessage.SERVER_ERROR.toResponseString());
              return;
            }
          } else {
            Optional<File> clonedFileOpt =
                cloneAttachmentForApplication(partFile, applicationFile, orgName, sessionOrganizationId);
            if (clonedFileOpt.isEmpty()) {
              ctx.result(PacketMessage.SERVER_ERROR.toResponseString());
              return;
            }
            attachmentFileForPacket = clonedFileOpt.get();
          }
        }

        ObjectId fileIdToAttach = attachmentFileForPacket.getId();
        boolean alreadyAttached =
            packet.getParts().stream().anyMatch(part -> part.getFileId().equals(fileIdToAttach));
        if (!alreadyAttached) {
          int nextOrder = packet.getParts().stream().mapToInt(PacketPart::getOrder).max().orElse(-1) + 1;
          packet.getParts().add(new PacketPart(fileIdToAttach, "ORG_ATTACHMENT", nextOrder, true));
        }

        packet = sortAndNormalizeParts(packet);
        if (applicationFile.getPacketId() == null || packetDao.get(packet.getId()).isEmpty()) {
          packetDao.save(packet);
        } else {
          packetDao.update(packet);
        }

        applicationFile.setPacketId(packet.getId());
        fileDao.update(applicationFile);

        JSONObject response = PacketMessage.SUCCESS.toJSON();
        response.put("packet", packetToJsonWithAttachmentMetadata(packet));
        response.put("alreadyAttached", alreadyAttached);
        response.put("attachedFileId", fileIdToAttach.toString());
        ctx.result(response.toString());
      };

  public Handler detachPacketPart =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        if (!req.has("applicationId") || !req.has("fileId")) {
          ctx.result(PacketMessage.INVALID_PARAMETER.toResponseString());
          return;
        }
        String orgName = ctx.sessionAttribute("orgName");
        Optional<ObjectId> sessionOrganizationId = SessionOrganizationId.fromContext(ctx);
        Optional<File> applicationFileOpt =
            getApplicationFileForPacket(req, orgName, sessionOrganizationId);
        if (applicationFileOpt.isEmpty()) {
          ctx.result(PacketMessage.NO_SUCH_FILE.toResponseString());
          return;
        }
        File applicationFile = applicationFileOpt.get();
        if (applicationFile.getPacketId() == null) {
          JSONObject response = PacketMessage.SUCCESS.toJSON();
          response.put("packet", JSONObject.NULL);
          ctx.result(response.toString());
          return;
        }

        Optional<Packet> packetOpt = packetDao.get(applicationFile.getPacketId());
        if (packetOpt.isEmpty()) {
          JSONObject response = PacketMessage.SUCCESS.toJSON();
          response.put("packet", JSONObject.NULL);
          ctx.result(response.toString());
          return;
        }

        Packet packet = packetOpt.get();
        String fileIdToDetach = req.getString("fileId");
        packet
            .getParts()
            .removeIf(
                part ->
                    part.getFileId().toString().equals(fileIdToDetach)
                        && !"APPLICATION_BASE".equals(part.getPartType()));

        packet = sortAndNormalizeParts(packet);
        packetDao.update(packet);
        JSONObject response = PacketMessage.SUCCESS.toJSON();
        response.put("packet", packetToJsonWithAttachmentMetadata(packet));
        ctx.result(response.toString());
      };

  public Handler reorderPacketParts =
      ctx -> {
        JSONObject req = new JSONObject(ctx.body());
        if (!req.has("applicationId") || !req.has("orderedFileIds")) {
          ctx.result(PacketMessage.INVALID_PARAMETER.toResponseString());
          return;
        }

        String orgName = ctx.sessionAttribute("orgName");
        Optional<ObjectId> sessionOrganizationId = SessionOrganizationId.fromContext(ctx);
        Optional<File> applicationFileOpt =
            getApplicationFileForPacket(req, orgName, sessionOrganizationId);
        if (applicationFileOpt.isEmpty()) {
          ctx.result(PacketMessage.NO_SUCH_FILE.toResponseString());
          return;
        }

        File applicationFile = applicationFileOpt.get();
        if (applicationFile.getPacketId() == null) {
          ctx.result(PacketMessage.NO_SUCH_PACKET.toResponseString());
          return;
        }

        Optional<Packet> packetOpt = packetDao.get(applicationFile.getPacketId());
        if (packetOpt.isEmpty()) {
          ctx.result(PacketMessage.NO_SUCH_PACKET.toResponseString());
          return;
        }

        Packet packet = packetOpt.get();
        org.json.JSONArray orderedFileIds = req.getJSONArray("orderedFileIds");
        List<PacketPart> parts = new ArrayList<>(packet.getParts());
        List<PacketPart> baseParts = new ArrayList<>();
        List<PacketPart> attachmentParts = new ArrayList<>();
        for (PacketPart part : parts) {
          if ("APPLICATION_BASE".equals(part.getPartType())) {
            baseParts.add(part);
          } else {
            attachmentParts.add(part);
          }
        }

        Set<String> requested = new HashSet<>();
        for (int i = 0; i < orderedFileIds.length(); i += 1) {
          requested.add(orderedFileIds.getString(i));
        }
        Set<String> existing =
            attachmentParts.stream().map(part -> part.getFileId().toString()).collect(java.util.stream.Collectors.toSet());
        if (!requested.equals(existing)) {
          ctx.result(PacketMessage.INVALID_PARAMETER.toResponseString());
          return;
        }

        List<PacketPart> reordered = new ArrayList<>();
        reordered.addAll(baseParts);
        int order = baseParts.size();
        for (int i = 0; i < orderedFileIds.length(); i += 1) {
          String fileId = orderedFileIds.getString(i);
          for (PacketPart part : attachmentParts) {
            if (part.getFileId().toString().equals(fileId)) {
              part.setOrder(order);
              reordered.add(part);
              order += 1;
              break;
            }
          }
        }
        packet.setParts(reordered);
        packet = sortAndNormalizeParts(packet);
        packetDao.update(packet);

        JSONObject response = PacketMessage.SUCCESS.toJSON();
        response.put("packet", packetToJsonWithAttachmentMetadata(packet));
        ctx.result(response.toString());
      };

  public Handler updateApplicationAttachmentPdf =
      ctx -> {
        UploadedFile uploadedAttachment = ctx.uploadedFile("file");
        String applicationId = ctx.formParam("applicationId");
        String attachmentFileId = ctx.formParam("fileId");
        UserType privilegeLevel = ctx.sessionAttribute("privilegeLevel");
        String orgName = ctx.sessionAttribute("orgName");
        Optional<ObjectId> sessionOrganizationId = SessionOrganizationId.fromContext(ctx);

        if (uploadedAttachment == null
            || applicationId == null
            || attachmentFileId == null
            || !ObjectId.isValid(applicationId)
            || !ObjectId.isValid(attachmentFileId)
            || privilegeLevel == null) {
          ctx.result(PacketMessage.INVALID_PARAMETER.toResponseString());
          return;
        }

        if (!canEditApplicationAttachment(privilegeLevel)) {
          ctx.result(PacketMessage.INSUFFICIENT_PRIVILEGE.toResponseString());
          return;
        }

        JSONObject requestJson = new JSONObject().put("applicationId", applicationId);
        Optional<File> applicationFileOpt =
            getApplicationFileForPacket(requestJson, orgName, sessionOrganizationId);
        if (applicationFileOpt.isEmpty()) {
          ctx.result(PacketMessage.NO_SUCH_FILE.toResponseString());
          return;
        }
        File applicationFile = applicationFileOpt.get();
        if (applicationFile.getPacketId() == null) {
          ctx.result(PacketMessage.NO_SUCH_PACKET.toResponseString());
          return;
        }

        Optional<Packet> packetOpt = packetDao.get(applicationFile.getPacketId());
        if (packetOpt.isEmpty()) {
          ctx.result(PacketMessage.NO_SUCH_PACKET.toResponseString());
          return;
        }

        ObjectId attachmentId = new ObjectId(attachmentFileId);
        Packet packet = packetOpt.get();
        boolean packetContainsAttachment =
            packet.getParts().stream()
                .anyMatch(
                    part ->
                        "ORG_ATTACHMENT".equals(part.getPartType()) && attachmentId.equals(part.getFileId()));
        if (!packetContainsAttachment) {
          ctx.result(PacketMessage.NO_SUCH_FILE.toResponseString());
          return;
        }

        Optional<File> attachmentFileOpt = fileDao.get(attachmentId);
        if (attachmentFileOpt.isEmpty()) {
          ctx.result(PacketMessage.NO_SUCH_FILE.toResponseString());
          return;
        }
        File attachmentFile = attachmentFileOpt.get();
        if (attachmentFile.getFileType() != FileType.ORG_DOCUMENT
            || !attachmentFile.isApplicationScopedAttachment()
            || attachmentFile.getAttachedApplicationId() == null
            || !attachmentFile.getAttachedApplicationId().equals(applicationFile.getId())
            || !isSameOrganization(attachmentFile, orgName, sessionOrganizationId)) {
          ctx.result(PacketMessage.INSUFFICIENT_PRIVILEGE.toResponseString());
          return;
        }

        if (encryptionController == null) {
          ctx.result(PacketMessage.SERVER_ERROR.toResponseString());
          return;
        }
        try {
          String encryptionContext =
              attachmentFile.getOrganizationId() != null
                  ? Security.OrganizationCryptoAad.fromOrganizationId(attachmentFile.getOrganizationId())
                  : attachmentFile.getUsername();
          InputStream encryptedStream =
              encryptionController.encryptFile(uploadedAttachment.getContent(), encryptionContext);
          attachmentFile.setFileStream(encryptedStream);
          attachmentFile.setUploadedAt(
              Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
          fileDao.update(attachmentFile);
          JSONObject response = PacketMessage.SUCCESS.toJSON();
          response.put("fileId", attachmentFile.getId().toString());
          ctx.result(response.toString());
        } catch (Exception e) {
          ctx.result(PacketMessage.SERVER_ERROR.toResponseString());
        }
      };

  /**
   * Renders the full application packet (base PDF + enabled attachments) into merged, flattened,
   * normalized PDF bytes suitable for client-side Print/Download. Lob letter mail uploads these
   * same bytes and sets {@code address_placement=insert_blank_page} so Lob prepends their address
   * sheet at print time — Print/Download and the mailed PDF payload stay aligned.
   *
   * <p>Multipart body:
   *
   * <ul>
   *   <li>{@code applicationId} (form param, required): the application File's id.
   *   <li>{@code mainPdf} (file, optional): current bytes of the main application PDF from the
   *       client-side viewer. When provided, these bytes replace the stored APPLICATION_BASE
   *       part -- this lets the user print/download their in-progress in-viewer edits (pdf.js
   *       form widget text + embedded signatures) without having to press Save first. The
   *       uploaded bytes are NOT persisted; the override applies only to this render call.
   * </ul>
   *
   * <p>Response: {@code application/pdf} bytes on success, or a JSON error body with the
   * appropriate HTTP status on failure.
   */
  public Handler renderApplicationPacket =
      ctx -> {
        String applicationId = ctx.formParam("applicationId");
        if (applicationId == null || !ObjectId.isValid(applicationId)) {
          ctx.status(400).result(PacketMessage.INVALID_PARAMETER.toResponseString());
          return;
        }

        String orgName = ctx.sessionAttribute("orgName");
        Optional<ObjectId> sessionOrganizationId = SessionOrganizationId.fromContext(ctx);
        JSONObject requestJson = new JSONObject().put("applicationId", applicationId);
        Optional<File> applicationFileOpt =
            getApplicationFileForPacket(requestJson, orgName, sessionOrganizationId);
        if (applicationFileOpt.isEmpty()) {
          ctx.status(404).result(PacketMessage.NO_SUCH_FILE.toResponseString());
          return;
        }
        File applicationFile = applicationFileOpt.get();

        Packet packet = null;
        if (applicationFile.getPacketId() != null) {
          packet = packetDao.get(applicationFile.getPacketId()).orElse(null);
        }

        byte[] mainPdfOverride = null;
        UploadedFile mainPdfUpload = ctx.uploadedFile("mainPdf");
        if (mainPdfUpload != null) {
          try (InputStream mainStream = mainPdfUpload.getContent()) {
            mainPdfOverride = org.apache.commons.io.IOUtils.toByteArray(mainStream);
          } catch (IOException e) {
            log.warn(
                "render-application-packet: failed to read mainPdf override for application {}: {}",
                applicationId,
                e.getMessage());
            ctx.status(400).result(PacketMessage.INVALID_PARAMETER.toResponseString());
            return;
          }
        }

        if (encryptionController == null) {
          ctx.status(500).result(PacketMessage.SERVER_ERROR.toResponseString());
          return;
        }

        try {
          byte[] renderedBytes =
              RenderPacketPdfService.render(
                  applicationFile,
                  packet,
                  fileDao,
                  encryptionController,
                  applicationFile.getUsername(),
                  mainPdfOverride);
          ctx.header("Content-Type", "application/pdf");
          ctx.header("Content-Disposition", "inline; filename=\"application.pdf\"");
          ctx.result(renderedBytes);
        } catch (IOException e) {
          log.error(
              "render-application-packet failed for applicationId={}: {}",
              applicationId,
              e.getMessage(),
              e);
          ctx.status(500).result(PacketMessage.SERVER_ERROR.toResponseString());
        }
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
                activityDao,
                username,
                username,
                Optional.ofNullable(organizationName),
                Optional.ofNullable(privilegeLevel),
                FileType.FORM,
                Optional.ofNullable(applicationId),
                Optional.ofNullable(encryptionController),
                SessionOrganizationId.fromContext(ctx),
                formDao);
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
                activityDao,
                username,
                username,
                Optional.ofNullable(organizationName),
                Optional.ofNullable(privilegeLevel),
                FileType.FORM,
                Optional.ofNullable(applicationId),
                Optional.ofNullable(encryptionController),
                SessionOrganizationId.fromContext(ctx),
                formDao);
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

  public Handler getWeeklyUploadedIds =
      ctx -> {
        GetWeeklyUploadedIdsJob.run(fileDao);
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
