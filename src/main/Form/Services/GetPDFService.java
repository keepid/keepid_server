package Form.Services;

import Config.Message;
import Config.Service;
import Form.Form;
import Form.FormMessage;
import PDF.PDFType;
import PDF.PdfMessage;
import PDF.Services.DownloadPDFService;
import Security.EncryptionController;
import User.UserType;
import com.mongodb.client.MongoDatabase;

import java.io.InputStream;

public class GetPDFService implements Service {

  MongoDatabase db;
  private String username;
  private String orgName;
  private UserType privilegeLevel;
  private PDFType pdfType;
  private String fileId;
  private InputStream inputStream;
  private EncryptionController encryptionController;

  public GetPDFService(
      MongoDatabase db,
      Form form,
      String orgName,
      UserType privilegeLevel,
      PDFType pdfType,
      EncryptionController encryptionController) {
    this.db = db;
    this.fileId = form.getPdfId().toString();
    this.username = form.getUsername();
    this.username = username;
    this.orgName = orgName;
    this.privilegeLevel = privilegeLevel;
    this.pdfType = pdfType;
    this.fileId = fileId;
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    DownloadPDFService pdfService =
        new DownloadPDFService(
            db, username, orgName, privilegeLevel, fileId, pdfType, encryptionController);
    Message pdfResponse = pdfService.executeAndGetResponse();
    if (pdfResponse != PdfMessage.SUCCESS) {
      return FormMessage.PDF_NOT_FOUND;
    }
    inputStream = pdfService.getInputStream();
    return FormMessage.SUCCESS;
  }

  public InputStream getInputStream() {
    return inputStream;
  }
}
