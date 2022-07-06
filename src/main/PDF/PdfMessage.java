package PDF;

import Config.Message;
import org.json.JSONObject;

public enum PdfMessage implements Message {
  INVALID_PDF_TYPE("INVALID_PDF_TYPE;Invalid PDF Type."),
  INVALID_PDF("INVALID_PDF;File is not a valid pdf"),
  INVALID_PRIVILEGE_TYPE("INVALID_PRIVILEGE_TYPE;The privilege type is invalid"),
  INVALID_PARAMETER("INVALID_PARAMETER;Please check your parameter"),
  SERVER_ERROR("SERVER_ERROR;There was an error with the server."),
  INSUFFICIENT_PRIVILEGE("INSUFFICIENT_PRIVILEGE;Privilege level too low."),
  SUCCESS("SUCCESS;Success."),
  NO_SUCH_FILE("NO_SUCH_FILE;PDF does not exist"),
  ENCRYPTION_ERROR("ENCRYPTION_ERROR;Error encrypting/decrypting");

  private String errorMessage;

  PdfMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String toResponseString() {
    return toJSON().toString();
  }

  public String getErrorName() {
    return this.errorMessage.split(";")[0];
  }

  public String getErrorDescription() {
    return this.errorMessage.split(";")[1];
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
