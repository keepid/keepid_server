package Form.Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Form.Form;
import Form.FormMessage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;

public class ManuallyUploadFormService implements Service {

  FormDao formDao;
  JSONObject formJson;
  Form form;
  private static String exampleFormJsonPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "main"
          + File.separator
          + "Form"
          + File.separator
          + "Services"
          + File.separator
          + "exampleForm.json";

  public ManuallyUploadFormService(FormDao formDao) {
    this.formDao = formDao;
    try {
      this.formJson =
          new JSONObject(new String(Files.readAllBytes(Paths.get(exampleFormJsonPath))));
    } catch (Exception e) {
      System.out.println(e);
    }
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
    formDao.save(form);
    return FormMessage.SUCCESS;
  }
}
