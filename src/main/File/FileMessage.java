package File;

import Config.Message;
import org.json.JSONObject;

public enum FileMessage implements Message {
  INVALID_FILE_TYPE("INVALID_FILE_TYPE:Invalid file type."),
  INVALID_FILE("INVALID_FILE:File is not valid."),
  INVALID_PRIVILEGE_TYPE("INVALID_PRIVILEGE_TYPE:The privilege type is invalid"),
  INVALID_PARAMETER("INVALID_PARAMETER:Please check your parameter"),
  SERVER_ERROR("SERVER_ERROR:There was an error with the server."),
  INSUFFICIENT_PRIVILEGE("INSUFFICIENT_PRIVILEGE:Privilege level too low."),
  SUCCESS("SUCCESS:Success."),
  NO_SUCH_FILE("NO_SUCH_FILE:PDF does not exist"),
  ENCRYPTION_ERROR("ENCRYPTION_ERROR:Error encrypting/decrypting"),
  FILE_EXISTS("FILE_EXISTS: File already exists!");

  private String errorMessage;
  private String fileId; // optional

  FileMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  FileMessage(String errorMessage, String fileId) {
    this.errorMessage = errorMessage;
    this.fileId = fileId;
  }

  public String toResponseString() {
    return toJSON().toString();
  }

  public String getErrorName() {
    return this.errorMessage.split(":")[0];
  }

  public String getErrorDescription() {
    return this.errorMessage.split(":")[1];
  }

  public String getFileId() {
    return this.fileId;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  public JSONObject toJSON() {
    JSONObject res = new JSONObject();
    res.put("status", getErrorName());
    res.put("message", getErrorDescription());
    if (this.fileId != null) {
      res.put("fileId", getFileId());
    }
    return res;
  }

  public JSONObject toJSON(String message) {
    JSONObject res = new JSONObject();
    res.put("status", getErrorName());
    res.put("message", message);
    if (this.fileId != null) {
      res.put("fileId", getFileId());
    }
    return res;
  }
}
