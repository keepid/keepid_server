package MailTest;

import static org.junit.Assert.*;

import Mail.*;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class NoOpMailSenderUnitTests {
  private NoOpMailSender sender;

  @Before
  public void setUp() {
    sender = new NoOpMailSender();
  }

  @Test
  public void sendMail_returnsSyntheticResult() throws Exception {
    Mail mail = new Mail(
        new org.bson.types.ObjectId(),
        new FormMailAddress(),
        "targetUser",
        "requesterUser");

    MailResult result = sender.sendMail(mail, null, null, null);

    assertNotNull(result.getLobId());
    assertTrue(result.getLobId().startsWith("noop_"));
    assertNotNull(result.getLobCreatedAt());
    assertNotNull(result.getExpectedDeliveryDate());
    assertEquals("rendered", result.getLobStatus());
    assertEquals("letter", result.getMailType());
    assertNotNull(result.getTrackingEvents());
    assertTrue(result.getTrackingEvents().isEmpty());
  }

  @Test
  public void refreshStatus_returnsSyntheticDelivered() throws Exception {
    MailResult result = sender.refreshStatus("noop_abc123", false);

    assertNotNull(result);
    assertEquals("noop_abc123", result.getLobId());
    assertEquals("rendered", result.getLobStatus());
    assertNotNull(result.getExpectedDeliveryDate());

    List<TrackingEvent> events = result.getTrackingEvents();
    assertNotNull(events);
    assertEquals(1, events.size());
    assertEquals("Delivered", events.get(0).getType());
    assertEquals("Delivered", events.get(0).getName());
    assertNotNull(events.get(0).getTime());
    assertEquals("Philadelphia, PA", events.get(0).getLocation());
  }
}
