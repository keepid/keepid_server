package Notification;

import okhttp3.*;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import static Security.EnvUtil.requireEnv;

@Slf4j
public class WindmillNotificationClient {
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+1\\d{10}"); // +1 followed by 10 digits
    private static final Pattern EMAIL_PATTERN
            = Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9]([a-zA-Z0-9.\\-]*[a-zA-Z0-9])?\\.[a-zA-Z]{2,}$");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();
    private final String WINDMILL_URL;
    private final String WINDMILL_TOKEN;
    private final String TWILIO_PHONE_NUMBER;
    private final String KEEPID_EMAIL_ADDRESS;
    private final Map<String, String> twilioResource;
    private final Map<String, String> sendgridResource;

    public WindmillNotificationClient() {
        WINDMILL_URL = requireEnv("WINDMILL_URL").replaceAll("^\"|\"$", "").trim();
        WINDMILL_TOKEN = requireEnv("WINDMILL_TOKEN");
        TWILIO_PHONE_NUMBER = requireEnv("TWILIO_PHONE_NUMBER");
        KEEPID_EMAIL_ADDRESS = requireEnv("EMAIL_ADDRESS");

        twilioResource = Map.of(
                "accountSid", requireEnv("ACCOUNT_SID"),
                "token", requireEnv("AUTH_TOKEN_TWILIO")
        );

        sendgridResource = Map.of(
                "token", requireEnv("SENDGRID_API_KEY")
        );
    }

    // constructor for testing
    public WindmillNotificationClient(String windmillUrl, String windmillToken,
                                      String twilioPhoneNumber, String twilioAccountSid, String twilioAuthToken,
                                      String keepidEmailAddress, String sendgridToken) {
        this.WINDMILL_URL = windmillUrl;
        this.WINDMILL_TOKEN = windmillToken;
        this.TWILIO_PHONE_NUMBER = twilioPhoneNumber;
        this.KEEPID_EMAIL_ADDRESS = keepidEmailAddress;

        this.twilioResource = Map.of(
                "accountSid", twilioAccountSid,
                "token", twilioAuthToken
        );

        this.sendgridResource = Map.of(
                "token", sendgridToken
        );
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public void executeRequest(Request request) {
        Callback callback = new Callback() {
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("executeRequest failed: " + e.getMessage());
            }

            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.isSuccessful()) {
                    log.info("executeRequest completed successfully. Status: {}", response.code());
                } else {
                    log.warn("executeRequest completed but failed. Status: {}", response.code());
                }
            }
        };
        client.newCall(request).enqueue(callback);
    }

    public void sendSms(String to, String message) {
        if (!isValidPhoneNumber(to)) {
            log.error("sendSms failed: invalid phone number provided");
            return;
        }
        if (message == null || message.isBlank()) {
            log.error("sendSms failed: empty message provided");
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

        log.info("Sending SMS notification request to windmill webhook endpoint");

        executeRequest(request);
    }

    public void sendEmail(String toEmailAddress, String subject, String message, Optional<String> html) {
        if (!isValidEmail(toEmailAddress)) {
            log.error("sendEmail failed: invalid to email address provided");
            return;
        }
        if (subject == null || subject.isBlank()) {
            log.error("sendEmail failed: empty subject provided");
            return;
        }
        if (message == null || message.isBlank()) {
            log.error("sendEmail failed: empty message provided");
            return;
        }

        Map<String, Object>  emailConfig = new HashMap<>();
        emailConfig.put("sendgrid_auth", sendgridResource);
        emailConfig.put("from_email", this.KEEPID_EMAIL_ADDRESS);
        emailConfig.put("to_email", toEmailAddress);
        emailConfig.put("subject", subject);
        if (html != null && html.isPresent()) {
            emailConfig.put("html", html.get());
        }

        Map<String, Object> payload = Map.of(
                "method", "email",
                "message", message,
                "sms_config", Map.of(),
                "email_config", emailConfig
        );

        Request request = new Request.Builder()
                .url(this.WINDMILL_URL)
                .post(RequestBody.create(gson.toJson(payload), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + this.WINDMILL_TOKEN)
                .build();

        log.info("Sending email notification request to windmill webhook endpoint");

        executeRequest(request);
    }
}
