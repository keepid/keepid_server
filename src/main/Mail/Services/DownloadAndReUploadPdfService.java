package Mail.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import File.FileType;
import File.IdCategoryType;
import Mail.MailMessage;
import PDF.PDFType;
import PDF.Services.CrudServices.DownloadPDFService;
import Security.EncryptionController;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import java.io.InputStream;
import java.util.Date;

public class DownloadAndReUploadPdfService implements Service {
  private EncryptionController encryptionController;
  private MongoDatabase db;
  private FileDao fileDao;
  private String fileId;
  private String username;
  private String orgName;
  private PDFType pdfType;
  private UserType userType;

  public DownloadAndReUploadPdfService(
      EncryptionController encryptionController,
      MongoDatabase db,
      FileDao fileDao,
      String fileId,
      String username,
      String orgName) {
    this.encryptionController = encryptionController;
    this.db = db;
    this.fileDao = fileDao;
    this.fileId = fileId;
    this.username = username;
    this.orgName = orgName;
    this.pdfType = PDFType.IDENTIFICATION_DOCUMENT;
    this.userType = UserType.Client;
  }

  @Override
  public Message executeAndGetResponse() throws Exception {
    DownloadPDFService downloadPDFService =
        new DownloadPDFService(
            db, username, orgName, userType, fileId, pdfType, encryptionController);
    downloadPDFService.executeAndGetResponse();
    InputStream fileInputStream = downloadPDFService.getInputStream();
    fileDao.save(
        this.username,
        fileInputStream,
        FileType.IDENTIFICATION_PDF,
        IdCategoryType.DRIVERS_LICENSE_PHOTO_ID,
        new Date(),
        this.orgName,
        false,
        "PA_DRIVERS_LICENSE",
        "application/pdf");
    return MailMessage.MAIL_SUCCESS;
  }
}
