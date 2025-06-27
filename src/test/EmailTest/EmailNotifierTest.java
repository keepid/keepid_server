package EmailTest;

import Mail.EmailNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import Activity.Activity;
import Mail.Services.SendgridService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.Arrays;
import static org.mockito.Mockito.*;

public class EmailNotifierTest {

    Activity activity;

    @BeforeEach
    public void setUp() {
        activity = new Activity();
        activity.setUsername("testuser");
    }

    @Test
    public void testCreateClientActivity() {
        activity.setType(Arrays.asList("Activity", "CreateClientActivity"));
        try (MockedStatic<SendgridService> mocked = mockStatic(SendgridService.class)) {
            EmailNotifier.handle(activity);
            mocked.verify(() -> SendgridService.handleCreateClientActivity("testuser", "PA"));
        }
    }

    @Test
    public void testUploadFileActivity() {
        activity.setType(Arrays.asList("Activity", "UploadFileActivity"));
        try (MockedStatic<SendgridService> mocked = mockStatic(SendgridService.class)) {
            EmailNotifier.handle(activity);
            mocked.verify(() -> SendgridService.handleUploadFileActivity("testuser", "DocumentType"));
        }
    }

    @Test
    public void testMailApplicationActivity() {
        activity.setType(Arrays.asList("Activity", "MailApplicationActivity"));
        try (MockedStatic<SendgridService> mocked = mockStatic(SendgridService.class)) {
            EmailNotifier.handle(activity);
            mocked.verify(() -> SendgridService.handleMailApplicationActivity("testuser"));
        }
    }

    @Test
    public void testSubmitApplicationActivity() {
        activity.setType(Arrays.asList("Activity", "SubmitApplicationActivity"));
        try (MockedStatic<SendgridService> mocked = mockStatic(SendgridService.class)) {
            EmailNotifier.handle(activity);
            mocked.verify(() -> SendgridService.handleSubmitApplicationActivity("testuser", "NonprofitName"));
        }
    }

    @Test
    public void testUnknownActivityType() {
        activity.setType(Arrays.asList("Activity", "SomethingElse"));
        try (MockedStatic<SendgridService> mocked = mockStatic(SendgridService.class)) {
            EmailNotifier.handle(activity);
            mocked.verifyNoInteractions();
        }
    }
}
