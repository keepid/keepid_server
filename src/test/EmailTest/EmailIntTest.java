package EmailTest;

import Activity.Activity;
import Mail.EmailNotifier;
import java.util.Arrays;
import org.junit.Test;

public class EmailIntTest {

    @Test
    public void testSendEmailsForAllActivities() {
        String testEmail = "vanessa137222@gmail.com";

        Activity createClient = new Activity();
        createClient.setUsername(testEmail);
        createClient.setType(Arrays.asList("Activity", "CreateClientActivity"));
        EmailNotifier.handle(createClient);

        Activity uploadFile = new Activity();
        uploadFile.setUsername(testEmail);
        uploadFile.setType(Arrays.asList("Activity", "UploadFileActivity"));
        EmailNotifier.handle(uploadFile);

        Activity startApp = new Activity();
        startApp.setUsername(testEmail);
        startApp.setType(Arrays.asList("Activity", "StartApplicationActivity"));
        EmailNotifier.handle(startApp);

        Activity mailApp = new Activity();
        mailApp.setUsername(testEmail);
        mailApp.setType(Arrays.asList("Activity", "MailApplicationActivity"));
        EmailNotifier.handle(mailApp);
    }
}

