package Packet;

import Config.Message;
import org.json.JSONObject;

public enum PacketMessage implements Message {
  INVALID_PARAMETER("INVALID_PARAMETER:Please check your parameter"),
  INVALID_FILE_TYPE("INVALID_FILE_TYPE:Invalid file type."),
  INSUFFICIENT_PRIVILEGE("INSUFFICIENT_PRIVILEGE:Privilege level too low."),
  NO_SUCH_FILE("NO_SUCH_FILE:File does not exist"),
  NO_SUCH_PACKET("NO_SUCH_PACKET:Packet does not exist"),
  SERVER_ERROR("SERVER_ERROR:There was an error with the server."),
  SUCCESS("SUCCESS:Success.");

  private final String errorMessage;

  PacketMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  public String toResponseString() {
    return toJSON().toString();
  }

  public String getErrorName() {
    return this.errorMessage.split(":")[0];
  }

  public String getErrorDescription() {
    return this.errorMessage.split(":")[1];
  }

  @Override
  public JSONObject toJSON() {
    return new JSONObject().put("status", getErrorName()).put("message", getErrorDescription());
  }

  @Override
  public JSONObject toJSON(String message) {
    return new JSONObject().put("status", getErrorName()).put("message", message);
  }
}
