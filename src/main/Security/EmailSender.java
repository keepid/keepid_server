package Security;

public interface EmailSender {
  void sendEmail(String senderName, String recipientEmail, String subject, String message)
      throws EmailExceptions;
}
