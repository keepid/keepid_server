package PDF.Services.V2Services;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import File.File;
import File.FileType;
import PDF.PDFTypeV2;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import User.UserType;
import java.util.List;
import java.util.Objects;
import org.bson.conversions.Bson;
import org.json.JSONArray;

// Formerly GetFilesInformationPDFService
public class FilterPDFServiceV2 implements Service {
  private FileDao fileDao;
  private String username;
  private String organizationName;
  private UserType privilegeLevel;
  private PDFTypeV2 pdfType;
  private boolean annotated;
  private JSONArray files;
  private Bson filter;

  public FilterPDFServiceV2(FileDao fileDao, UserParams userParams, FileParams fileParams) {
    this.fileDao = fileDao;
    this.username = userParams.getUsername();
    this.organizationName = userParams.getOrganizationName();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.pdfType = fileParams.getPdfType();
    this.annotated = fileParams.getAnnotated();
  }

  public JSONArray getFiles() {
    return Objects.requireNonNull(this.files);
  }

  @Override
  public Message executeAndGetResponse() {
    Message FilterConditionsErrorMessage = checkFilterConditions();
    if (FilterConditionsErrorMessage != null) {
      return FilterConditionsErrorMessage;
    }
    Message setFilterErrorMessage = setFilter();
    if (setFilterErrorMessage != null) {
      return setFilterErrorMessage;
    }
    return filter();
  }

  public Message checkFilterConditions() {
    if (this.pdfType == null) {
      return PdfMessage.INVALID_PDF_TYPE;
    }
    if (this.privilegeLevel != UserType.Client
        && this.privilegeLevel != UserType.Worker
        && this.privilegeLevel != UserType.Director
        && this.privilegeLevel != UserType.Admin
        && this.privilegeLevel != UserType.Developer) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
    }
    return null;
  }

  // TODO: Team Keep filter gets everything ()
  public Message setFilter() {
    if (this.pdfType == PDFTypeV2.BLANK_APPLICATION) {
      if (this.privilegeLevel == UserType.Developer) { // Deprecated
        this.filter = and(eq("fileType", FileType.FORM.toString()), eq("annotated", annotated));
      } else {
        this.filter =
            and(
                eq("fileType", FileType.FORM.toString()),
                or(eq("organizationName", organizationName), eq("organizationName", "Team Keep")),
                eq("annotated", annotated));
      }
    } else if (this.pdfType == PDFTypeV2.CLIENT_UPLOADED_DOCUMENT) {
      if (this.privilegeLevel == UserType.Client) {
        this.filter =
            and(
                eq("fileType", FileType.IDENTIFICATION_PDF.toString()),
                eq("organizationName", organizationName),
                eq("username", username));
      } else {
        return PdfMessage.INSUFFICIENT_PRIVILEGE;
      }
    } else if (this.pdfType == PDFTypeV2.ANNOTATED_APPLICATION) {
      if (this.privilegeLevel == UserType.Director
          || this.privilegeLevel == UserType.Admin
          || this.privilegeLevel == UserType.Worker) {
        this.filter =
            and(
                eq("fileType", FileType.APPLICATION_PDF.toString()),
                eq("organizationName", organizationName));
      } else {
        return PdfMessage.INSUFFICIENT_PRIVILEGE;
      }
    } else {
      return PdfMessage.INVALID_PDF_TYPE;
    }
    return null;
  }

  public Message filter() {
    List<File> filteredFiles = this.fileDao.getAll(this.filter);
    this.files = new JSONArray();
    for (File filteredFile : filteredFiles) {
      this.files.put(filteredFile.toJsonView());
    }
    return PdfMessage.SUCCESS;
  }
}
