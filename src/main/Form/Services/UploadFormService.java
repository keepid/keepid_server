package Form.Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Form.Form;
import Form.FormMessage;
import Form.FormType;
import User.UserType;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.json.JSONObject;

public class UploadFormService implements Service {

  String uploader;
  UserType privilegeLevel;
  FormDao formDao;
  JSONObject formJson;
  Form form;

  public UploadFormService(
      FormDao formDao, String uploaderUsername, UserType privilegeLevel, JSONObject formJson) {
    this.formDao = formDao;
    this.uploader = uploaderUsername;
    this.privilegeLevel = privilegeLevel;
    this.formJson = formJson;
    //    this.formType = formType;
    //    this.metadata = metadata;
    //    this.body = body;
    //    this.isTemplate = isTemplate;
  }

  @Override
  public Message executeAndGetResponse() {
    try {
      this.form = Form.fromJson(formJson);
    } catch (Exception e) {
      return FormMessage.INVALID_FORM;
    }
    if (this.form == null) {
      return FormMessage.INVALID_FORM;
    }

    if (form.getFormType() == null) {
      return FormMessage.INVALID_FORM_TYPE;
    } else if (form.getMetadata() == null) {
      return FormMessage.INVALID_FORM;
    } else if (form.getBody() == null) {
      return FormMessage.INVALID_FORM;
    } else {
      if ((form.getFormType() == FormType.APPLICATION
              || form.getFormType() == FormType.IDENTIFICATION
              || form.getFormType() == FormType.FORM)
          && (privilegeLevel == UserType.Client
              || privilegeLevel == UserType.Worker
              || privilegeLevel == UserType.Director
              || privilegeLevel == UserType.Admin
              || privilegeLevel == UserType.Developer)) {
        try {
          return mongodbUpload(uploader, form, formDao);
        } catch (GeneralSecurityException | IOException e) {
          return FormMessage.SERVER_ERROR;
        }
      } else {
        return FormMessage.INSUFFICIENT_PRIVILEGE;
      }
    }
  }

  public Message mongodbUpload(String uploader, Form form, FormDao formDao)
      throws GeneralSecurityException, IOException {
    formDao.save(form);
    return FormMessage.SUCCESS;
  }
}
