package PDF.Services;

import static com.mongodb.client.model.Filters.eq;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Organization.OrgDao;
import Database.User.UserDao;
import File.File;
import File.FileMessage;
import File.FileType;
import File.IdCategoryType;
import PDF.PDFType;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import PDF.Services.CrudServices.DownloadPDFService;
import PDF.Services.CrudServices.GetFilesInformationPDFService;
import PDF.Services.V2Services.DownloadPDFServiceV2;
import Security.EncryptionController;
import User.User;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class DownloadAndReUploadPdfsService implements Service {
  private EncryptionController encryptionController;
  private MongoDatabase db;
  private FileDao fileDao;
  private UserDao userDao;
  private OrgDao orgDao;
  private Boolean deleteParam;
  private Boolean filterParam;
  private Boolean downloadParam;
  private String backfillType;
  private String downloadStrings;

  public DownloadAndReUploadPdfsService(
      EncryptionController encryptionController,
      MongoDatabase db,
      FileDao fileDao,
      UserDao userDao,
      OrgDao orgDao,
      Boolean deleteParam,
      Boolean filterParam,
      Boolean downloadParam,
      String backfillType,
      String downloadStrings) {
    this.encryptionController = encryptionController;
    this.db = db;
    this.fileDao = fileDao;
    this.userDao = userDao;
    this.orgDao = orgDao;
    this.deleteParam = deleteParam;
    this.filterParam = filterParam;
    this.downloadParam = downloadParam;
    this.backfillType = backfillType;
    this.downloadStrings = downloadStrings;
  }

  @Override
  public Message executeAndGetResponse() throws Exception {
    boolean dangerousDelete = this.deleteParam; // MAKE SURE TO CHANGE THE FILETYPE IN THIS SCRIPT
    boolean testingFilter = this.filterParam; // MAKE SURE TO CHANGE THE FILETYPE IN THIS SCRIPT
    boolean testingDownload = this.downloadParam; // MAKE SURE TO CHANGE THE USER/FILE PARAMS IN THIS SCRIPT
    boolean backfillIdentificationPdfs = this.backfillType.equals("1");
    boolean backfillBlankForms = this.backfillType.equals("2");
    boolean backfillCompletedApps = this.backfillType.equals("3");
    boolean backfillCompletedAppsOption = this.backfillType.equals("4");

    if (dangerousDelete) {
      List<File> fileDaoFiles =
          fileDao.getAll(eq("fileType", FileType.APPLICATION_PDF.toString()));
      System.out.println("Presize: " + fileDaoFiles.size());
      for (File f : fileDaoFiles) {
        this.fileDao.delete(f.getId());
      }
      fileDaoFiles = fileDao.getAll(eq("fileType", FileType.APPLICATION_PDF.toString()));
      System.out.println("Postsize: " + fileDaoFiles.size());
      return PdfMessage.SUCCESS;
    }
    if (testingFilter) {
      GetFilesInformationPDFService getFilesInformationPDFService =
          new GetFilesInformationPDFService(
              db, "username", "orgName", UserType.Developer, PDFType.IDENTIFICATION_DOCUMENT, false);
      JSONArray allFiles = getFilesInformationPDFService.mongodbGetAllFiles();
      Set<String> s = new HashSet<>();
      for (int i = 0; i < allFiles.length(); i++) {
        JSONObject fileJSON = allFiles.getJSONObject(i);
        String filename = fileJSON.get("filename").toString();
        String uploader = fileJSON.get("uploader").toString();
        s.add(filename + "   " + uploader);
        System.out.println(filename + "   " + uploader);
      }
      System.out.println("Orig Size: " + allFiles.length());
      System.out.println("Unique file/name combos: " + s.size());
      List<File> fileDaoFiles =
          fileDao.getAll(eq("fileType", FileType.APPLICATION_PDF.toString()));
      for (File f : fileDaoFiles) {
        if (!backfillCompletedAppsOption || !f.getOrganizationName().equals("Team Keep")) {
          System.out.println(f.toJsonView().toString());
        }
      }
      System.out.println("Filedao size:" + fileDaoFiles.size());
//      List<User> allUsers = userDao.getAll();
//      for (User u : allUsers) {
//        System.out.println(u.getUsername());
//      }
      return PdfMessage.SUCCESS;
    }
    if (testingDownload) {
     String[] dsParams = this.downloadStrings.split(",");
      DownloadPDFServiceV2 service =
          new DownloadPDFServiceV2(
              fileDao,
              null,
              new UserParams()
                  .setUsername(dsParams[0])
                  .setOrganizationName(dsParams[1])
                  .setPrivilegeLevel(UserType.userTypeFromString(dsParams[2])),
              new FileParams()
                  .setFileId(dsParams[3])
                  .setPdfType(PDFTypeV2.createFromString(dsParams[4])),
              encryptionController);
      System.out.println(service.executeAndGetResponse());
      InputStream is = service.getDownloadedInputStream();
      Files.copy(
          is,
          Paths.get(
              "/Users/samuellee/Library/Mobile Documents/com~apple~CloudDocs/Online Downloads/testtest"+dsParams[5]+".pdf"),
          StandardCopyOption.REPLACE_EXISTING);
      return PdfMessage.SUCCESS;
    }
    // Identification Documents
    if (backfillIdentificationPdfs) {
      GetFilesInformationPDFService getFilesInformationPDFService =
          new GetFilesInformationPDFService(
              db,
              "username_doesnt_matter",
              "orgName_doesnt_matter",
              UserType.Developer,
              PDFType.IDENTIFICATION_DOCUMENT,
              false);
      JSONArray allFiles = getFilesInformationPDFService.mongodbGetAllFiles();
      SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
      System.out.println("Starting size: " + allFiles.length());
      for (int i = 0; i < allFiles.length(); i++) {
        JSONObject fileJSON = allFiles.getJSONObject(i);
        String fileId = fileJSON.get("id").toString();
        String filename = fileJSON.get("filename").toString();
        String uploader = fileJSON.get("uploader").toString();
        String uploadDateString = fileJSON.get("uploadDate").toString().substring(0, 10);
        Date uploadDate = dateFormatter.parse(uploadDateString);
        String orgName = fileJSON.get("organizationName").toString();
        IdCategoryType categoryType =
            fileJSON.has("idCategory")
                ? IdCategoryType.createFromString(fileJSON.get("idCategory").toString())
                : null;

        User user = userDao.get(uploader).get();
        DownloadPDFService downloadPDFService =
            new DownloadPDFService(
                db,
                user.getUsername(),
                user.getOrganization(),
                user.getUserType(),
                fileId,
                PDFType.IDENTIFICATION_DOCUMENT,
                encryptionController);
        downloadPDFService.executeAndGetResponse();
        InputStream fileInputStream = downloadPDFService.getInputStream();
        InputStream encryptedInputStream =
            this.encryptionController.encryptFile(fileInputStream, user.getUsername());
        Message fileDaoMessage =
            this.fileDao.save(
                user.getUsername(),
                encryptedInputStream,
                FileType.IDENTIFICATION_PDF,
                categoryType == null ? IdCategoryType.NONE : categoryType,
                uploadDate,
                orgName,
                false,
                filename,
                "application/pdf");
        int copyNumber = 0;
        while (fileDaoMessage
            .toResponseString()
            .equals(FileMessage.FILE_EXISTS.toResponseString())) {
          System.out.println(
              "Duplicate file: user "
                  + user.getUsername()
                  + " orgname "
                  + orgName
                  + " filename "
                  + filename);
          copyNumber++;
          String newFileName =
              filename.substring(0, filename.length() - 4) + "__" + copyNumber + ".pdf";
          System.out.println("Previous filename: " + filename);
          System.out.println("Attempting new filename: " + newFileName);
          fileDaoMessage =
              this.fileDao.save(
                  user.getUsername(),
                  encryptedInputStream,
                  FileType.IDENTIFICATION_PDF,
                  categoryType == null ? IdCategoryType.NONE : categoryType,
                  uploadDate,
                  orgName,
                  false,
                  newFileName,
                  "application/pdf");
        }
      }
      System.out.println(
          "Added size: "
              + this.fileDao.getAll(eq("fileType", FileType.IDENTIFICATION_PDF.toString())).size());
      return PdfMessage.SUCCESS;
    }
    if (backfillBlankForms) {
      GetFilesInformationPDFService getFilesInformationPDFService =
          new GetFilesInformationPDFService(
              db,
              "username_doesnt_matter",
              "orgName_doesnt_matter",
              UserType.Developer,
              PDFType.BLANK_FORM, // only this field matters for mongodbGetAllFiles
              false);
      JSONArray allFiles = getFilesInformationPDFService.mongodbGetAllFiles();
      SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
      System.out.println("Starting size: " + allFiles.length());
      for (int i = 0; i < allFiles.length(); i++) {
        JSONObject fileJSON = allFiles.getJSONObject(i);
        System.out.println(fileJSON.toString());
        String fileId = fileJSON.get("id").toString();
        String filename = fileJSON.get("filename").toString();
        String uploader = fileJSON.get("uploader").toString();
        String uploadDateString = fileJSON.get("uploadDate").toString().substring(0, 10);
        Date uploadDate = dateFormatter.parse(uploadDateString);
        String orgName = fileJSON.get("organizationName").toString();
        IdCategoryType categoryType =
            fileJSON.has("idCategory")
                ? IdCategoryType.createFromString(fileJSON.get("idCategory").toString())
                : null;

        User user = userDao.get(uploader).get();
        DownloadPDFService downloadPDFService =
            new DownloadPDFService(
                db,
                user.getUsername(),
                user.getOrganization(),
                user.getUserType(),
                fileId,
                PDFType.BLANK_FORM,
                encryptionController);
        downloadPDFService.executeAndGetResponse();
        InputStream fileInputStream =
            downloadPDFService.getInputStream(); // Dont need to encrypt blank forms
        // TODO: Save file
        // TODO: Use functions in UploadAnnotatedPDFServiceV2 to create form object
        // TODO: Add fileid to form object

        //        Message fileDaoMessage =
        //            this.fileDao.save(
        //                user.getUsername(),
        //                fileInputStream,
        //                FileType.BLANK_FORM,
        //                categoryType == null ? IdCategoryType.NONE : categoryType,
        //                uploadDate,
        //                orgName,
        //                true,
        //                filename,
        //                "application/pdf");
        //        int copyNumber = 0;
        //        while (fileDaoMessage
        //            .toResponseString()
        //            .equals(FileMessage.FILE_EXISTS.toResponseString())) {
        //          System.out.println(
        //              "Duplicate file: user "
        //                  + user.getUsername()
        //                  + " orgname "
        //                  + orgName
        //                  + " filename "
        //                  + filename);
        //          copyNumber++;
        //          String newFileName =
        //              filename.substring(0, filename.length() - 4) + "__" + copyNumber + ".pdf";
        //          System.out.println("Previous filename: " + filename);
        //          System.out.println("Attempting new filename: " + newFileName);
        //          fileDaoMessage =
        //              this.fileDao.save(
        //                  user.getUsername(),
        //                  fileInputStream,
        //                  FileType.BLANK_FORM,
        //                  categoryType == null ? IdCategoryType.NONE : categoryType,
        //                  uploadDate,
        //                  orgName,
        //                  true,
        //                  newFileName,
        //                  "application/pdf");
        //        }
      }
      //      System.out.println(
      //          "Added size: " + this.fileDao.getAll(eq("fileType",
      // FileType.FORM.toString())).size());
      return PdfMessage.SUCCESS;
    }
    if (backfillCompletedApps) {
      GetFilesInformationPDFService getFilesInformationPDFService =
          new GetFilesInformationPDFService(
              db,
              "username_doesnt_matter",
              "orgName_doesnt_matter",
              UserType.Developer,
              PDFType.COMPLETED_APPLICATION, // only this field matters for mongodbGetAllFiles
              false);
      JSONArray allFiles = getFilesInformationPDFService.mongodbGetAllFiles();
      SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
      System.out.println("Starting size: " + allFiles.length());
      for (int i = 0; i < allFiles.length(); i++) {
        JSONObject fileJSON = allFiles.getJSONObject(i);
        System.out.println(fileJSON.toString());
        String fileId = fileJSON.get("id").toString();
        String filename = fileJSON.get("filename").toString();
        String uploader = fileJSON.get("uploader").toString();
        String uploadDateString = fileJSON.get("uploadDate").toString().substring(0, 10);
        Date uploadDate = dateFormatter.parse(uploadDateString);
        String orgName = fileJSON.get("organizationName").toString();
        IdCategoryType categoryType =
            fileJSON.has("idCategory")
                ? IdCategoryType.createFromString(fileJSON.get("idCategory").toString())
                : null;
        if (uploader.equals("YI-ZENG")) {
          System.out.println("SKIPPED USER");
          System.out.printf("%s %s %s %s %s", fileId, filename, uploader, uploadDateString, orgName);
          continue;
        }
        User user = userDao.get(uploader).get();
        System.out.println(user);
        DownloadPDFService downloadPDFService =
            new DownloadPDFService(
                db,
                user.getUsername(),
                user.getOrganization(),
                UserType.Worker, // Use worker to pass download filter
                fileId,
                PDFType.COMPLETED_APPLICATION,
                encryptionController);
        downloadPDFService.executeAndGetResponse();
        InputStream fileInputStream = downloadPDFService.getInputStream();
        InputStream encryptedInputStream =
            this.encryptionController.encryptFile(fileInputStream, user.getUsername());
        // TODO: Use functions in UploadAnnotatedPDFServiceV2 to create form object
        // TODO: Add fileid to form object

                Message fileDaoMessage =
                    this.fileDao.save(
                        user.getUsername(),
                        encryptedInputStream,
                        FileType.APPLICATION_PDF,
                        categoryType == null ? IdCategoryType.NONE : categoryType,
                        uploadDate,
                        orgName,
                        true,
                        filename,
                        "application/pdf");
                int copyNumber = 0;
                while (fileDaoMessage
                    .toResponseString()
                    .equals(FileMessage.FILE_EXISTS.toResponseString())) {
                  System.out.println(
                      "Duplicate file: user "
                          + user.getUsername()
                          + " orgname "
                          + orgName
                          + " filename "
                          + filename);
                  copyNumber++;

                  String newFileName = filename.replace(".pdf", "");
                  newFileName = newFileName + "__" + copyNumber + ".pdf";
                  System.out.println("Previous filename: " + filename);
                  System.out.println("Attempting new filename: " + newFileName);
                  fileDaoMessage =
                      this.fileDao.save(
                          user.getUsername(),
                          encryptedInputStream,
                          FileType.APPLICATION_PDF,
                          categoryType == null ? IdCategoryType.NONE : categoryType,
                          uploadDate,
                          orgName,
                          true,
                          newFileName,
                          "application/pdf");
                }
      }
            System.out.println(
                "Added size: " + this.fileDao.getAll(eq("fileType",
       FileType.APPLICATION_PDF.toString())).size());
      return PdfMessage.SUCCESS;
    }
    return PdfMessage.SUCCESS;
  }
}
