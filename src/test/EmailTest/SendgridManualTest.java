package EmailTest;


import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
// ensure sendgrid API is connected
public class SendgridManualTest {
    public static void main(String[] args) {
        // manually load .env
        Dotenv dotenv = Dotenv.load();
        String SENDGRID_API_KEY = dotenv.get("SENDGRID_API_KEY");

        if (SENDGRID_API_KEY == null) {
            System.err.println("SENDGRID_API_KEY is missing or .env not loaded.");
            return;
        }

        Email from = new Email("vanessachung@keep.id"); // sender
        Email to = new Email("vanessa137222@gmail.com");
        String subject = "Manual Test Email from KeepID";
        Content content = new Content("text/plain", "This is a test email sent directly via SendGrid.");
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(SENDGRID_API_KEY);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());
        } catch (IOException ex) {
            System.err.println("Failed to send email: " + ex.getMessage());
        }
    }
}
