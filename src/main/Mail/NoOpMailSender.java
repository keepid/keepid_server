package Mail;

import Database.File.FileDao;
import Security.EncryptionController;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoOpMailSender implements MailSender {

  @Override
  public MailResult sendMail(
      Mail mail,
      FileDao fileDao,
      EncryptionController encryptionController,
      ReturnAddress returnAddress) {

    log.info(
        "NoOpMailSender: simulating mail for fileId={}, to={}",
        mail.getFileId(),
        mail.getMailingAddress().getName());

    boolean isCheck =
        mail.getMailingAddress().getMaybeCheckAmount().compareTo(BigDecimal.ZERO) > 0;

    MailResult result = new MailResult();
    result.setLobId("noop_" + UUID.randomUUID());
    result.setLobCreatedAt(new Date());
    result.setExpectedDeliveryDate(LocalDate.now().plusDays(5).toString());
    result.setLobStatus("rendered");
    result.setMailType(isCheck ? "check" : "letter");
    result.setTrackingEvents(new ArrayList<>());
    return result;
  }

  @Override
  public MailResult refreshStatus(String lobId, boolean isCheck) {
    log.info("NoOpMailSender: simulating status refresh for lobId={}", lobId);

    MailResult result = new MailResult();
    result.setLobId(lobId);
    result.setLobStatus("rendered");
    result.setExpectedDeliveryDate(LocalDate.now().plusDays(3).toString());
    result.setTrackingEvents(
        List.of(new TrackingEvent("Delivered", "Delivered", new Date(), "Philadelphia, PA")));
    return result;
  }
}
