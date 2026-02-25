package UserTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import User.Address;
import User.Services.UpdateProfileFromFormService;
import User.User;
import User.UserMessage;
import User.UserType;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

public class UpdateProfileFromFormServiceTest {
  private final UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);

  @After
  public void reset() {
    userDao.clear();
  }

  @Test
  public void updatesClientProfileFromAnnotatedFormAnswers() {
    EntityFactory.createUser()
        .withUsername("client1")
        .withFirstName("Old")
        .withLastName("Name")
        .withEmail("old@example.com")
        .withPhoneNumber("1112223333")
        .withAddress("100 Old St")
        .withCity("Old City")
        .withState("PA")
        .withZipcode("19104")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject formAnswers = new JSONObject();
    formAnswers.put("First Name:firstName", "Jane");
    formAnswers.put("Last Name:currentName.last", "Doe");
    formAnswers.put("DOB:birthDate", "01/25/1990");
    formAnswers.put("Phone:phone", "2155551212");
    formAnswers.put("Street:personalAddress.line1", "456 New St");
    formAnswers.put("City:city", "Philadelphia");
    formAnswers.put("State:state", "PA");
    formAnswers.put("Zip:zipcode", "19107");

    UpdateProfileFromFormService service =
        new UpdateProfileFromFormService(userDao, "client1", formAnswers);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    User updated = userDao.get("client1").orElse(null);
    assertNotNull(updated);
    assertEquals("Jane", updated.getCurrentName().getFirst());
    assertEquals("Doe", updated.getCurrentName().getLast());
    assertEquals("01-25-1990", updated.getBirthDate());
    assertEquals("2155551212", updated.getPhone());
    assertEquals("456 New St", updated.getPersonalAddress().getLine1());
    assertEquals("Philadelphia", updated.getPersonalAddress().getCity());
    assertEquals("PA", updated.getPersonalAddress().getState());
    assertEquals("19107", updated.getPersonalAddress().getZip());
  }

  @Test
  public void ignoresWorkerOrgAndEmptyValues() {
    EntityFactory.createUser()
        .withUsername("client1")
        .withFirstName("Existing")
        .withEmail("existing@example.com")
        .withAddress("100 Keep St")
        .withCity("Keep City")
        .withState("PA")
        .withZipcode("19104")
        .withUserType(UserType.Client)
        .buildAndPersist(userDao);

    JSONObject formAnswers = new JSONObject();
    formAnswers.put("Worker Email:worker.email", "worker@example.com");
    formAnswers.put("Agency Name:org.name", "My Org");
    formAnswers.put("Email:email", "");
    formAnswers.put("City:personalAddress.city", ""); // Empty should preserve existing.
    formAnswers.put("Client First Name:client.currentName.first", "Updated");

    UpdateProfileFromFormService service =
        new UpdateProfileFromFormService(userDao, "client1", formAnswers);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    User updated = userDao.get("client1").orElse(null);
    assertNotNull(updated);
    assertEquals("Updated", updated.getCurrentName().getFirst());
    assertEquals("existing@example.com", updated.getEmail());
    assertEquals("Keep City", updated.getPersonalAddress().getCity());
  }

  @Test
  public void updatesNestedNameAndMailingFields() {
    User seeded =
        EntityFactory.createUser()
            .withUsername("client1")
            .withFirstName("Alpha")
            .withLastName("Beta")
            .withUserType(UserType.Client)
            .buildAndPersist(userDao);
    seeded.setMailAddress(new Address("10 Old Mail", null, "Oldtown", "PA", "19000", null));
    userDao.update(seeded);

    JSONObject formAnswers = new JSONObject();
    formAnswers.put("Mother Maiden:motherName.maiden", "Smith");
    formAnswers.put("Father First:fatherName.first", "Robert");
    formAnswers.put("Mail City:mailAddress.city", "Newtown");
    formAnswers.put("Former Name:nameHistory.0.first", "Legacy");

    UpdateProfileFromFormService service =
        new UpdateProfileFromFormService(userDao, "client1", formAnswers);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    User updated = userDao.get("client1").orElse(null);
    assertNotNull(updated);
    assertEquals("Smith", updated.getMotherName().getMaiden());
    assertEquals("Robert", updated.getFatherName().getFirst());
    assertEquals("Newtown", updated.getMailAddress().getCity());
    assertEquals("Legacy", updated.getNameHistory().get(0).getFirst());
  }
}
