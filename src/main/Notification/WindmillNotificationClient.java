package Notification;

import okhttp3.*;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class WindmillNotificationClient {
    private final OkHttpClient client;
    private final Gson gson;
    private final String WINDMILL_URL;
    private final String WINDMILL_TOKEN;
    private final String TWILIO_PHONE_NUMBER;
    private final Map<String, String> twilioResource;
    private final Pattern PHONE_PATTERN = Pattern.compile("\\+1\\d{10}"); // +1 followed by 10 digits

    public WindmillNotificationClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.WINDMILL_URL = System.getenv("WINDMILL_URL");
        this.WINDMILL_TOKEN = System.getenv("WINDMILL_TOKEN");
        this.TWILIO_PHONE_NUMBER = System.getenv("TWILIO_PHONE_NUMBER");
        this.twilioResource = new HashMap<>();
        String TWILIO_ACCOUNT_SID = System.getenv("ACCOUNT_SID");
        String TWILIO_AUTH_TOKEN = System.getenv("AUTH_TOKEN_TWILIO");
        this.twilioResource.put("accountSid", TWILIO_ACCOUNT_SID);
        this.twilioResource.put("token", TWILIO_AUTH_TOKEN);
    }

    // Constructor for testing
    public WindmillNotificationClient(String windmillUrl, String windmillToken, String twilioPhoneNumber,
                                      String twilioAccountSid, String twilioAuthToken) {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.WINDMILL_URL = windmillUrl;
        this.WINDMILL_TOKEN = windmillToken;
        this.TWILIO_PHONE_NUMBER = twilioPhoneNumber;
        this.twilioResource = new HashMap<>();
        this.twilioResource.put("accountSid", twilioAccountSid);
        this.twilioResource.put("token", twilioAuthToken);
    }

    public boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && this.PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    public void executeRequest(Request request, Callback callback) {
        client.newCall(request).enqueue(callback);
    }

    public void sendSms(String to, String message) {
        if (!isValidPhoneNumber(to)) {
            log.error("sendSms failed: invalid phone number provided: {}", to);
            return;
        }
        if (message == null || message.isBlank()) {
            log.error("sendSms failed: empty message provided: {}", message);
            return;
        }

        Map<String, Object> payload = Map.of(
                "method", "sms",
                "message", message,
                "sms_config", Map.of(
                        "twilio_auth", twilioResource,
                        "to_phone_number", to,
                        "from_phone_number", this.TWILIO_PHONE_NUMBER
                ),
                "email_config", Map.of()
        );

        Request request = new Request.Builder()
                .url(this.WINDMILL_URL)
                .post(RequestBody.create(gson.toJson(payload), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + this.WINDMILL_TOKEN)
                .build();

        log.info("Sending SMS to {} with message: {}", to, message);

        Callback callback = new Callback() {
            public void onFailure(@NotNull Call call, @NotNull java.io.IOException e) {
                log.error("sendSms failed: " + e.getMessage());
            }

            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (response) {
                    if (response.isSuccessful()) {
                        log.info("sent SMS successfully. Status: {}", response.code());
                    } else {
                        log.warn("SMS request completed but failed. Status: {}, Body: {}",
                                response.code(), response.body() != null ? response.body().string() : "");
                    }
                } catch (IOException e) {
                    log.error("caught error reading SMS response: " + e.getMessage());
                }
            }
        };
        executeRequest(request, callback);
    }
}
