package Form.Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Form.Form;
import Form.FormMessage;
import User.UserType;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Optional;

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
          System.out.println(e.toString());
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

    Optional<Form> formOptional = formDao.get(id);
    Form form = null;
    if (formOptional.isPresent()) {
      System.out.println("Form is present");
      form = formOptional.get();
    }
    if (form == null) {
      System.out.println("Form is null");
      return FormMessage.FORM_NOT_FOUND;
    }

    JSONObject responseJSON = form.toJSON();
    this.formInformation = responseJSON;

    return FormMessage.SUCCESS;
  }
}
