package User;

import Config.Message;
import org.json.JSONObject;

/** Message for Microsoft Login Requests. */
public enum MicrosoftLoginRequestMessage implements Message {
  REQUEST_SUCCESS("Successfully processed request"),
  INTERNAL_ERROR("Internal error processing request"),
  INVALID_REDIRECT_URI("Invalid redirect URI provided"),
  INVALID_ORIGIN_URI("Invalid origin URI provided");

  private final String message;

  MicrosoftLoginRequestMessage(String message) {
    this.message = message;
  }

  @Override
  public JSONObject toJSON() {
    JSONObject res = new JSONObject();
    res.put("status", this.name());
    res.put("message", this.message);
    return res;
  }

  @Override
  public JSONObject toJSON(String message) {
    JSONObject res = new JSONObject();
    res.put("status", this.name());
    res.put("message", message);
    return res;
  }

  @Override
  public String getErrorName() {
    return this.name();
  }

  @Override
  public String getErrorDescription() {
    return this.message;
  }

  @Override
  public String toResponseString() {
    return toJSON().toString();
  }
}
