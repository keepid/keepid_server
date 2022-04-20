package Form.Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Form.FormMessage;
import User.UserType;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.security.GeneralSecurityException;

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
          return mongodbDelete(id, formDao);
        } catch (GeneralSecurityException | IOException e) {
          return FormMessage.SERVER_ERROR;
        }
      } else {
        return FormMessage.INSUFFICIENT_PRIVILEGE;
      }
    }
  }

  public Message mongodbDelete(ObjectId id, FormDao formDao)
      throws GeneralSecurityException, IOException {
    formDao.delete(id);
    return FormMessage.SUCCESS;
  }
}
