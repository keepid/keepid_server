package MailTest;

import static org.junit.Assert.*;

import Config.DeploymentLevel;
import Database.Mail.MailDao;
import Database.Mail.MailDaoFactory;
import Mail.FormMailAddress;
import Mail.Mail;
import Mail.MailStatus;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MailDaoTestImplUnitTests {
  private MailDao mailDao;

  @Before
  public void setUp() {
    mailDao = MailDaoFactory.create(DeploymentLevel.IN_MEMORY);
  }

  @After
  public void tearDown() {
    mailDao.clear();
  }

  @Test
  public void saveAndGet() {
    ObjectId fileId = new ObjectId();
    Mail mail = new Mail(fileId, FormMailAddress.values()[0], "targetUser", "requesterUser");
    mailDao.save(mail);

    assertTrue(mailDao.get(mail.getId()).isPresent());
    assertEquals(mail.getId(), mailDao.get(mail.getId()).get().getId());
  }

  @Test
  public void getByFileId_returnsMatchingRecords() {
    ObjectId fileId1 = new ObjectId();
    ObjectId fileId2 = new ObjectId();

    Mail mail1 = new Mail(fileId1, FormMailAddress.values()[0], "user1", "requester");
    Mail mail2 = new Mail(fileId1, FormMailAddress.values()[0], "user1", "requester");
    Mail mail3 = new Mail(fileId2, FormMailAddress.values()[0], "user2", "requester");

    mailDao.save(mail1);
    mailDao.save(mail2);
    mailDao.save(mail3);

    List<Mail> result = mailDao.getByFileId(fileId1);
    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(m -> m.getFileId().equals(fileId1)));
  }

  @Test
  public void getByFileId_returnsEmptyForNonexistentFileId() {
    List<Mail> result = mailDao.getByFileId(new ObjectId());
    assertTrue(result.isEmpty());
  }

  @Test
  public void getByOrganization_returnsMatchingRecords() {
    ObjectId fileId = new ObjectId();
    Mail mail1 = new Mail(fileId, FormMailAddress.values()[0], "user", "requester");
    mail1.setOrganizationName("OrgA");
    Mail mail2 = new Mail(fileId, FormMailAddress.values()[0], "user", "requester");
    mail2.setOrganizationName("OrgA");
    Mail mail3 = new Mail(fileId, FormMailAddress.values()[0], "user", "requester");
    mail3.setOrganizationName("OrgB");

    mailDao.save(mail1);
    mailDao.save(mail2);
    mailDao.save(mail3);

    List<Mail> result = mailDao.getByOrganization("OrgA");
    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(m -> "OrgA".equals(m.getOrganizationName())));
  }

  @Test
  public void getByOrganization_withDateRange() {
    ObjectId fileId = new ObjectId();
    long now = System.currentTimeMillis();

    Mail mail1 = new Mail(fileId, FormMailAddress.values()[0], "user", "req");
    mail1.setOrganizationName("OrgA");
    mail1.setLobCreatedAt(new Date(now - 86400000L));

    Mail mail2 = new Mail(fileId, FormMailAddress.values()[0], "user", "req");
    mail2.setOrganizationName("OrgA");
    mail2.setLobCreatedAt(new Date(now - 86400000L * 60));

    mailDao.save(mail1);
    mailDao.save(mail2);

    Date from = new Date(now - 86400000L * 7);
    Date to = new Date(now);

    List<Mail> result = mailDao.getByOrganization("OrgA", from, to);
    assertEquals(1, result.size());
    assertEquals(mail1.getId(), result.get(0).getId());
  }

  @Test
  public void update_modifiesExistingRecord() {
    ObjectId fileId = new ObjectId();
    Mail mail = new Mail(fileId, FormMailAddress.values()[0], "user", "requester");
    mailDao.save(mail);

    mail.setMailStatus(MailStatus.MAILED);
    mail.setLobId("ltr_test_123");
    mailDao.update(mail);

    Mail updated = mailDao.get(mail.getId()).orElseThrow();
    assertEquals(MailStatus.MAILED, updated.getMailStatus());
    assertEquals("ltr_test_123", updated.getLobId());
  }

  @Test
  public void delete_removesRecord() {
    ObjectId fileId = new ObjectId();
    Mail mail = new Mail(fileId, FormMailAddress.values()[0], "user", "requester");
    mailDao.save(mail);
    assertEquals(1, mailDao.size());

    mailDao.delete(mail);
    assertEquals(0, mailDao.size());
  }
}
