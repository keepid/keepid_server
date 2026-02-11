package Security;

public class NoOpEmailSender implements EmailSender {
  @Override
  public void sendEmail(String senderName, String recipientEmail, String subject, String message)
      throws EmailExceptions {
    // Intentionally no-op for test and local flows where outbound email is disabled.
  }
}
