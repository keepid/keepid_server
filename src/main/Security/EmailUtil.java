package Security;

import Validation.ValidationUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;

public class EmailUtil {
  private static String passwordResetLinkEmailPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "main"
          + File.separator
          + "Security"
          + File.separator
          + "Resources"
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
          + "Resources"
          + File.separator
          + "organizationInviteEmail.html";

  public static void sendEmail(
      String senderName, String recipientEmail, String subject, String message)
      throws EmailExceptions {

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
    if (!ValidationUtils.isValidEmail(recipientEmail)) {
      throw new EmailExceptions(EmailMessages.NOT_VALID_EMAIL);
    }
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
    } catch (MessagingException | UnsupportedEncodingException e) {
      throw new EmailExceptions(EmailMessages.UNABLE_TO_SEND);
    }
  }

  public static String getPasswordResetEmail(String jwt) throws EmailExceptions {
    File passwordResetEmail = new File(passwordResetLinkEmailPath);
    try {
      Document htmlDoc = Jsoup.parse(passwordResetEmail, "UTF-8");
      Element targetElement = htmlDoc.getElementById("hrefTarget");
      if (targetElement != null) {
        targetElement.attr("href", jwt);
      } else {
        throw new EmailExceptions(EmailMessages.EMAIL_DOM_NOT_FOUND);
      }
      return htmlDoc.toString();
    } catch (FileNotFoundException e) {
      throw new EmailExceptions(EmailMessages.HTML_NOT_FOUND);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String getOrganizationInviteEmail(String jwt, String inviter, String receiver)
      throws EmailExceptions {
    try {
      File organizationInviteEmail = new File(organizationInviteEmailPath);
      Document htmlDoc = Jsoup.parse(organizationInviteEmail, "UTF-8");
      Element targetLink = htmlDoc.getElementById("hrefTarget");
      if (targetLink != null) {
        targetLink.attr("href", jwt);
      } else {
        throw new EmailExceptions(EmailMessages.EMAIL_DOM_NOT_FOUND);
      }
      Element targetName = htmlDoc.getElementById("targetName");
      if (targetName != null) {
        targetName.text(receiver);
      } else {
        throw new EmailExceptions(EmailMessages.RECEIVER_DOM_NOT_FOUND);
      }
      Element inviterName = htmlDoc.getElementById("inviterName");
      if (inviterName != null) {
        inviterName.text(inviter);
      } else {
        throw new EmailExceptions(EmailMessages.INVITER_DOM_NOT_FOUND);
      }
      return htmlDoc.toString();
    } catch (FileNotFoundException e) {
      throw new EmailExceptions(EmailMessages.HTML_NOT_FOUND);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String getAccountEmailChangedNotificationEmail() {
    return "<html><body style=\"font-family: Arial, sans-serif; line-height: 1.5; color: #222;\">"
        + "<h2>Keep.id account email updated</h2>"
        + "<p>An email has been added to your Keep.id account, or the email attached to your Keep.id account has changed.</p>"
        + "<p>How to log in:</p>"
        + "<ul>"
        + "<li>If this email is a Google account, you can use Google authentication.</li>"
        + "<li>If you do not want to use Google authentication, log in with your password.</li>"
        + "<li>If you do not remember your password, click <b>Forgot Password</b> on the login page.</li>"
        + "</ul>"
        + "<p>If you did not make this change, please contact your organization administrator immediately.</p>"
        + "</body></html>";
  }

  public static String getLoginInstructionsEmail() {
    return "<html><body style=\"font-family: Arial, sans-serif; line-height: 1.5; color: #222;\">"
        + "<h2>Keep.id login instructions</h2>"
        + "<p>You have a Keep.id account with this email.</p>"
        + "<p>How to log in:</p>"
        + "<ul>"
        + "<li>If this email is a Google account, you can use Google authentication.</li>"
        + "<li>If you do not want to use Google authentication, log in with your password.</li>"
        + "<li>If you do not remember your password, click <b>Forgot Password</b> on the login page.</li>"
        + "</ul>"
        + "<p>If you were not expecting this email, please contact your organization administrator.</p>"
        + "</body></html>";
  }

  public static String getEnrollmentWelcomeEmail(String firstName) {
    return "<html><body style=\"font-family: Arial, sans-serif; line-height: 1.5; color: #222;\">"
        + "<h2>Welcome to Keep.id</h2>"
        + "<p>Hi " + firstName + ",</p>"
        + "<p>An account has been created for you on Keep.id.</p>"
        + "<p>How to log in at <a href=\"https://keep.id\">keep.id</a>:</p>"
        + "<ul>"
        + "<li>If this email is a Google account, you can log in immediately using <b>Google authentication</b>.</li>"
        + "<li>To set a password, click <b>Forgot Password</b> on the login page and enter this email address. "
        + "You will receive a link to create your password.</li>"
        + "</ul>"
        + "<p>If you were not expecting this email, please contact your organization administrator.</p>"
        + "</body></html>";
  }
}
