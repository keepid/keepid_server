package NotificationTest;

import Notification.WindmillNotificationClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

// Sends actual webhooks to windmill
// Need to set environment variables in run configuration in Intellij (look at WindmillNotificationClient.java)
class WindmillWebhookManualTest {

    @Test
    void sendSms() throws InterruptedException {
        WindmillNotificationClient client = new WindmillNotificationClient();

        System.out.println("Sending webhook...");
        assertDoesNotThrow(() -> {
            client.sendSms("TEST_PHONE_NUMBER_HERE", "Test payload");
        });
        System.out.println("Done! Check the webhook dashboard to verify receipt.");

        // wait for callback
        Thread.sleep(10000);
    }
}
