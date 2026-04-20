package NotificationTest;

import Notification.WindmillNotificationClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
public class WindmillNotificationClientTest {
    @Test
    public void sendSMSSuccess() {
        var testClient = new WindmillNotificationClient(
                "http://localhost", "test_windmill_token",
                "test_twilio_phone_number", "fake_twilio_resource",
                "fake_email", "fake_sendgrid_resource") {
            @Override
            public void executeRequest(Request request) {
                // Don't actually send, just verify the request looks right
                assertNotNull(request);
                assertEquals("POST", request.method());
            }
        };

        assertDoesNotThrow(() -> testClient.sendSms("+12025551234", "Test"));
    }

    @Test
    public void sendEmailSuccess() {
        var testClient = new WindmillNotificationClient(
                "http://localhost", "test_windmill_token",
                "test_twilio_phone_number", "fake_twilio_resource",
                "fake_email", "fake_sendgrid_resource") {
            @Override
            public void executeRequest(Request request) {
                // Don't actually send, just verify the request looks right
                assertNotNull(request);
                assertEquals("POST", request.method());
            }
        };

        assertDoesNotThrow(() -> testClient.sendEmail(
                "foo@example.com",
                "Test",
                "Test",
                Optional.of("<strong>Test</strong>")));
    }

    @Test
    public void testValidPhoneNumbers() {
        assertTrue(WindmillNotificationClient.isValidPhoneNumber("+12025551234"));
        assertTrue(WindmillNotificationClient.isValidPhoneNumber("+19999999999"));
    }

    @Test
    public void testInvalidPhoneNumbers() {
        assertFalse(WindmillNotificationClient.isValidPhoneNumber("12025551234")); // missing +
        assertFalse(WindmillNotificationClient.isValidPhoneNumber("+44123456789")); // wrong country code
        assertFalse(WindmillNotificationClient.isValidPhoneNumber("+1202555123")); // too few digits
        assertFalse(WindmillNotificationClient.isValidPhoneNumber("+120255512345")); // too many digits
        assertFalse(WindmillNotificationClient.isValidPhoneNumber(null));
    }

    @Test
    public void testValidEmails() {
        assertTrue(WindmillNotificationClient.isValidEmail("user@example.com"));
        assertTrue(WindmillNotificationClient.isValidEmail("user.name@example.com"));
        assertTrue(WindmillNotificationClient.isValidEmail("user+tag@example.com"));
        assertTrue(WindmillNotificationClient.isValidEmail("user_name@example.org"));
        assertTrue(WindmillNotificationClient.isValidEmail("user123@sub.domain.com"));
        assertTrue(WindmillNotificationClient.isValidEmail("u@example.io"));
        assertTrue(WindmillNotificationClient.isValidEmail("test@my-domain.co.uk"));
    }

    @Test
    public void testInvalidEmails() {
        assertFalse(WindmillNotificationClient.isValidEmail("user@"));
        assertFalse(WindmillNotificationClient.isValidEmail("@example.com"));
        assertFalse(WindmillNotificationClient.isValidEmail("userexample.com"));
        assertFalse(WindmillNotificationClient.isValidEmail("user@@example.com"));
        assertFalse(WindmillNotificationClient.isValidEmail("us er@example.com"));
        assertFalse(WindmillNotificationClient.isValidEmail("user@exa mple.com"));
        assertFalse(WindmillNotificationClient.isValidEmail("user@.com"));
        assertFalse(WindmillNotificationClient.isValidEmail("user@-example.com"));
        assertFalse(WindmillNotificationClient.isValidEmail("user@example.c"));
        assertFalse(WindmillNotificationClient.isValidEmail("user@example"));
        assertFalse(WindmillNotificationClient.isValidEmail(""));
        assertFalse(WindmillNotificationClient.isValidEmail(null));
    }
}
