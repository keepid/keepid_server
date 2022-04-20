package Form.Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Form.Form;
import Form.FormMessage;
import User.UserType;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

public class GetFormService implements Service {

  ObjectId id;
  String username;
  UserType privilegeLevel;

  FormDao formDao;
  boolean isTemplate;

  JSONObject formInformation;

  public GetFormService(
      FormDao formDao, ObjectId id, String username, UserType privilegeLevel, boolean isTemplate) {
    this.formDao = formDao;
    this.id = id;
    this.username = username;
    this.privilegeLevel = privilegeLevel;
    this.isTemplate = isTemplate;
  }

  @Override
  public Message executeAndGetResponse() {
    if (id == null) {
      return FormMessage.INVALID_PARAMETER;
    } else {
      if (privilegeLevel == UserType.Client
          || privilegeLevel == UserType.Worker
          || privilegeLevel == UserType.Director
          || privilegeLevel == UserType.Admin
          || privilegeLevel == UserType.Developer) {
        try {
          return mongodbGet(id, formDao);
        } catch (GeneralSecurityException | IOException e) {
          return FormMessage.SERVER_ERROR;
        }
      } else {
        return FormMessage.INSUFFICIENT_PRIVILEGE;
      }
    }
  }

  public JSONObject getJsonInformation() {
    Objects.requireNonNull(formInformation);
    return formInformation;
  }

  public Message mongodbGet(ObjectId id, FormDao formDao)
      throws GeneralSecurityException, IOException {
    JSONObject responseJSON = new JSONObject();

    Optional<Form> formOptional = formDao.get(id);
    Form form = null;
    if (formOptional.isPresent()) {
      form = formOptional.get();
    }
    if (form == null) {
      throw new IOException();
    }

    responseJSON.put("id", id);
    responseJSON.put("fileId", form.getFileId());
    responseJSON.put("username", form.getUsername());
    responseJSON.put("uploaderUsername", form.getUploaderUsername());
    responseJSON.put("formType", form.getFormType().toString());

    // Get Metadata

    JSONObject metadata = new JSONObject();

    Form.Metadata md = form.getMetadata();
    metadata.put("title", md.getTitle());
    metadata.put("description", md.getDescription());
    metadata.put("state", md.getState());
    metadata.put("county", md.getCounty());

    List<String> prerequisites = new LinkedList<>();
    Set<ObjectId> prereqs = md.getPrerequisities();
    Iterator<ObjectId> prereqIterator = prereqs.iterator();
    while (prereqIterator.hasNext()) {
      ObjectId nextPrereq = prereqIterator.next();
      prerequisites.add(nextPrereq.toString());
    }
    metadata.put("prerequisites", new JSONArray(prerequisites));

    List<String> paymentInfos = md.getPaymentInfo();
    metadata.put("paymentInfos", new JSONArray(paymentInfos));

    int numLines = md.getNumLines();
    metadata.put("numLines", numLines);

    Date lastRevisionDate = md.getLastRevisionDate();
    metadata.put("lastRevisionDate", lastRevisionDate.toString());

    responseJSON.put("metadata", metadata);

    // Get body

    JSONObject body = getBody(new JSONObject(), form.getBody());
    responseJSON.put("body", body);

    this.formInformation = responseJSON;

    return FormMessage.SUCCESS;
  }

  private JSONObject getBody(JSONObject body, Form.Section bd) {
    body.put("title", bd.getTitle());
    body.put("description", bd.getDescription());

    List<JSONObject> subsections = new LinkedList<>();
    List<Form.Section> subsectionList = bd.getSubsections();
    Iterator<Form.Section> subsectionIterator = subsectionList.iterator();
    while (subsectionIterator.hasNext()) {
      Form.Section nextBd = subsectionIterator.next();
      JSONObject nextBody = getBody(new JSONObject(), nextBd);
    }
    body.put("subsections", subsections);

    List<JSONObject> questions = new LinkedList<>();
    List<Form.Question> questionList = bd.getQuestions();
    Iterator<Form.Question> questionIterator = questionList.iterator();
    while (questionIterator.hasNext()) {
      Form.Question nextQuestion = questionIterator.next();
      JSONObject question = new JSONObject();
      question.put("id", nextQuestion.getId().toString());
      question.put("type", nextQuestion.getType().toString());
      question.put("questionText", nextQuestion.getQuestionText());
      question.put("options", new JSONArray(nextQuestion.getOptions()));
      question.put("defaultValue", nextQuestion.getDefaultValue());
      question.put("isRequired", String.valueOf(nextQuestion.isRequired()));
      question.put("numLines", nextQuestion.getNumLines());
      question.put("isMatched", String.valueOf(nextQuestion.isMatched()));
      question.put("conditionalOnField", nextQuestion.getConditionalOnField().toString());
      question.put("isConditionalType", String.valueOf(nextQuestion.isConditionalType()));
      questions.add(question);
    }
    body.put("questions", questions);

    return body;
  }
}
