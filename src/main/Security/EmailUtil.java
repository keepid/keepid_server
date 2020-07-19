package Security;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;

public class EmailUtil {
  private static String verificationCodeEmailPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "main"
          + File.separator
          + "Security"
          + File.separator
          + "verificationCodeEmail.html";

  private static String passwordResetLinkEmailPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "main"
          + File.separator
          + "Security"
          + File.separator
          + "passwordResetLinkEmail.html";

  private static String organizationInviteEmailPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "main"
          + File.separator
          + "Security"
          + File.separator
          + "organizationInviteEmail.html";

  public void sendEmail(String senderName, String recipientEmail, String subject, String message)
      throws UnsupportedEncodingException {

    // Set SMTP server properties.
    Properties properties = System.getProperties();
    properties.put("mail.smtp.host", Objects.requireNonNull(System.getenv("EMAIL_HOST")));
    properties.put("mail.smtp.port", Objects.requireNonNull(System.getenv("EMAIL_PORT")));
    properties.put("mail.smtp.auth", "true");
    properties.put(
        "mail.smtp.starttls.enable", "true"); // creates a new session with an authenticator

    Session session =
        Session.getDefaultInstance(
            properties,
            new javax.mail.Authenticator() {
              protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    Objects.requireNonNull(System.getenv("EMAIL_ADDRESS")),
                    Objects.requireNonNull(
                        System.getenv("EMAIL_PASSWORD"))); // Specify the Username and the PassWord
              }
            });

    // Creates a new Email message.
    try {
      Message msg = new MimeMessage(session);
      msg.setFrom(
          new InternetAddress(Objects.requireNonNull(System.getenv("EMAIL_ADDRESS")), senderName));
      InternetAddress[] toAddresses = {new InternetAddress(recipientEmail)};
      msg.setRecipients(Message.RecipientType.TO, toAddresses);
      msg.setSubject(subject);
      msg.setSentDate(new Date());
      msg.setContent(message, "text/html; charset=utf-8");

      // Send the Email.
      Transport.send(msg);
    } catch (MessagingException e) {
      e.printStackTrace();
    }
  }

  public String getVerificationCodeEmail(String verificationCode) {
    File verificationCodeEmail = new File(verificationCodeEmailPath);
    try {
      Document htmlDoc = Jsoup.parse(verificationCodeEmail, "UTF-8");
      Element targetElement = htmlDoc.getElementById("targetVerificationCode");
      targetElement.text(verificationCode);
      return htmlDoc.toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public String getPasswordResetEmail(String jwt) {
    File passwordResetEmail = new File(passwordResetLinkEmailPath);
    try {
      Document htmlDoc = Jsoup.parse(passwordResetEmail, "UTF-8");
      Element targetElement = htmlDoc.getElementById("hrefTarget");
      targetElement.attr("href", jwt);
      return htmlDoc.toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public String getOrganizationInviteEmail(String jwt, String inviter, String receiver) {
    File organizationInviteEmail = new File(organizationInviteEmailPath);
    try {
      Document htmlDoc = Jsoup.parse(organizationInviteEmail, "UTF-8");
      Element targetLink = htmlDoc.getElementById("hrefTarget");
      targetLink.attr("href", jwt);
      Element targetName = htmlDoc.getElementById("targetName");
      targetName.text(receiver);
      Element inviterName = htmlDoc.getElementById("inviterName");
      inviterName.text(inviter);

      return htmlDoc.toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
