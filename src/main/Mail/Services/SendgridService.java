package Mail.Services;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.Mail;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Optional;


import java.io.IOException;

public class SendgridService {
    private static final String SENDGRID_API_KEY =
        Optional.ofNullable(System.getenv("SENDGRID_API_KEY"))
            .orElseGet(() -> Dotenv.configure()
                .ignoreIfMissing()
                .load()
                .get("SENDGRID_API_KEY"));

    //private static final String SENDGRID_API_KEY = System.getenv("SENDGRID_API_KEY");

    private static void sendEmail(String toEmail, String subject, String bodyHtml) {
        Email from = new Email("vanessachung@keep.id");
        Email to = new Email(toEmail);
        Content content = new Content("text/html", bodyHtml);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(SENDGRID_API_KEY);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            if (response.getStatusCode() >= 400) {
                System.err.println("Email failed: " + response.getBody());
            }
        } catch (IOException ex) {
            System.err.println("SendGrid Exception: " + ex.getMessage());
        }
    }

    public static void sendWelcomeWithQuickStart(String username, String state) {
        String subject = "Welcome to Keep.id!";
        String body = "<p>Hi " + username + ", welcome! Here's how to get your ID in " + state + "...</p>";
        sendEmail(username, subject, body); // Replace with real email
    }

    public static void sendUploadReminder(String username, String docType) {
        String subject = "You uploaded a " + docType;
        String body = "<p>We’ve saved your " + docType + ". Here's what to upload next…</p>";
        sendEmail(username, subject, body);
    }

    public static void sendApplicationReminder(String username) {
        sendEmail(username , "Finish your application", "<p>You started an application. Come back and finish it!</p>");
    }

    public static void sendPickupInfo(String username, String nonprofit) {
        sendEmail(username , "Pick up your ID", "<p>Your documents are ready! Go to " + nonprofit + " to pick them up.</p>");
    }
    public static void sendTestEmail() {
        String subject = "Test from Keep.id";
        String body = "<p>Hello! This is a test email sent via SendGrid!</p>";
        sendEmail("vanessachung@keep.id", subject, body);
    }

}
