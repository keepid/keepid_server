package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Database.User.UserDao;
import Form.Form;
import Form.FormQuestion;
import Form.FormSection;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import User.UserType;
import Validation.ValidationUtils;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetQuestionsPDFServiceV2 implements Service {
  private FormDao formDao;
  private UserDao userDao;
  private String username;
  private UserType privilegeLevel;
  private String fileId;
  private JSONObject applicationInformation;
  private Form form;

  public GetQuestionsPDFServiceV2(
      FormDao formDao, UserDao userDao, UserParams userParams, FileParams fileParams) {
    this.formDao = formDao;
    this.userDao = userDao;
    this.username = userParams.getUsername();
    this.privilegeLevel = userParams.getPrivilegeLevel();
    this.fileId = fileParams.getFileId();
    this.applicationInformation = new JSONObject();
  }

  public JSONObject getApplicationInformation() {
    return Objects.requireNonNull(applicationInformation);
  }

  @Override
  public Message executeAndGetResponse() {
    Message getQuestionsConditionsErrorMessage = checkGetQuestionsConditions();
    if (getQuestionsConditionsErrorMessage != null) {
      return getQuestionsConditionsErrorMessage;
    }
    return getQuestions();
  }

  public Message checkGetQuestionsConditions() {
    if (privilegeLevel == null) {
      return PdfMessage.INVALID_PRIVILEGE_TYPE;
    }
    if (!ValidationUtils.isValidObjectId(fileId)) {
      return PdfMessage.INVALID_PARAMETER;
    }
    ObjectId fileObjectId = new ObjectId(fileId);
    Optional<Form> formOptional = formDao.getByFileId(fileObjectId);
    if (formOptional.isEmpty()) {
      return PdfMessage.MISSING_FORM;
    }
    form = formOptional.get();
    return null;
  }

  public Message getQuestions() {
    FormSection formBody = form.getBody();
    applicationInformation.put("title", formBody.getTitle());
    applicationInformation.put("description", formBody.getDescription());
    List<FormQuestion> formQuestions = formBody.getQuestions();
    List<JSONObject> formFields = new LinkedList<>();
    for (FormQuestion formQuestion : formQuestions) {
      JSONObject formField = new JSONObject();
      // WHAT TO DO WITH TITLE? IT IS NOT IN formQuestion
      //
      //
      formField.put("fieldName", "HELP THIS STRING PLEASE");
      formField.put("fieldType", formQuestion.getType().toString());
      formField.put("fieldValueOptions", new JSONArray(formQuestion.getOptions()));
      formField.put("fieldDefaultValue", formQuestion.getDefaultValue());
      formField.put("fieldIsRequired", formQuestion.isRequired());
      formField.put("fieldNumLines", formQuestion.getNumLines());
      formField.put("fieldIsMatched", formQuestion.isMatched());
      formField.put("fieldQuestion", formQuestion.getQuestionText());
      // WHAT TO DO WITH THESE THREE FIELDS?
      //
      //
      formField.put("fieldLinkageType", "HELP THIS STRING PLEASE");
      formField.put("fieldLinkedTo", "HELP THIS STRING PLEASE");
      formField.put("fieldStatus", "HELP THIS STRING PLEASE");
      formFields.add(formField);
    }
    applicationInformation.put("fields", formFields);
    return null;
  }
}
