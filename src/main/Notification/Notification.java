package Notification;

import java.time.LocalDateTime;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import org.json.JSONObject;

public class Notification implements Comparable<Notification> {
    private ObjectId id;

    @BsonProperty(value = "workerUsername")
    private String workerUsername;

    @BsonProperty(value = "clientUsername")
    private String clientUsername;

    @BsonProperty(value = "clientPhoneNumber")
    private String clientPhoneNumber;

    @BsonProperty(value = "message")
    private String message;

    @BsonProperty(value = "sentAt")
    private LocalDateTime sentAt;

    public Notification() {}

    public Notification(
            String workerUsername,
            String clientUsername,
            String clientPhoneNumber,
            String message) {
        this.workerUsername = workerUsername;
        this.clientUsername = clientUsername;
        this.clientPhoneNumber = clientPhoneNumber;
        this.message = message;
        this.sentAt = LocalDateTime.now();
    }

    public ObjectId getId() {
        return id;
    }

    public Notification setId(ObjectId id) {
        this.id = id;
        return this;
    }

    public String getWorkerUsername() {
        return workerUsername;
    }

    public Notification setWorkerUsername(String workerUsername) {
        this.workerUsername = workerUsername;
        return this;
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public Notification setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
        return this;
    }

    public String getClientPhoneNumber() {
        return clientPhoneNumber;
    }

    public Notification setClientPhoneNumber(String clientPhoneNumber) {
        this.clientPhoneNumber = clientPhoneNumber;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Notification setMessage(String message) {
        this.message = message;
        return this;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public Notification setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
        return this;
    }

    public JSONObject serialize() {
        JSONObject json = new JSONObject();
        json.put("_id", id != null ? id.toHexString() : null);
        json.put("workerUsername", workerUsername);
        json.put("clientUsername", clientUsername);
        json.put("clientPhoneNumber", clientPhoneNumber);
        json.put("message", message);
        json.put("sentAt", sentAt != null ? sentAt.toString() : null);
        return json;
    }

    @Override
    public int compareTo(Notification other) {
        if (this.sentAt == null && other.sentAt == null) return 0;
        if (this.sentAt == null) return 1;
        if (other.sentAt == null) return -1;
        return this.sentAt.compareTo(other.sentAt);
    }
}
