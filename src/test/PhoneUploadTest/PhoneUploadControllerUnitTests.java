package PhoneUploadTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import Config.DeploymentLevel;
import Database.PhoneUpload.PhoneUploadSessionDao;
import Database.PhoneUpload.PhoneUploadSessionDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import Notification.WindmillNotificationClient;
import PhoneUpload.PhoneUploadController;
import PhoneUpload.PhoneUploadSession;
import PhoneUpload.PhoneUploadTokenUtil;
import java.util.Date;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PhoneUploadControllerUnitTests {
  private UserDao userDao;
  private PhoneUploadSessionDao phoneUploadSessionDao;
  private PhoneUploadController controller;

  @Before
  public void setUp() {
    userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);
    phoneUploadSessionDao = PhoneUploadSessionDaoFactory.create(DeploymentLevel.IN_MEMORY);
    WindmillNotificationClient notificationClient =
        new WindmillNotificationClient(
            "http://localhost",
            "token",
            "+10000000000",
            "twilio-res",
            "test@example.com",
            "sendgrid-res");
    controller = new PhoneUploadController(userDao, phoneUploadSessionDao, notificationClient);
  }

  @After
  public void tearDown() {
    userDao.clear();
    phoneUploadSessionDao.clear();
  }

  @Test
  public void getOpenSessionReturnsPresentForValidToken() {
    String rawToken = PhoneUploadTokenUtil.generateRawToken();
    PhoneUploadSession session =
        new PhoneUploadSession()
            .setTokenHash(PhoneUploadTokenUtil.hashToken(rawToken))
            .setActorUsername("worker1")
            .setTargetClientUsername("client1")
            .setIdCategory("Drivers License / Photo ID")
            .setCreatedAt(new Date(System.currentTimeMillis() - 1_000))
            .setExpiresAt(new Date(System.currentTimeMillis() + 60_000));
    phoneUploadSessionDao.save(session);

    Optional<PhoneUploadSession> open = controller.getOpenSession(rawToken, new Date());
    assertTrue(open.isPresent());
  }

  @Test
  public void getOpenSessionRejectsExpiredToken() {
    String rawToken = PhoneUploadTokenUtil.generateRawToken();
    PhoneUploadSession session =
        new PhoneUploadSession()
            .setTokenHash(PhoneUploadTokenUtil.hashToken(rawToken))
            .setActorUsername("worker1")
            .setTargetClientUsername("client1")
            .setIdCategory("Drivers License / Photo ID")
            .setCreatedAt(new Date(System.currentTimeMillis() - 120_000))
            .setExpiresAt(new Date(System.currentTimeMillis() - 60_000));
    phoneUploadSessionDao.save(session);

    Optional<PhoneUploadSession> open = controller.getOpenSession(rawToken, new Date());
    assertFalse(open.isPresent());
  }

  @Test
  public void getOpenSessionRejectsClosedToken() {
    String rawToken = PhoneUploadTokenUtil.generateRawToken();
    PhoneUploadSession session =
        new PhoneUploadSession()
            .setTokenHash(PhoneUploadTokenUtil.hashToken(rawToken))
            .setActorUsername("worker1")
            .setTargetClientUsername("client1")
            .setIdCategory("Drivers License / Photo ID")
            .setCreatedAt(new Date(System.currentTimeMillis() - 1_000))
            .setExpiresAt(new Date(System.currentTimeMillis() + 60_000))
            .setClosedAt(new Date());
    phoneUploadSessionDao.save(session);

    Optional<PhoneUploadSession> open = controller.getOpenSession(rawToken, new Date());
    assertFalse(open.isPresent());
  }

  @Test
  public void normalizeUsPhoneToE164AcceptsCommonFormats() {
    assertEquals("+13108179067", PhoneUploadController.normalizeUsPhoneToE164("3108179067"));
    assertEquals("+13108179067", PhoneUploadController.normalizeUsPhoneToE164("1-310-817-9067"));
    assertEquals("+13108179067", PhoneUploadController.normalizeUsPhoneToE164("(310) 817-9067"));
    assertEquals("+13108179067", PhoneUploadController.normalizeUsPhoneToE164("+1 310 817 9067"));
    assertEquals(null, PhoneUploadController.normalizeUsPhoneToE164("310817906"));
  }
}
