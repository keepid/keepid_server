package Mail;

import Config.DeploymentLevel;
import Config.Message;
import Database.File.FileDao;
import Database.Mail.MailDao;
import Mail.Services.SubmitToLobMailService;
import Security.EncryptionController;
import io.javalin.http.Handler;
import java.util.Arrays;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class MailController {
  private MailDao mailDao;
  private FileDao fileDao;
  private String lobApiKey;
  private EncryptionController encryptionController;

  public MailController(
      MailDao mailDao,
      FileDao fileDao,
      EncryptionController encryptionController,
      DeploymentLevel deploymentLevel) {
    this.mailDao = mailDao;
    this.fileDao = fileDao;
    this.encryptionController = encryptionController;
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
          addressJson.put("check_amount", address.getMaybeCheckAmount().toString());
          addressJson.put("acceptable_states", address.getAcceptable_states());
          addressJson.put("acceptable_counties", address.getAcceptable_counties());
          response.put(address.name(), addressJson);
        }
        ctx.result(response.toString());
      };

  public Handler saveMail =
      ctx -> {
        JSONObject request = new JSONObject(ctx.body());
        String username = request.getString("username");
        String loggedInUser = ctx.sessionAttribute("username");
        String formMailAddressString = request.getString("mailKey");
        FormMailAddress formMailAddress =
            Arrays.stream(FormMailAddress.values())
                .filter(e -> e.name().equalsIgnoreCase(formMailAddressString))
                .findFirst()
                .orElseThrow();
        System.out.println("ADDRESS: " + formMailAddress);
        String fileId = request.getString("fileId");
        SubmitToLobMailService submitToLobMailService =
            new SubmitToLobMailService(
                fileDao,
                mailDao,
                formMailAddress,
                fileId,
                username,
                loggedInUser,
                lobApiKey,
                encryptionController);
        Message response = submitToLobMailService.executeAndGetResponse();
        ctx.result(response.toJSON().toString());
      };
}
