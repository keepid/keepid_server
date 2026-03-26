package MailTest;

import static org.junit.Assert.*;

import Config.DeploymentLevel;
import Mail.LobMailSender;
import Mail.MailSender;
import Mail.MailSenderFactory;
import Mail.NoOpMailSender;
import org.junit.Test;

public class MailSenderFactoryUnitTests {

  @Test
  public void inMemory_createsNoOpMailSender() {
    MailSender sender = MailSenderFactory.create(DeploymentLevel.IN_MEMORY);
    assertNotNull(sender);
    assertTrue(sender instanceof NoOpMailSender);
  }
}
