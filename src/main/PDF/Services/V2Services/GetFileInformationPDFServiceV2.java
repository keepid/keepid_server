package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import User.UserType;
import java.util.Objects;
import org.json.JSONArray;

public class GetFileInformationPDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private PDFTypeV2 pdfType;
  private boolean annotated;
  private JSONArray files;

  public GetFileInformationPDFServiceV2(
      FileDao fileDao, FormDao formDao, UserParams userParams, FileParams fileParams) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.username = userParams.getUsername();
    this.organizationName = userParams.getOrganizationName();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.pdfType = fileParams.getPdfType();
    this.annotated = fileParams.getAnnotated();
  }

  public JSONArray getFiles() {
    return Objects.requireNonNull(files);
  }

  @Override
  public Message executeAndGetResponse() {
    Message GetFileInformationConditionsErrorMessage = checkGetFileInfoConditions();
    if (GetFileInformationConditionsErrorMessage != null) {
      return GetFileInformationConditionsErrorMessage;
    }
    return getFileInformation();
  }

  public Message checkGetFileInfoConditions() {
    if (pdfType == null) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
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

  public Message getFileInformation() {
    // HOW TO HANDLE FILES WITH CORRESPONDING FORMS
    //
    //
    return null;
  }
}
