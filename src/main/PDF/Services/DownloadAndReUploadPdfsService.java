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

  public DownloadAndReUploadPdfsService(
      EncryptionController encryptionController,
      MongoDatabase db,
      FileDao fileDao,
      UserDao userDao,
      OrgDao orgDao) {
    this.encryptionController = encryptionController;
    this.db = db;
    this.fileDao = fileDao;
    this.userDao = userDao;
    this.orgDao = orgDao;
  }

  @Override
  public Message executeAndGetResponse() throws Exception {
    boolean dangerousDelete = false; // MAKE SURE TO CHANGE THE FILETYPE IN THIS SCRIPT
    boolean testingFilter = false; // MAKE SURE TO CHANGE THE FILETYPE IN THIS SCRIPT
    boolean testingDownload = false; // MAKE SURE TO CHANGE THE USER/FILE PARAMS IN THIS SCRIPT
    boolean backfillIdentificationPdfs = false;
    boolean backfillBlankForms = true;
    boolean backfillCompletedApps = false;
    if (dangerousDelete) {
      List<File> fileDaoFiles =
          fileDao.getAll(eq("fileType", FileType.IDENTIFICATION_PDF.toString()));
      System.out.println("Presize: " + fileDaoFiles.size());
      for (File f : fileDaoFiles) {
        this.fileDao.delete(f.getId());
      }
      fileDaoFiles = fileDao.getAll(eq("fileType", FileType.IDENTIFICATION_PDF.toString()));
      System.out.println("Postsize: " + fileDaoFiles.size());
      return PdfMessage.SUCCESS;
    }
    if (testingFilter) {
      GetFilesInformationPDFService getFilesInformationPDFService =
          new GetFilesInformationPDFService(
              db, "username", "orgName", UserType.Developer, PDFType.BLANK_FORM, false);
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
          fileDao.getAll(eq("fileType", FileType.IDENTIFICATION_PDF.toString()));
      for (File f : fileDaoFiles) {
        System.out.println(f.toJsonView().toString());
      }
      System.out.println("Filedao size:" + fileDaoFiles.size());
      return PdfMessage.SUCCESS;
    }
    if (testingDownload) {
      DownloadPDFServiceV2 service =
          new DownloadPDFServiceV2(
              fileDao,
              null,
              new UserParams()
                  .setUsername("ChristineFowlkes")
                  .setOrganizationName("Safe Harbor Easton Inc.")
                  .setPrivilegeLevel(UserType.Client),
              new FileParams()
                  .setFileId("66c3d9bb4a519b3d7e3f9604")
                  .setPdfType(PDFTypeV2.CLIENT_UPLOADED_DOCUMENT),
              encryptionController);
      System.out.println(service.executeAndGetResponse());
      InputStream is = service.getDownloadedInputStream();
      Files.copy(
          is,
          Paths.get(
              "/Users/samuellee/Library/Mobile Documents/com~apple~CloudDocs/Online Downloads/testtest1.pdf"),
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
    if (false && backfillCompletedApps) {
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

        User user = userDao.get(uploader).get();
        DownloadPDFService downloadPDFService =
            new DownloadPDFService(
                db,
                user.getUsername(),
                user.getOrganization(),
                user.getUserType(),
                fileId,
                PDFType.COMPLETED_APPLICATION,
                encryptionController);
        downloadPDFService.executeAndGetResponse();
        InputStream fileInputStream = downloadPDFService.getInputStream();
        InputStream encryptedInputStream =
            this.encryptionController.encryptFile(fileInputStream, user.getUsername());
        // TODO: Save file
        // TODO: Use functions in UploadAnnotatedPDFServiceV2 to create form object
        // TODO: Add fileid to form object

        //        Message fileDaoMessage =
        //            this.fileDao.save(
        //                user.getUsername(),
        //                encryptedInputStream,
        //                FileType.COMPLETED_APPLICATION,
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
        //                  encryptedInputStream,
        //                  FileType.COMPLETED_APPLICATION,
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
      // FileType.COMPLETED_APPLICATION.toString())).size());
      return PdfMessage.SUCCESS;
    }
    return PdfMessage.SUCCESS;
  }
}
