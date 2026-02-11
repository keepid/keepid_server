package User;

import Config.Message;
import org.json.JSONObject;

public enum GoogleLoginResponseMessage implements Message {
  AUTH_SUCCESS("Successfully processed Google login."),
  AUTH_FAILURE("Google authentication failed. The response from Google could not be verified."),
  USER_NOT_FOUND("No Keep.id account found for this Google account. Please sign up with an organization first."),
  INTERNAL_ERROR("An internal error occurred while processing Google login. Please try again later.");

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
