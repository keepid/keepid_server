package PDF;

import Config.Message;
import org.json.JSONObject;

public class PdfAnnotationError implements Message {
  final String errorName = "ANNOTATION_ERROR";
  String errorDescription;

  public PdfAnnotationError(String errorDescription) {
    this.errorDescription = errorDescription;
  }

  public String toResponseString() {
    return toJSON().toString();
  }

  public String getErrorName() {
    return this.errorName;
  }

  public String getErrorDescription() {
    return this.errorDescription;
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
