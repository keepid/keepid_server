package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import User.UserType;
import Validation.ValidationUtils;
import java.io.InputStream;
import java.util.Objects;
import org.json.JSONObject;

public class FillPDFServiceV2 implements Service {
  private FileDao fileDao;
  private FormDao formDao;
  private UserType privilegeLevel;
  private String fileId;
  private JSONObject formAnswers;
  private InputStream filledForm;

  public FillPDFServiceV2(
      FileDao fileDao, FormDao formDao, UserParams userParams, FileParams fileParams) {
    this.fileDao = fileDao;
    this.formDao = formDao;
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.fileId = fileParams.getFileId();
    this.formAnswers = fileParams.getFormAnswers();
  }

  public InputStream getFilledForm() {
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
    //
    return null;
  }
}
