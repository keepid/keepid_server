package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import File.FileType;
import File.IdCategoryType;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import PDF.Services.CrudServices.ImageToPDFService;
import Security.EncryptionController;
import User.UserType;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Date;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

public class UploadPDFServiceV2 implements Service {
  private FileDao fileDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private PDFTypeV2 pdfType;
  private String fileName;
  private String fileContentType;
  private InputStream fileStream;
  private IdCategoryType idCategoryType;
  private EncryptionController encryptionController;

  public UploadPDFServiceV2(
      FileDao fileDao,
      UserParams userParams,
      FileParams fileParams,
      EncryptionController encryptionController) {
    this.fileDao = fileDao;
    this.username = userParams.getUsername();
    this.organizationName = userParams.getOrganizationName();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.pdfType = fileParams.getPdfType();
    this.fileName = fileParams.getFileName();
    this.fileContentType = fileParams.getFileContentType();
    this.fileStream = fileParams.getFileStream();
    this.idCategoryType = fileParams.getIdCategoryType();
    this.encryptionController = encryptionController;
  }

  @Override
  public Message executeAndGetResponse() {
    Message uploadConditionsErrorMessage = checkUploadConditions();
    if (uploadConditionsErrorMessage != null) {
      return uploadConditionsErrorMessage;
    }
    if (fileContentType.startsWith("image")) {
      Message convertImageToPDFErrorMessage = convertImageToPDF();
      if (convertImageToPDFErrorMessage != null) {
        return convertImageToPDFErrorMessage;
      }
    }
    return upload();
  }

  public Message checkUploadConditions() {
    if (pdfType == null) {
      return PdfMessage.INVALID_PDF_TYPE;
    }
    if (fileStream == null
        || (!fileContentType.equals("application/pdf") && !fileContentType.startsWith("image"))) {
      return PdfMessage.INVALID_PDF;
    }
    if (privilegeLevel != UserType.Client
        && privilegeLevel != UserType.Worker
        && privilegeLevel != UserType.Director
        && privilegeLevel != UserType.Admin
        && privilegeLevel != UserType.Developer) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
    }
    return null;
  }

  public Message convertImageToPDF() {
    ImageToPDFService imageToPDFService = new ImageToPDFService(fileStream);
    Message imageToPDFResponse = imageToPDFService.executeAndGetResponse();
    if (imageToPDFResponse != PdfMessage.SUCCESS) return imageToPDFResponse;
    InputStream tempFileStream = imageToPDFService.getFileStream();
    try {
      fileStream.close();
    } catch (IOException e) {
      return PdfMessage.SERVER_ERROR;
    }
    fileStream = tempFileStream;
    fileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".pdf";
    return null;
  }

  public static String getPDFTitle(String fileName, InputStream content, PDFTypeV2 pdfType) {
    String title = fileName;
    if (pdfType == PDFTypeV2.BLANK_APPLICATION || pdfType == PDFTypeV2.ANNOTATED_APPLICATION) {
      try {
        PDDocument pdfDocument = Loader.loadPDF(content);
        pdfDocument.setAllSecurityToBeRemoved(true);
        String titleTmp = pdfDocument.getDocumentInformation().getTitle();
        title = titleTmp != null ? titleTmp : fileName;
        content.reset();
        pdfDocument.close();
      } catch (IOException exception) {
        return fileName;
      }
    }
    return title;
  }

  public Message upload() {
    fileName = getPDFTitle(fileName, fileStream, pdfType);
    Date currentDate = new Date();
    FileType fileType = null;
    boolean annotated = false;
    if (pdfType == PDFTypeV2.BLANK_APPLICATION) {
      fileType = FileType.FORM_PDF;
    } else if (pdfType == PDFTypeV2.ANNOTATED_APPLICATION) {
      fileType = FileType.APPLICATION_PDF;
      try {
        fileStream = encryptionController.encryptFile(fileStream, username);
      } catch (GeneralSecurityException | IOException e) {
        return PdfMessage.SERVER_ERROR;
      }
      annotated = true;
    } else if (pdfType == PDFTypeV2.CLIENT_UPLOADED_DOCUMENT) {
      try {
        fileStream = encryptionController.encryptFile(fileStream, username);
      } catch (GeneralSecurityException | IOException e) {
        return PdfMessage.SERVER_ERROR;
      }
      fileType = FileType.IDENTIFICATION_PDF;
    } else {
      return PdfMessage.INVALID_PDF_TYPE;
    }
    fileDao.save(
        username,
        fileStream,
        fileType,
        idCategoryType,
        currentDate,
        organizationName,
        annotated,
        fileName,
        fileContentType);
    try {
      fileStream.close();
    } catch (IOException e) {
      return PdfMessage.SERVER_ERROR;
    }
    return PdfMessage.SUCCESS;
  }
}
