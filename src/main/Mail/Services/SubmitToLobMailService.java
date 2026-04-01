package Mail.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Mail.MailDao;
import Mail.*;
import Security.EncryptionController;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

@Slf4j
public class SubmitToLobMailService implements Service {
  private final MailDao mailDao;
  private final Mail mail;
  private final FileDao fileDao;
  private final MailSender mailSender;
  private final EncryptionController encryptionController;
  private final ReturnAddress returnAddress;

  public SubmitToLobMailService(
      FileDao fileDao,
      MailDao mailDao,
      MailSender mailSender,
      FormMailAddress formMailAddress,
      String fileId,
      String username,
      String loggedInUser,
      String organizationName,
      EncryptionController encryptionController,
      ReturnAddress returnAddress,
      int costCents) {

    Mail mail = new Mail(new ObjectId(fileId), formMailAddress, username, loggedInUser);
    mail.setOrganizationName(organizationName);
    mail.setCostCents(costCents);
    this.mail = mail;
    this.mailDao = mailDao;
    this.fileDao = fileDao;
    this.mailSender = mailSender;
    this.encryptionController = encryptionController;
    this.returnAddress = returnAddress;
    mailDao.save(mail);
  }

  @Override
  public Message executeAndGetResponse() throws Exception {
    try {
      MailResult result = mailSender.sendMail(mail, fileDao, encryptionController, returnAddress);
      mail.applyResult(result);
      mailDao.update(mail);
      return MailMessage.MAIL_SUCCESS;
    } catch (Exception e) {
      log.error("Failed to send mail for fileId={}: {}", mail.getFileId(), e.getMessage(), e);
      mail.setMailStatus(MailStatus.FAILED);
      mailDao.update(mail);
      throw e;
    }
  }
}
