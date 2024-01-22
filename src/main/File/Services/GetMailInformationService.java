package File.Services;

import static io.javalin.Javalin.log;

import Config.Message;
import Config.Service;
import File.FileMessage;
import kong.unirest.json.JSONObject;

public class GetMailInformationService implements Service {
  private String username;
  private String organization;
  private String applicationType;
  private String price;
  private String mailAddress;
  private String returnAddress;

  public GetMailInformationService(String applicationType, String username, String organization) {
    this.applicationType = applicationType;
    this.username = username;
    this.organization = organization;
  }

  @Override
  public Message executeAndGetResponse() {
    try {
      this.mailAddress = "example street";
      this.returnAddress = "return street";
      this.price = "0.10";
      // Assuming Message is a type that can encapsulate a JSON response.
      return FileMessage.SUCCESS;
    } catch (Exception e) {
      log.error("Error in executeAndGetResponse: " + e.getMessage(), e);
      return FileMessage.SERVER_ERROR;
    }
  }

  public JSONObject getMailInformation() {
    JSONObject jsonResponse = new JSONObject();
    jsonResponse.put("price", "0.10");
    jsonResponse.put("mailAddress", "123 example street");
    jsonResponse.put("returnAddress", "456 return street");
    jsonResponse.put("status", "SUCCESS");
    return jsonResponse;
  }
}
