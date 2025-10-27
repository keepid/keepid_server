package User;

import Config.Message;
import org.json.JSONObject;

public enum GoogleLoginResponseMessage implements Message {
  AUTH_SUCCESS("Successfully processed Google login."),
  AUTH_FAILURE("Invalid Google response."),
  USER_NOT_FOUND("No user with associated account found."),
  INTERNAL_ERROR("Error occurred while processing.");

  private final String message;

  GoogleLoginResponseMessage(String message) {
    this.message = message;
  }

  @Override
  public JSONObject toJSON() {
    JSONObject res = new JSONObject();
    res.put("status", this.getErrorName());
    res.put("message", this.message);
    return res;
  }

  @Override
  public JSONObject toJSON(String message) {
    JSONObject res = new JSONObject();
    res.put("status", this.getErrorName());
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
    return this.toJSON().toString();
  }
}
