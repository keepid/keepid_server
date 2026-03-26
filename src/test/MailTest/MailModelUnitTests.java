package MailTest;

import static org.junit.Assert.*;

import Mail.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.Test;

public class MailModelUnitTests {

  @Test
  public void constructor_setsCorrectDefaults() {
    Mail mail = new Mail(
        new ObjectId(),
        FormMailAddress.values()[0],
        "targetUser",
        "requesterUser");

    assertEquals(MailStatus.CREATED, mail.getMailStatus());
    assertNull(mail.getLobId());
    assertNull(mail.getLobCreatedAt());
    assertNotNull(mail.getTrackingEvents());
    assertTrue(mail.getTrackingEvents().isEmpty());
    assertNotNull(mail.getMailType());
  }

  @Test
  public void applyResult_populatesAllFields() {
    Mail mail = new Mail(
        new ObjectId(),
        FormMailAddress.values()[0],
        "target",
        "requester");

    MailResult result = new MailResult(
        "ltr_test_123",
        new Date(),
        "2026-04-01",
        "processed",
        new ArrayList<>(),
        "letter");

    mail.applyResult(result);

    assertEquals("ltr_test_123", mail.getLobId());
    assertNotNull(mail.getLobCreatedAt());
    assertEquals("2026-04-01", mail.getExpectedDeliveryDate());
    assertEquals("processed", mail.getLobStatus());
    assertEquals(MailStatus.MAILED, mail.getMailStatus());
    assertEquals("letter", mail.getMailType());
  }

  @Test
  public void applyRefreshResult_updatesStatusToDelivered() {
    Mail mail = new Mail(
        new ObjectId(),
        FormMailAddress.values()[0],
        "target",
        "requester");
    mail.setMailStatus(MailStatus.MAILED);

    List<TrackingEvent> events = new ArrayList<>();
    events.add(new TrackingEvent("In Transit", "In Transit", new Date(), "Philadelphia, PA"));
    events.add(new TrackingEvent("Delivered", "Delivered", new Date(), "New York, NY"));

    MailResult refresh = new MailResult();
    refresh.setLobStatus("rendered");
    refresh.setExpectedDeliveryDate("2026-04-01");
    refresh.setTrackingEvents(events);

    mail.applyRefreshResult(refresh);

    assertEquals(MailStatus.DELIVERED, mail.getMailStatus());
    assertEquals(2, mail.getTrackingEvents().size());
    assertEquals("2026-04-01", mail.getExpectedDeliveryDate());
  }

  @Test
  public void applyRefreshResult_updatesStatusToInTransit() {
    Mail mail = new Mail(
        new ObjectId(),
        FormMailAddress.values()[0],
        "target",
        "requester");
    mail.setMailStatus(MailStatus.MAILED);

    List<TrackingEvent> events = new ArrayList<>();
    events.add(new TrackingEvent("In Transit", "In Transit", new Date(), "Philadelphia, PA"));

    MailResult refresh = new MailResult();
    refresh.setLobStatus("rendered");
    refresh.setExpectedDeliveryDate("2026-04-05");
    refresh.setTrackingEvents(events);

    mail.applyRefreshResult(refresh);

    assertEquals(MailStatus.IN_TRANSIT, mail.getMailStatus());
  }

  @Test
  public void applyRefreshResult_noEventsLeavesStatusUnchanged() {
    Mail mail = new Mail(
        new ObjectId(),
        FormMailAddress.values()[0],
        "target",
        "requester");
    mail.setMailStatus(MailStatus.MAILED);

    MailResult refresh = new MailResult();
    refresh.setLobStatus("processed");
    refresh.setExpectedDeliveryDate("2026-04-03");
    refresh.setTrackingEvents(new ArrayList<>());

    mail.applyRefreshResult(refresh);

    assertEquals(MailStatus.MAILED, mail.getMailStatus());
  }
}
