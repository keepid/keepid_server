package Mail;

import Config.Message;
import org.json.JSONObject;

public enum MailMessage implements Message{
    MAIL_SUCCESS("MAIL_SUCCESS: mail successfully sent"),
    FAILED_WHEN_MAPPING_FORM_MAIL_ADDRESS("FAILED_WHEN_MAPPING_FORM_MAIL_ADDRESS: received form mail address" +
            "does not match with any in Form Mail Address"),
    FAILED_WHEN_SENDING_MAIL("FAILED_WHEN_SENDING_MAIL: failed when sending mail through lob");

    private String errorMessage;

    MailMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String toResponseString() {
        return toJSON().toString();
    }

    public Message withMessage(String message) {
        this.errorMessage = getErrorName() + ":" + message;
        return this;
    }

    public String getErrorName() {
        return this.errorMessage.split(":")[0];
    }

    public String getErrorDescription() {
        return this.errorMessage.split(":")[1];
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
