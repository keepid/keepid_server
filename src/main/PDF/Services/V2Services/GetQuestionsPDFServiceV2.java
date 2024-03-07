package PDF.Services.V2Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Database.User.UserDao;
import Form.FieldType;
import Form.Form;
import Form.FormQuestion;
import Form.FormSection;
import PDF.PdfControllerV2.FileParams;
import PDF.PdfControllerV2.UserParams;
import PDF.PdfMessage;
import User.Services.GetUserInfoService;
import User.UserMessage;
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
  private JSONObject userInfo;
  private FormQuestion currentFormQuestion;

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
    GetUserInfoService getUserInfoService = new GetUserInfoService(userDao, username);
    Message getUserInfoServiceResponse = getUserInfoService.executeAndGetResponse();
    if (getUserInfoServiceResponse != UserMessage.SUCCESS) {
      return getUserInfoServiceResponse;
    }
    this.userInfo = getUserInfoService.getUserFields();
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

  public Message setMatchedFields(FormQuestion fq) {
    String questionName = fq.getQuestionName();
    String[] splitQuestionName = questionName.split(":");
    if (splitQuestionName.length != 1
        && splitQuestionName.length != 2
        && splitQuestionName.length != 3) {
      return PdfMessage.INVALID_MATCHED_FIELD;
    }

    fq.setQuestionText(splitQuestionName[0]);
    if (splitQuestionName.length == 1) {
      return null;
    }
    String fieldTypeIndicatorString = splitQuestionName[1];
    if (fieldTypeIndicatorString.startsWith("+")) {
      // Positively linked field
      fq.setConditionalType("POSITIVE");
      fq.setConditionalOnField(new ObjectId(fieldTypeIndicatorString.substring(1)));
    } else if (fieldTypeIndicatorString.startsWith("-")) {
      // Negatively linked field
      fq.setConditionalType("NEGATIVE");
      fq.setConditionalOnField(new ObjectId(fieldTypeIndicatorString.substring(1)));
    } else if (fieldTypeIndicatorString.equals("anyDate")) {
      // Make it a date field that can be selected by the client
      fq.setType(FieldType.DATE_FIELD);
    } else if (fieldTypeIndicatorString.equals("currentDate")) {
      // Make a date field with the current date that cannot be changed (value set on frontend)
      fq.setType(FieldType.DATE_FIELD);
      fq.setMatched(true);
    } else if (fieldTypeIndicatorString.equals("signature")) {
      // Signatures not handled in first round of form completion
      fq.setType(FieldType.SIGNATURE);
    } else if (this.userInfo.has(fieldTypeIndicatorString)) {
      // Field has a matched database variable, so make that the autofilled value
      fq.setMatched(true);
      fq.setDefaultValue((String) this.userInfo.get(fieldTypeIndicatorString));
    } else {
      return PdfMessage.INVALID_MATCHED_FIELD;
    }
    this.currentFormQuestion = fq;
    return null;
  }

  public Message getQuestions() {
    FormSection formBody = form.getBody();
    applicationInformation.put("title", formBody.getTitle());
    applicationInformation.put("description", formBody.getDescription());
    List<FormQuestion> formQuestions = formBody.getQuestions();
    List<JSONObject> formFields = new LinkedList<>();
    for (FormQuestion formQuestion : formQuestions) {
      this.currentFormQuestion = formQuestion;
      Message matchedFieldsMessage = setMatchedFields(formQuestion);
      if (matchedFieldsMessage != null) {
        return matchedFieldsMessage;
      }
      JSONObject formField = new JSONObject();
      formField.put("fieldName", this.currentFormQuestion.getQuestionName());
      formField.put("fieldType", this.currentFormQuestion.getType().toString());
      formField.put("fieldValueOptions", new JSONArray(this.currentFormQuestion.getOptions()));
      formField.put("fieldDefaultValue", this.currentFormQuestion.getDefaultValue());
      formField.put("fieldIsRequired", this.currentFormQuestion.isRequired());
      formField.put("fieldNumLines", this.currentFormQuestion.getNumLines());
      formField.put("fieldIsMatched", this.currentFormQuestion.isMatched());
      formField.put("fieldQuestion", this.currentFormQuestion.getQuestionText());
      formField.put("fieldLinkageType", this.currentFormQuestion.getConditionalType());
      formField.put("fieldLinkedTo", this.currentFormQuestion.getConditionalOnField());
      formField.put("fieldStatus", "SUCCESS");
      formFields.add(formField);
    }
    applicationInformation.put("fields", formFields);
    return PdfMessage.SUCCESS;
  }
}
