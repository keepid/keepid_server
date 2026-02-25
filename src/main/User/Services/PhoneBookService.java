package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.PhoneBookEntry;
import User.User;
import User.UserMessage;
import Validation.ValidationUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class PhoneBookService implements Service {

  public enum Action {
    GET, ADD, UPDATE, DELETE
  }

  private final UserDao userDao;
  private final String username;
  private final Action action;
  private final String label;
  private final String phoneNumber;
  private final String newLabel;
  private final String newPhoneNumber;

  private List<PhoneBookEntry> resultPhoneBook;

  private PhoneBookService(
      UserDao userDao,
      String username,
      Action action,
      String label,
      String phoneNumber,
      String newLabel,
      String newPhoneNumber) {
    this.userDao = userDao;
    this.username = username;
    this.action = action;
    this.label = label;
    this.phoneNumber = phoneNumber;
    this.newLabel = newLabel;
    this.newPhoneNumber = newPhoneNumber;
  }

  public static PhoneBookService get(UserDao userDao, String username) {
    return new PhoneBookService(userDao, username, Action.GET, null, null, null, null);
  }

  public static PhoneBookService add(UserDao userDao, String username, String label, String phoneNumber) {
    return new PhoneBookService(userDao, username, Action.ADD, label, phoneNumber, null, null);
  }

  public static PhoneBookService update(
      UserDao userDao, String username, String phoneNumber, String newLabel, String newPhoneNumber) {
    return new PhoneBookService(userDao, username, Action.UPDATE, null, phoneNumber, newLabel, newPhoneNumber);
  }

  public static PhoneBookService delete(UserDao userDao, String username, String phoneNumber) {
    return new PhoneBookService(userDao, username, Action.DELETE, null, phoneNumber, null, null);
  }

  @Override
  public Message executeAndGetResponse() {
    if (username == null || username.isBlank()) {
      return UserMessage.SESSION_TOKEN_FAILURE;
    }
    Optional<User> optionalUser = userDao.get(username);
    if (optionalUser.isEmpty()) {
      return UserMessage.USER_NOT_FOUND;
    }
    User user = optionalUser.get();
    List<PhoneBookEntry> phoneBook = user.getPhoneBook();
    if (phoneBook == null) {
      phoneBook = new ArrayList<>();
    }

    // Lazy migration: if phone book is empty but root phone exists, seed it
    if (phoneBook.isEmpty() && user.getPhone() != null && !user.getPhone().isBlank()) {
      phoneBook.add(new PhoneBookEntry(PhoneBookEntry.PRIMARY_LABEL, user.getPhone()));
    }

    switch (action) {
      case GET:
        this.resultPhoneBook = phoneBook;
        return UserMessage.SUCCESS;
      case ADD:
        return handleAdd(user, phoneBook);
      case UPDATE:
        return handleUpdate(user, phoneBook);
      case DELETE:
        return handleDelete(user, phoneBook);
      default:
        return UserMessage.INVALID_PARAMETER;
    }
  }

  private Message handleAdd(User user, List<PhoneBookEntry> phoneBook) {
    if (label == null || label.isBlank()) {
      log.error("Phone book entry label is empty");
      return UserMessage.EMPTY_FIELD;
    }
    if (PhoneBookEntry.PRIMARY_LABEL.equalsIgnoreCase(label.strip())) {
      log.error("Cannot add an entry with the reserved label \"primary\"");
      return UserMessage.INVALID_PARAMETER;
    }
    if (!ValidationUtils.isValidPhoneNumber(phoneNumber)) {
      log.error("Invalid phone number: {}", phoneNumber);
      return UserMessage.INVALID_PARAMETER;
    }
    String normalized = normalizeToDigits(phoneNumber);
    if (findByPhone(phoneBook, normalized) != null) {
      log.error("Phone number already exists in phone book: {}", normalized);
      return UserMessage.INVALID_PARAMETER;
    }

    // If no primary entry exists, this first number gets the primary label
    String resolvedLabel = phoneBook.stream().noneMatch(PhoneBookEntry::hasPrimaryLabel)
        ? PhoneBookEntry.PRIMARY_LABEL
        : label.strip();
    phoneBook.add(new PhoneBookEntry(resolvedLabel, normalized));
    persistPhoneBook(user, phoneBook);
    this.resultPhoneBook = phoneBook;
    return UserMessage.SUCCESS;
  }

  private Message handleUpdate(User user, List<PhoneBookEntry> phoneBook) {
    String normalized = normalizeToDigits(phoneNumber);
    PhoneBookEntry entry = findByPhone(phoneBook, normalized);
    if (entry == null) {
      log.error("Phone book entry not found: {}", normalized);
      return UserMessage.INVALID_PARAMETER;
    }

    if (newLabel != null && !newLabel.isBlank()) {
      if (entry.hasPrimaryLabel()) {
        log.error("Cannot change the label of the primary entry");
        return UserMessage.INVALID_PARAMETER;
      }
      if (PhoneBookEntry.PRIMARY_LABEL.equalsIgnoreCase(newLabel.strip())) {
        log.error("Cannot set label to the reserved label \"primary\"");
        return UserMessage.INVALID_PARAMETER;
      }
      entry.setLabel(newLabel.strip());
    }

    if (newPhoneNumber != null && !newPhoneNumber.isBlank()) {
      if (!ValidationUtils.isValidPhoneNumber(newPhoneNumber)) {
        log.error("Invalid new phone number: {}", newPhoneNumber);
        return UserMessage.INVALID_PARAMETER;
      }
      String normalizedNew = normalizeToDigits(newPhoneNumber);
      if (!normalizedNew.equals(normalized) && findByPhone(phoneBook, normalizedNew) != null) {
        log.error("New phone number already exists in phone book: {}", normalizedNew);
        return UserMessage.INVALID_PARAMETER;
      }
      entry.setPhoneNumber(normalizedNew);
    }

    persistPhoneBook(user, phoneBook);
    this.resultPhoneBook = phoneBook;
    return UserMessage.SUCCESS;
  }

  private Message handleDelete(User user, List<PhoneBookEntry> phoneBook) {
    String normalized = normalizeToDigits(phoneNumber);
    PhoneBookEntry entry = findByPhone(phoneBook, normalized);
    if (entry == null) {
      log.error("Phone book entry not found: {}", normalized);
      return UserMessage.INVALID_PARAMETER;
    }
    if (entry.hasPrimaryLabel()) {
      log.error("Cannot delete the primary phone book entry");
      return UserMessage.INVALID_PARAMETER;
    }

    phoneBook.remove(entry);
    persistPhoneBook(user, phoneBook);
    this.resultPhoneBook = phoneBook;
    return UserMessage.SUCCESS;
  }

  private void persistPhoneBook(User user, List<PhoneBookEntry> phoneBook) {
    userDao.updateField(username, "phoneBook", phoneBook);
  }

  private PhoneBookEntry findByPhone(List<PhoneBookEntry> phoneBook, String normalized) {
    for (PhoneBookEntry entry : phoneBook) {
      if (normalizeToDigits(entry.getPhoneNumber()).equals(normalized)) {
        return entry;
      }
    }
    return null;
  }

  /**
   * Strips a US phone number to its 10-digit form. Removes formatting characters
   * and a leading country code '1' if present.
   */
  private String normalizeToDigits(String phone) {
    if (phone == null) return "";
    String digits = phone.replaceAll("[^0-9]", "");
    if (digits.length() == 11 && digits.startsWith("1")) {
      digits = digits.substring(1);
    }
    return digits;
  }

  public List<PhoneBookEntry> getResultPhoneBook() {
    return resultPhoneBook;
  }
}
