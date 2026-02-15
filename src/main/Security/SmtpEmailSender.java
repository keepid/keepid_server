package Security;

public class SmtpEmailSender implements EmailSender {
  @Override
  public void sendEmail(String senderName, String recipientEmail, String subject, String message)
      throws EmailExceptions {
    EmailUtil.sendEmail(senderName, recipientEmail, subject, message);
  }
}
