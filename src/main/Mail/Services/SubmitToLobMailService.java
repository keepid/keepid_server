package Mail.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Mail.MailDao;
import File.File;
import Mail.*;
import Packet.Packet;
import Packet.Services.RenderPacketPdfService;
import Security.EncryptionController;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubmitToLobMailService implements Service {

  /** Lob's hard limit for a single letter product (single-sided). */
  public static final int LOB_LETTER_MAX_PAGES = 60;

  /** Lob's hard limit for the attachment on a single check product. */
  public static final int LOB_CHECK_MAX_PAGES = 6;

  private final MailDao mailDao;
  private final Mail mail;
  private final FileDao fileDao;
  private final MailSender mailSender;
  private final EncryptionController encryptionController;
  private final ReturnAddress returnAddress;
  private final File applicationFile;
  private final Packet packet;

  public SubmitToLobMailService(
      FileDao fileDao,
      MailDao mailDao,
      MailSender mailSender,
      FormMailAddress formMailAddress,
      File applicationFile,
      Packet packet,
      String username,
      String loggedInUser,
      String organizationName,
      EncryptionController encryptionController,
      ReturnAddress returnAddress,
      int costCents) {
    if (applicationFile == null) {
      throw new IllegalArgumentException("applicationFile must not be null");
    }

    Mail mail = new Mail(applicationFile.getId(), formMailAddress, username, loggedInUser);
    mail.setOrganizationName(organizationName);
    mail.setCostCents(costCents);
    this.mail = mail;
    this.mailDao = mailDao;
    this.fileDao = fileDao;
    this.mailSender = mailSender;
    this.encryptionController = encryptionController;
    this.returnAddress = returnAddress;
    this.applicationFile = applicationFile;
    this.packet = packet;
    mailDao.save(mail);
  }

  @Override
  public Message executeAndGetResponse() throws Exception {
    try {
      byte[] renderedBytes =
          RenderPacketPdfService.render(
              applicationFile, packet, fileDao, encryptionController, mail.getTargetUsername());

      boolean isCheck =
          mail.getMailingAddress().getMaybeCheckAmount().compareTo(BigDecimal.ZERO) > 0;
      // Letters use Lob's address_placement=insert_blank_page (see LobMailSender) instead of
      // mutating PDF bytes here — keeps print/download identical to the mailed file payload.
      byte[] lobPdfBytes = renderedBytes;

      int pageCount = RenderPacketPdfService.countPages(lobPdfBytes);
      try {
        assertWithinPageLimit(pageCount, isCheck);
      } catch (IllegalStateException limitExceeded) {
        mail.setMailStatus(MailStatus.FAILED);
        mailDao.update(mail);
        throw limitExceeded;
      }

      MailResult result = mailSender.sendMail(mail, lobPdfBytes, returnAddress);
      mail.applyResult(result);
      mailDao.update(mail);
      return MailMessage.MAIL_SUCCESS;
    } catch (IllegalStateException e) {
      // Page-limit errors already recorded FAILED above; no extra state write here.
      throw e;
    } catch (Exception e) {
      log.error("Failed to send mail for fileId={}: {}", mail.getFileId(), e.getMessage(), e);
      mail.setMailStatus(MailStatus.FAILED);
      mailDao.update(mail);
      throw e;
    }
  }

  /**
   * Throws {@link IllegalStateException} with a caller-friendly message when {@code pageCount}
   * is outside Lob's limits for the product. Public for unit-testing in isolation from the
   * render/encryption pipeline.
   *
   * @param pageCount page count of the merged packet PDF uploaded to Lob (Lob may insert an
   *     additional address page for letters via {@code address_placement=insert_blank_page}).
   * @param isCheck {@code true} if the mailing is a check (6-page cap), {@code false} for a
   *     letter (60-page cap).
   */
  public static void assertWithinPageLimit(int pageCount, boolean isCheck) {
    int pageLimit = isCheck ? LOB_CHECK_MAX_PAGES : LOB_LETTER_MAX_PAGES;
    if (pageCount < 1 || pageCount > pageLimit) {
      String product = isCheck ? "check" : "letter";
      throw new IllegalStateException(
          String.format(
              "Packet exceeds Lob %s page limit: got %d page(s), max %d. Reduce attachments or split into multiple mailings.",
              product, pageCount, pageLimit));
    }
  }
}
