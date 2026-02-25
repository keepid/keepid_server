package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import User.PhoneBookEntry;
import User.Services.PhoneBookService;
import User.User;
import User.UserMessage;
import User.UserType;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertNull;

import java.util.List;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PhoneBookServiceTest {
  UserDao userDao = UserDaoFactory.create(DeploymentLevel.IN_MEMORY);

  @After
  public void reset() {
    if (userDao != null) {
      userDao.clear();
    }
  }

  // ---- GET ----

  @Test
  public void getPhoneBookReturnsEmptyForUserWithNoPhone() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.get(userDao, "testuser");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertTrue(service.getResultPhoneBook().isEmpty());
  }

  @Test
  public void getPhoneBookReturnsPrimaryForUserWithPhone() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.get(userDao, "testuser");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    List<PhoneBookEntry> book = service.getResultPhoneBook();
    assertEquals(1, book.size());
    assertEquals("primary", book.get(0).getLabel());
    assertEquals("6305264087", book.get(0).getPhoneNumber());
    assertTrue(book.get(0).hasPrimaryLabel());
  }

  @Test
  public void getPhoneBookReturnsEmptyWhenPhoneBookNull() {
    User user = EntityFactory.createUser()
        .withUsername("legacyuser")
        .withPhoneNumber("5551234567")
        .build();
    user.setPhoneBook(null);
    userDao.save(user);

    PhoneBookService service = PhoneBookService.get(userDao, "legacyuser");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    List<PhoneBookEntry> book = service.getResultPhoneBook();
    assertEquals(0, book.size());
  }

  @Test
  public void getPhoneBookFailsForNonexistentUser() {
    PhoneBookService service = PhoneBookService.get(userDao, "nobody");
    Message response = service.executeAndGetResponse();
    assertEquals(UserMessage.USER_NOT_FOUND, response);
  }

  @Test
  public void getPhoneBookFailsForNullUsername() {
    PhoneBookService service = PhoneBookService.get(userDao, null);
    Message response = service.executeAndGetResponse();
    assertEquals(UserMessage.SESSION_TOKEN_FAILURE, response);
  }

  // ---- ADD ----

  @Test
  public void addFirstEntryToEmptyPhoneBookBecomesPrimary() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.add(userDao, "testuser", "personal", "4103022342");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    List<PhoneBookEntry> book = service.getResultPhoneBook();
    assertEquals(1, book.size());
    assertEquals("primary", book.get(0).getLabel());
    assertTrue(book.get(0).hasPrimaryLabel());
    assertEquals("4103022342", book.get(0).getPhoneNumber());

    User user = userDao.get("testuser").orElse(null);
    assertNotNull(user);
    assertEquals("4103022342", user.getPhone());
  }

  @Test
  public void addEntryAppendsNonPrimary() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.add(userDao, "testuser", "daughter", "4103022342");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    List<PhoneBookEntry> book = service.getResultPhoneBook();
    assertEquals(2, book.size());

    PhoneBookEntry added = book.get(1);
    assertEquals("daughter", added.getLabel());
    assertEquals("4103022342", added.getPhoneNumber());
    assertFalse(added.hasPrimaryLabel());
  }

  @Test
  public void addEntryRejectsPrimaryLabel() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.add(userDao, "testuser", "primary", "4103022342");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void addEntryRejectsDuplicatePhoneNumber() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.add(userDao, "testuser", "duplicate", "6305264087");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void addEntryRejectsInvalidPhoneNumber() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.add(userDao, "testuser", "bad", "123");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void addEntryRejectsEmptyLabel() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.add(userDao, "testuser", "", "4103022342");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.EMPTY_FIELD, response);
  }

  @Test
  public void addEntryStripsLabelWhitespace() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.add(userDao, "testuser", "  shelter front desk  ", "4103022342");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertEquals("shelter front desk", service.getResultPhoneBook().get(1).getLabel());
  }

  // ---- UPDATE ----

  @Test
  public void updateEntryChangesLabel() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);
    PhoneBookService.add(userDao, "testuser", "daughter", "4103022342").executeAndGetResponse();

    PhoneBookService service = PhoneBookService.update(userDao, "testuser", "4103022342", "son", null);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    PhoneBookEntry updated = service.getResultPhoneBook().stream()
        .filter(e -> e.getPhoneNumber().equals("4103022342"))
        .findFirst()
        .orElse(null);
    assertNotNull(updated);
    assertEquals("son", updated.getLabel());
  }

  @Test
  public void updatePrimaryLabelIsRejected() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.update(userDao, "testuser", "6305264087", "personal", null);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void updateLabelToPrimaryIsRejected() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);
    PhoneBookService.add(userDao, "testuser", "daughter", "4103022342").executeAndGetResponse();

    PhoneBookService service = PhoneBookService.update(userDao, "testuser", "4103022342", "primary", null);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void updateEntryChangesPhoneNumber() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);
    PhoneBookService.add(userDao, "testuser", "daughter", "4103022342").executeAndGetResponse();

    PhoneBookService service = PhoneBookService.update(userDao, "testuser", "4103022342", null, "2155551234");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    PhoneBookEntry updated = service.getResultPhoneBook().stream()
        .filter(e -> e.getPhoneNumber().equals("2155551234"))
        .findFirst()
        .orElse(null);
    assertNotNull(updated);
    assertEquals("daughter", updated.getLabel());
  }

  @Test
  public void updatePrimaryPhoneReflectsInGetPhone() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.update(userDao, "testuser", "6305264087", null, "9998887777");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);

    User user = userDao.get("testuser").orElse(null);
    assertNotNull(user);
    assertEquals("9998887777", user.getPhone());
  }

  @Test
  public void updateEntryRejectsDuplicateNewPhone() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);
    PhoneBookService.add(userDao, "testuser", "daughter", "4103022342").executeAndGetResponse();

    PhoneBookService service = PhoneBookService.update(userDao, "testuser", "4103022342", null, "6305264087");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void updateEntryRejectsNonexistentPhone() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.update(userDao, "testuser", "0000000000", "label", null);
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  // ---- DELETE ----

  @Test
  public void deleteEntryRemovesNonPrimary() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);
    PhoneBookService.add(userDao, "testuser", "daughter", "4103022342").executeAndGetResponse();

    PhoneBookService service = PhoneBookService.delete(userDao, "testuser", "4103022342");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertEquals(1, service.getResultPhoneBook().size());
    assertEquals("6305264087", service.getResultPhoneBook().get(0).getPhoneNumber());
  }

  @Test
  public void deleteEntryRejectsPrimary() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.delete(userDao, "testuser", "6305264087");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void deleteEntryRejectsNonexistentPhone() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.delete(userDao, "testuser", "0000000000");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  // ---- Phone number normalization ----

  @Test
  public void addEntryNormalizesPhoneWithCountryCode() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.add(userDao, "testuser", "dup", "16305264087");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.INVALID_PARAMETER, response);
  }

  @Test
  public void addEntryAcceptsFormattedPhoneAndStoresAs10Digits() {
    EntityFactory.createUser()
        .withUsername("testuser")
        .withPhoneNumber("6305264087")
        .buildAndPersist(userDao);

    PhoneBookService service = PhoneBookService.add(userDao, "testuser", "work", "(215)555-1234");
    Message response = service.executeAndGetResponse();

    assertEquals(UserMessage.SUCCESS, response);
    assertEquals(2, service.getResultPhoneBook().size());
    assertEquals("2155551234", service.getResultPhoneBook().get(1).getPhoneNumber());
  }

  // ---- User constructor phoneBook initialization ----

  @Test
  public void userConstructorInitializesPhoneBookWithPrimaryEntry() {
    User user = EntityFactory.createUser()
        .withPhoneNumber("6305264087")
        .build();

    assertNotNull(user.getPhoneBook());
    assertEquals(1, user.getPhoneBook().size());
    assertEquals("primary", user.getPhoneBook().get(0).getLabel());
    assertEquals("6305264087", user.getPhoneBook().get(0).getPhoneNumber());
    assertTrue(user.getPhoneBook().get(0).hasPrimaryLabel());
  }

  @Test
  public void userConstructorInitializesEmptyPhoneBookWhenNoPhone() {
    User user = EntityFactory.createUser()
        .withPhoneNumber("")
        .build();

    assertNotNull(user.getPhoneBook());
    assertTrue(user.getPhoneBook().isEmpty());
  }

  // ---- Serialization ----

  @Test
  public void userSerializeIncludesPhoneBook() {
    User user = EntityFactory.createUser()
        .withPhoneNumber("6305264087")
        .build();

    org.json.JSONObject json = user.serialize();

    assertTrue(json.has("phoneBook"));
    org.json.JSONArray phoneBookArray = json.getJSONArray("phoneBook");
    assertEquals(1, phoneBookArray.length());

    org.json.JSONObject entry = phoneBookArray.getJSONObject(0);
    assertEquals("primary", entry.getString("label"));
    assertEquals("6305264087", entry.getString("phoneNumber"));
  }

  // ---- getPhone() derivation ----

  @Test
  public void getPhoneDerivesPrimaryFromPhoneBook() {
    User user = EntityFactory.createUser()
        .withPhoneNumber("6305264087")
        .build();

    assertEquals("6305264087", user.getPhone());
    user.getPhoneBook().get(0).setPhoneNumber("9998887777");
    assertEquals("9998887777", user.getPhone());
  }

  @Test
  public void getPhoneReturnsNullWhenPhoneBookNull() {
    User user = EntityFactory.createUser()
        .withPhoneNumber("6305264087")
        .build();
    user.setPhoneBook(null);

    assertNull(user.getPhone());
  }

  @Test
  public void setPhoneUpdatesPhoneBookPrimaryEntry() {
    User user = EntityFactory.createUser()
        .withPhoneNumber("6305264087")
        .build();

    user.setPhone("9998887777");
    assertEquals("9998887777", user.getPhoneBook().get(0).getPhoneNumber());
    assertEquals("9998887777", user.getPhone());
  }
}
