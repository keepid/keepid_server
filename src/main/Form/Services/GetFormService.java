package Form.Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Form.Form;
import Form.FormMessage;
import User.UserType;
import java.util.Objects;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.json.JSONObject;

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
      Optional<Form> formOptional = formDao.get(id);
      if (formOptional.isEmpty()) {
        return FormMessage.NO_SUCH_FILE;
      }
      JSONObject responseJSON = formOptional.get().toJSON();
      this.formInformation = responseJSON;
      return FormMessage.SUCCESS;
    }
  }

  public JSONObject getJsonInformation() {
    Objects.requireNonNull(formInformation);
    return this.formInformation;
  }
}
