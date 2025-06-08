package Mail;

import Mail.Services.SendgridService;


public class SendgridTest {
    public static void main(String[] args) {
        // System.out.println("SENDGRID_API_KEY = " + System.getenv("SENDGRID_API_KEY"));
        SendgridService.sendTestEmail();
    }
}

