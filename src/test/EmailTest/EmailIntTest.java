package EmailTest;

import Activity.Activity;
import Mail.EmailNotifier;
import java.util.Arrays;
import org.junit.Test;

public class EmailIntTest {

    @Test
    public void testSendEmailsForAllActivities() {

        // change the email to test
        String testEmail = "vanessachung@keep.id";

        Activity createClient = new Activity();
        createClient.setUsername(testEmail);
        createClient.setType(Arrays.asList("Activity", "CreateClientActivity"));
        EmailNotifier.handle(createClient);

//        Activity uploadFile = new Activity();
//        uploadFile.setUsername(testEmail);
//        uploadFile.setType(Arrays.asList("Activity", "UploadFileActivity"));
//        EmailNotifier.handle(uploadFile);
//
//        Activity submitApp = new Activity();
//        submitApp.setUsername(testEmail);
//        submitApp.setType(Arrays.asList("Activity", "SubmitApplicationActivity"));
//        EmailNotifier.handle(submitApp);
//
//        Activity mailApp = new Activity();
//        mailApp.setUsername(testEmail);
//        mailApp.setType(Arrays.asList("Activity", "MailApplicationActivity"));
//        EmailNotifier.handle(mailApp);
    }
}

