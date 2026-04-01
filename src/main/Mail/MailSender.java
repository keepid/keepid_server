package Mail;

import Database.File.FileDao;
import Security.EncryptionController;

public interface MailSender {
  MailResult sendMail(
      Mail mail,
      FileDao fileDao,
      EncryptionController encryptionController,
      ReturnAddress returnAddress)
      throws Exception;

  MailResult refreshStatus(String lobId, boolean isCheck) throws Exception;
}
