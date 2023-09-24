package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import File.File;
import Form.Form;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import User.UserType;
import Validation.ValidationUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.bson.types.ObjectId;
import org.json.JSONObject;

public class FillPDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private UserType privilegeLevel;
  private String fileId;
  private JSONObject formAnswers;
  private InputStream filledFileStream;
  private File filledFile;
  private Form filledForm;

  public FillPDFServiceV2(
      FileDao fileDao, FormDao formDao, UserParams userParams, FileParams fileParams) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.fileId = fileParams.getFileId();
    this.formAnswers = fileParams.getFormAnswers();
  }

  public InputStream getFilledFileStream() {
    return Objects.requireNonNull(filledFileStream);
  }

  public File getFilledFile() {
    return Objects.requireNonNull(filledFile);
  }

  public Form getFilledForm() {
    return Objects.requireNonNull(filledForm);
  }

  @Override
  public Message executeAndGetResponse() {
    Message FillPDFConditionsErrorMessage = checkFillConditions();
    if (FillPDFConditionsErrorMessage != null) {
      return FillPDFConditionsErrorMessage;
    }
    return fill();
  }

  public Message checkFillConditions() {
    if (!ValidationUtils.isValidObjectId(fileId) || formAnswers == null) {
      return PdfMessage.INVALID_PARAMETER;
    }
    if (privilegeLevel == UserType.Developer) {
      return PdfMessage.INSUFFICIENT_PRIVILEGE;
    }
    return null;
  }

  public Message fill() {
    // FILL (NEW?) FORM WITH ANSWERS
    // FIGURE OUT HOW TO GET THE DISPLAYED PDF
    //
    ObjectId fileObjectId = new ObjectId(fileId);
    Optional<File> templateFileOptional = fileDao.get(fileObjectId);
    if (templateFileOptional.isEmpty()) {
      return PdfMessage.NO_SUCH_FILE;
    }
    File templateFile = templateFileOptional.get();
    Optional<Form> templateFormOptional = formDao.getByFileId(fileObjectId);
    if (templateFormOptional.isEmpty()) {
      return PdfMessage.MISSING_FORM;
    }
    PDDocument pdfDocument = null;
    try {
      pdfDocument = Loader.loadPDF(templateFile.getFileStream());
    } catch (IOException e) {
      return PdfMessage.SERVER_ERROR;
    }
    pdfDocument.setAllSecurityToBeRemoved(true);
    PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm();
    if (acroForm == null) {
      return PdfMessage.INVALID_PDF;
    }

    return PdfMessage.SUCCESS;
  }
}
