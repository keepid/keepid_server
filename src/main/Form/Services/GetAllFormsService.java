package Form.Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Form.Form;
import Form.FormMessage;
import User.UserType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Objects;

public class GetAllFormsService implements Service {

  String username;
  UserType privilegeLevel;

  FormDao formDao;
  boolean isTemplate;

  JSONArray formInformation;

  public GetAllFormsService(
      FormDao formDao, String username, UserType privilegeLevel, boolean isTemplate) {
    this.formDao = formDao;
    this.username = username;
    this.privilegeLevel = privilegeLevel;
    this.isTemplate = isTemplate;
  }

  @Override
  public Message executeAndGetResponse() {
    if (username == null) {
      return FormMessage.INVALID_PARAMETER;
    } else {
      if (privilegeLevel == UserType.Client
          || privilegeLevel == UserType.Worker
          || privilegeLevel == UserType.Director
          || privilegeLevel == UserType.Admin
          || privilegeLevel == UserType.Developer) {
        try {
          return mongodbGet(username, formDao);
        } catch (GeneralSecurityException | IOException e) {
          System.out.println(e.toString());
          return FormMessage.SERVER_ERROR;
        }
      } else {
        return FormMessage.INSUFFICIENT_PRIVILEGE;
      }
    }
  }

  public JSONArray getJsonInformation() {
    Objects.requireNonNull(formInformation);
    return formInformation;
  }

  public Message mongodbGet(String username, FormDao formDao)
      throws GeneralSecurityException, IOException {

    List<Form> formList = formDao.get(username);
    if (formList == null) {
      System.out.println("Form List is null");
      throw new IOException();
    }
    JSONArray responseJSON = new JSONArray();

    for (int i = 0; i < formList.size(); i++) {
      JSONObject formJSON = formList.get(i).toJSON();
      responseJSON.put(formJSON);
    }
    this.formInformation = responseJSON;

    return FormMessage.SUCCESS;
  }
}
