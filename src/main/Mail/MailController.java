package Mail;

import Config.DeploymentLevel;
import Config.Message;
import Database.File.FileDao;
import Database.Mail.MailDao;
import Mail.Services.SubmitToLobMailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Handler;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class MailController {
  private MailDao mailDao;
  private FileDao fileDao;
  private String lobApiKey;

  public MailController(MailDao mailDao, FileDao fileDao, DeploymentLevel deploymentLevel) {
    this.mailDao = mailDao;
    this.fileDao = fileDao;
    if (deploymentLevel == DeploymentLevel.PRODUCTION
        || deploymentLevel == DeploymentLevel.STAGING) {
      this.lobApiKey = Objects.requireNonNull(System.getenv("LOB_API_KEY_PROD"));
    } else if (deploymentLevel == DeploymentLevel.TEST
        || deploymentLevel == DeploymentLevel.IN_MEMORY) {
      this.lobApiKey = Objects.requireNonNull(System.getenv("LOB_API_KEY_TEST"));
    }
  }

  public Handler getFormMailAddresses =
      ctx -> {
        FormMailAddress[] formMailAddresses = FormMailAddress.values();
        JSONObject response = new JSONObject();
        for (FormMailAddress address : formMailAddresses) {
          JSONObject addressJson = new JSONObject();
          addressJson.put("name", address.getName());
          addressJson.put("description", address.getDescription());
          addressJson.put("office_name", address.getOffice_name());
          addressJson.put("name_for_check", address.getNameForCheck());
          addressJson.put("street1", address.getStreet1());
          addressJson.put("street2", address.getStreet2());
          addressJson.put("city", address.getCity());
          addressJson.put("state", address.getState());
          addressJson.put("zipcode", address.getZipcode());
          addressJson.put("acceptable_states", address.getAcceptable_states());
          addressJson.put("acceptable_counties", address.getAcceptable_counties());
          response.put(address.name(), addressJson);
        }
        ctx.result(response.toString());
      };

  public Handler saveMail =
      ctx -> {
        JSONObject request = new JSONObject(ctx.body());
        ObjectMapper objectMapper = new ObjectMapper();
        String username = request.getString("username");
        String loggedInUser = ctx.sessionAttribute("username");

        System.out.println("ADDRESS: " + request.getJSONObject("mailAddress").toString());
        FormMailAddress formMailAddress = FormMailAddress.PA_DRIVERS_LICENSE;
        String fileId = request.getString("fileId");
        SubmitToLobMailService submitToLobMailService =
            new SubmitToLobMailService(
                fileDao, mailDao, formMailAddress, fileId, username, loggedInUser, lobApiKey, true);
        Message response = submitToLobMailService.executeAndGetResponse();
        ctx.result(response.toJSON().toString());
        //        } catch (Exception e) {
        //          Message response = MailMessage.FAILED_WHEN_MAPPING_FORM_MAIL_ADDRESS;
        //          ctx.result(response.toJSON().toString());
        //        }
      };
}
