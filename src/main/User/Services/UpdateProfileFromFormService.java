package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.Address;
import User.Name;
import User.User;
import User.UserMessage;
import Validation.ValidationUtils;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class UpdateProfileFromFormService implements Service {
  private static final Map<String, String> FIELD_ALIASES = new HashMap<>();
  private static final Pattern NAME_HISTORY_PATTERN =
      Pattern.compile("^nameHistory\\.(\\d+)\\.(first|middle|last|suffix|maiden)$");

  static {
    FIELD_ALIASES.put("firstName", "currentName.first");
    FIELD_ALIASES.put("lastName", "currentName.last");
    FIELD_ALIASES.put("middleName", "currentName.middle");
    FIELD_ALIASES.put("phone", "phoneBook.0.phoneNumber");
    FIELD_ALIASES.put("phoneNumber", "phoneBook.0.phoneNumber");
    FIELD_ALIASES.put("address", "personalAddress.line1");
    FIELD_ALIASES.put("streetAddress", "personalAddress.line1");
    FIELD_ALIASES.put("city", "personalAddress.city");
    FIELD_ALIASES.put("state", "personalAddress.state");
    FIELD_ALIASES.put("zipcode", "personalAddress.zip");
    FIELD_ALIASES.put("genderAssignedAtBirth", "sex");
    FIELD_ALIASES.put("emailAddress", "email");
  }

  private final UserDao userDao;
  private final String username;
  private final JSONObject formAnswers;

  public UpdateProfileFromFormService(UserDao userDao, String username, JSONObject formAnswers) {
    this.userDao = userDao;
    this.username = username;
    this.formAnswers = formAnswers;
  }

  @Override
  public Message executeAndGetResponse() {
    if (userDao == null || username == null || formAnswers == null) {
      return UserMessage.INVALID_PARAMETER;
    }

    Optional<User> userOptional = userDao.get(username);
    if (userOptional.isEmpty()) {
      return UserMessage.USER_NOT_FOUND;
    }

    User user = userOptional.get();
    boolean changed = false;

    for (String fieldName : formAnswers.keySet()) {
      String answer = getNonEmptyAnswer(formAnswers.opt(fieldName));
      if (answer == null) {
        continue;
      }

      String directive = normalizeDirective(fieldName);
      if (directive == null) {
        continue;
      }

      changed = applyDirective(user, directive, answer) || changed;
    }

    if (changed) {
      userDao.update(user);
    }
    return UserMessage.SUCCESS;
  }

  private String getNonEmptyAnswer(Object rawValue) {
    if (rawValue == null || JSONObject.NULL.equals(rawValue)) {
      return null;
    }
    String answer = String.valueOf(rawValue).trim();
    if (answer.isEmpty() || answer.equalsIgnoreCase("null")) {
      return null;
    }
    return answer;
  }

  private String normalizeDirective(String fieldName) {
    String directive = fieldName;
    int lastColon = fieldName.lastIndexOf(':');
    if (lastColon >= 0 && lastColon + 1 < fieldName.length()) {
      directive = fieldName.substring(lastColon + 1);
    }

    if (directive.isEmpty()
        || directive.equals("currentDate")
        || directive.equals("anyDate")
        || directive.equals("signature")
        || directive.startsWith("+")
        || directive.startsWith("-")) {
      return null;
    }

    if (directive.startsWith("org.") || directive.startsWith("worker.")) {
      return null;
    }
    if (directive.startsWith("client.")) {
      directive = directive.substring("client.".length());
    }

    return FIELD_ALIASES.getOrDefault(directive, directive);
  }

  private boolean applyDirective(User user, String directive, String value) {
    switch (directive) {
      case "currentName.first":
      case "currentName.middle":
      case "currentName.last":
      case "currentName.suffix":
      case "currentName.maiden":
        return applyNameField(ensureCurrentName(user), directive.substring("currentName.".length()), value);
      case "motherName.first":
      case "motherName.middle":
      case "motherName.last":
      case "motherName.suffix":
      case "motherName.maiden":
        return applyNameField(ensureMotherName(user), directive.substring("motherName.".length()), value);
      case "fatherName.first":
      case "fatherName.middle":
      case "fatherName.last":
      case "fatherName.suffix":
      case "fatherName.maiden":
        return applyNameField(ensureFatherName(user), directive.substring("fatherName.".length()), value);
      case "birthDate":
        return applyBirthDate(user, value);
      case "sex":
        user.setSex(value);
        return true;
      case "email":
        if (ValidationUtils.isValidEmail(value)) {
          user.setEmail(value.toLowerCase());
          return true;
        }
        return false;
      case "phoneBook.0.phoneNumber":
        if (ValidationUtils.isValidPhoneNumber(value)) {
          user.setPhone(value);
          return true;
        }
        return false;
      case "personalAddress.line1":
      case "personalAddress.line2":
      case "personalAddress.city":
      case "personalAddress.state":
      case "personalAddress.zip":
      case "personalAddress.county":
        return applyAddressField(
            ensurePersonalAddress(user),
            directive.substring("personalAddress.".length()),
            value);
      case "mailAddress.line1":
      case "mailAddress.line2":
      case "mailAddress.city":
      case "mailAddress.state":
      case "mailAddress.zip":
      case "mailAddress.county":
        return applyAddressField(
            ensureMailAddress(user),
            directive.substring("mailAddress.".length()),
            value);
      default:
        return applyNameHistoryField(user, directive, value);
    }
  }

  private boolean applyBirthDate(User user, String value) {
    String normalized = normalizeBirthDate(value);
    if (normalized == null || !ValidationUtils.isValidBirthDate(normalized)) {
      return false;
    }
    user.setBirthDate(normalized);
    return true;
  }

  private String normalizeBirthDate(String input) {
    List<DateTimeFormatter> formatters =
        List.of(
            DateTimeFormatter.ofPattern("MM-dd-uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT));
    for (DateTimeFormatter formatter : formatters) {
      try {
        LocalDate parsed = LocalDate.parse(input, formatter);
        return parsed.format(DateTimeFormatter.ofPattern("MM-dd-uuuu"));
      } catch (DateTimeParseException ignored) {
        // Try next format.
      }
    }
    return input;
  }

  private boolean applyNameField(Name name, String part, String value) {
    switch (part) {
      case "first":
        name.setFirst(value);
        return true;
      case "middle":
        name.setMiddle(value);
        return true;
      case "last":
        name.setLast(value);
        return true;
      case "suffix":
        name.setSuffix(value);
        return true;
      case "maiden":
        name.setMaiden(value);
        return true;
      default:
        return false;
    }
  }

  private boolean applyAddressField(Address address, String part, String value) {
    switch (part) {
      case "line1":
        if (!ValidationUtils.isValidAddress(value)) {
          return false;
        }
        address.setLine1(value);
        return true;
      case "line2":
        address.setLine2(value);
        return true;
      case "city":
        if (!ValidationUtils.isValidCity(value)) {
          return false;
        }
        address.setCity(value);
        return true;
      case "state":
        if (!ValidationUtils.isValidUSState(value)) {
          return false;
        }
        address.setState(value);
        return true;
      case "zip":
        if (!ValidationUtils.isValidZipCode(value)) {
          return false;
        }
        address.setZip(value);
        return true;
      case "county":
        address.setCounty(value);
        return true;
      default:
        return false;
    }
  }

  private boolean applyNameHistoryField(User user, String directive, String value) {
    Matcher matcher = NAME_HISTORY_PATTERN.matcher(directive);
    if (!matcher.matches()) {
      return false;
    }
    int index = Integer.parseInt(matcher.group(1));
    String part = matcher.group(2);
    Name historyName = ensureNameHistoryEntry(user, index);
    return applyNameField(historyName, part, value);
  }

  private Name ensureCurrentName(User user) {
    if (user.getCurrentName() == null) {
      user.setCurrentName(new Name());
    }
    return user.getCurrentName();
  }

  private Name ensureMotherName(User user) {
    if (user.getMotherName() == null) {
      user.setMotherName(new Name());
    }
    return user.getMotherName();
  }

  private Name ensureFatherName(User user) {
    if (user.getFatherName() == null) {
      user.setFatherName(new Name());
    }
    return user.getFatherName();
  }

  private Address ensurePersonalAddress(User user) {
    if (user.getPersonalAddress() == null) {
      user.setPersonalAddress(new Address());
    }
    return user.getPersonalAddress();
  }

  private Address ensureMailAddress(User user) {
    if (user.getMailAddress() == null) {
      user.setMailAddress(new Address());
    }
    return user.getMailAddress();
  }

  private Name ensureNameHistoryEntry(User user, int index) {
    List<Name> nameHistory = user.getNameHistory();
    if (nameHistory == null) {
      nameHistory = new ArrayList<>();
      user.setNameHistory(nameHistory);
    }
    while (nameHistory.size() <= index) {
      nameHistory.add(new Name());
    }
    return nameHistory.get(index);
  }
}
