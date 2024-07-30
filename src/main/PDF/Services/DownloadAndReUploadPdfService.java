package PDF.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.User.UserDao;
import File.FileType;
import File.IdCategoryType;
import Mail.MailMessage;
import PDF.PDFType;
import PDF.Services.CrudServices.DownloadPDFService;
import PDF.Services.CrudServices.GetFilesInformationPDFService;
import Security.EncryptionController;
import User.User;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import java.io.InputStream;
import org.json.JSONObject;

public class DownloadAndReUploadPdfService implements Service {
  private EncryptionController encryptionController;
  private MongoDatabase db;
  private FileDao fileDao;
  private String username;
  private UserDao userDao;
  private PDFType pdfType;
  private FileType fileType;

  public DownloadAndReUploadPdfService(
      EncryptionController encryptionController,
      MongoDatabase db,
      FileDao fileDao,
      UserDao userDao,
      String username,
      PDFType pdfType,
      FileType fileType) {
    this.encryptionController = encryptionController;
    this.db = db;
    this.fileDao = fileDao;
    this.userDao = userDao;
    this.username = username;
    this.pdfType = pdfType;
    this.fileType = fileType;
  }

  @Override
  public Message executeAndGetResponse() throws Exception {
    boolean isDryRun = true;
    User user = userDao.get(username).get();
    UserType userType = user.getUserType();
    String orgName = user.getOrganization();
    GetFilesInformationPDFService getFilesInformationPDFService =
        new GetFilesInformationPDFService(db, username, orgName, userType, pdfType, false);
    getFilesInformationPDFService.executeAndGetResponse();

    String filename;
    String uploader;
    String uploadDate;
    IdCategoryType categoryType;

    for (int i = 0; i < getFilesInformationPDFService.getFiles().length(); i++) {
      JSONObject fileJSON = getFilesInformationPDFService.getFiles().getJSONObject(i);

      String fileId = fileJSON.get("id").toString();

      filename = fileJSON.get("filename").toString();
      uploader = fileJSON.get("uploader").toString();
      uploadDate = fileJSON.get("uploadDate").toString();
      categoryType = IdCategoryType.createFromString(fileJSON.get("idCategory").toString());

      System.out.println("FILE ID: " + fileId);
      System.out.println("FILENAME: " + filename);
      System.out.println("uploader: " + uploader);
      System.out.println("uploadDate: " + uploadDate);
      System.out.println("categoryType: " + categoryType);

      if (!isDryRun) {
        DownloadPDFService downloadPDFService =
            new DownloadPDFService(
                db, username, orgName, userType, fileId, pdfType, encryptionController);
        downloadPDFService.executeAndGetResponse();
        InputStream fileInputStream = downloadPDFService.getInputStream();
        //        fileDao.save(
        //            this.username,
        //            fileInputStream,
        //            FileType.IDENTIFICATION_PDF,
        //            IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
        //            new Date(),
        //            this.orgName,
        //            false,
        //            "PA_DRIVERS_LICENSE",
        //            "application/pdf");
      }
    }
    return MailMessage.MAIL_SUCCESS;
  }
}
