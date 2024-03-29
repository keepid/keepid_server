package Form.Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Form.Form;
import Form.FormMessage;
import User.UserType;
import org.bson.types.ObjectId;

import java.util.Optional;

public class DeleteFormService implements Service {

  ObjectId id;
  String username;
  UserType privilegeLevel;

  FormDao formDao;
  boolean isTemplate;

  public DeleteFormService(
      FormDao formDao, ObjectId id, String username, UserType privilegeLevel, boolean isTemplate) {
    this.formDao = formDao;
    this.id = id;
    this.username = username;
    this.privilegeLevel = privilegeLevel;
    this.isTemplate = isTemplate; // this extra param is also useless
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
          || privilegeLevel == UserType.Developer) { // this privilegeLevel check is useless
        try {
          Optional<Form> maybeForm = formDao.get(id);
          if (maybeForm.isPresent()) {
            formDao.delete(id);
          }
          return FormMessage.SUCCESS;
        } catch (Exception e) {
          return FormMessage.SERVER_ERROR;
        }
      } else {
        return FormMessage.INSUFFICIENT_PRIVILEGE;
      }
    }
  }
}
