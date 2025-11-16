package NotificationTest;

import Notification.WindmillNotificationClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Callback;
import okhttp3.Request;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
public class WindmillNotificationClientTest {

    WindmillNotificationClient client = new WindmillNotificationClient("http://localhost",
            "test_windmill_token", "test_twilio_phone_number",
            "test_twilio_account_sid", "test_twilio_auth_token");
    @Test
    public void sendSMSSuccess() {
        var testClient = new WindmillNotificationClient("http://localhost",
                "test_windmill_token", "test_twilio_phone_number",
                "test_twilio_account_sid", "test_twilio_auth_token") {
            @Override
            public void executeRequest(Request request, Callback callback) {
                // Don't actually send, just verify the request looks right
                assertNotNull(request);
                assertEquals("POST", request.method());
            }
        };

        assertDoesNotThrow(() -> testClient.sendSms("+12025551234", "Test"));
    }

    @Test
    public void testValidPhoneNumbers() {
        assertTrue(client.isValidPhoneNumber("+12025551234"));
        assertTrue(client.isValidPhoneNumber("+19999999999"));
    }

    @Test
    public void testInvalidPhoneNumbers() {
        assertFalse(client.isValidPhoneNumber("12025551234")); // missing +
        assertFalse(client.isValidPhoneNumber("+44123456789")); // wrong country code
        assertFalse(client.isValidPhoneNumber("+1202555123")); // too few digits
        assertFalse(client.isValidPhoneNumber("+120255512345")); // too many digits
        assertFalse(client.isValidPhoneNumber(null));
    }
}
