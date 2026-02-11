package Security;

import Config.Message;
import org.json.JSONObject;

public enum EmailMessages implements Message {
  EMAIL_DOM_NOT_FOUND("EMAIL_DOM_NOT_FOUND: Can't locate target email in html"),
  HTML_NOT_FOUND("HTML_NOT_FOUND: Can't locate html needed for email"),
  RECEIVER_DOM_NOT_FOUND("RECEIVER_DOM_NOT_FOUND: Can't locate target name in html"),
  INVITER_DOM_NOT_FOUND("INVITER_DOM_NOT_FOUND: Can't locate sender name in html"),
  CODE_DOM_NOT_FOUND("CODE_DOM_NOT_FOUND: Can't locate verification code in html"),
  NOT_VALID_EMAIL("NOT_VALID_EMAIL: The email address isn't valid"),
  UNABLE_TO_SEND("UNABLE_TO_SEND: Failed to send emails"),
  SUCCESS("SUCCESS:Success.");

  private final String errorMessage;

  EmailMessages(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String toResponseString() {
    return toJSON().toString();
  }

  public String getErrorName() {
    String[] split = this.errorMessage.split(":", 2);
    return split[0].trim();
  }

  public String getErrorDescription() {
    String[] split = this.errorMessage.split(":", 2);
    if (split.length < 2) {
      return this.errorMessage;
    }
    return split[1].trim();
  }

  public JSONObject toJSON() {
    JSONObject res = new JSONObject();
    res.put("status", getErrorName());
    res.put("message", getErrorDescription());
    return res;
  }

  public JSONObject toJSON(String message) {
    JSONObject res = new JSONObject();
    res.put("status", getErrorName());
    res.put("message", message);
    return res;
  }
}
