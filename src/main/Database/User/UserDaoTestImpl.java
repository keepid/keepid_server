package Database.User;

import Config.DeploymentLevel;
import User.Address;
import User.Name;
import User.PhoneBookEntry;
import User.User;
import java.util.*;
import org.bson.types.ObjectId;

public class UserDaoTestImpl implements UserDao {
  Map<String, User> userMap;

  public UserDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in memory test database in production or staging");
    }
    userMap = new LinkedHashMap<>();
  }

  @Override
  public Optional<User> get(String username) {
    return Optional.ofNullable(userMap.get(username));
  }

  @Override
  public Optional<User> getByEmail(String email) {
    if (email == null) return Optional.empty();
    String normalizedEmail = email.trim();
    for (User user : userMap.values()) {
      String userEmail = user.getEmail();
      if (userEmail != null && userEmail.trim().equalsIgnoreCase(normalizedEmail)) {
        return Optional.of(user);
      }
    }
    return Optional.empty();
  }

  @Override
  public void delete(String username) {
    userMap.remove(username);
  }

  @Override
  public Optional<User> get(ObjectId id) {
    for (User user : userMap.values()) {
      if (user.getId().equals(id)) {
        return Optional.of(user);
      }
    }
    return Optional.empty();
  }

  @Override
  public List<User> getAll() {
    return new ArrayList<User>(userMap.values());
  }

  @Override
  public List<User> getAllFromOrg(String orgName) {
    List<User> result = new ArrayList<>();
    for (User user : userMap.values()) {
      if (user.getOrganization().equals(orgName)) {
        result.add(user);
      }
    }
    return result;
  }

  @Override
  public List<User> getAllFromOrg(ObjectId objectId) {
    List<User> result = new ArrayList<>();
    for (User user : userMap.values()) {
      if (user.getId().equals(objectId)) {
        result.add(user);
      }
    }
    return result;
  }

  @Override
  public int size() {
    return userMap.size();
  }

  @Override
  public void delete(User user) {
    userMap.remove(user.getUsername());
  }

  @Override
  public void clear() {
    userMap.clear();
  }

  @Override
  public void update(User user) {
    userMap.put(user.getUsername(), user);
  }

  @Override
  public void resetPassword(User user, String password) {
    user.setPassword(password);
    userMap.put(user.getUsername(), user);
  }

  @Override
  public void save(User user) {
    userMap.put(user.getUsername(), user);
  }

  @Override
  public void deleteField(String username, String fieldPath) {
    User user = userMap.get(username);
    if (user == null) return;
    deleteFieldFromUser(user, fieldPath);
    userMap.put(username, user);
  }

  @Override
  public void updateField(String username, String fieldPath, Object value) {
    User user = userMap.get(username);
    if (user == null) return;
    updateFieldInUser(user, fieldPath, value);
    userMap.put(username, user);
  }

  private void updateFieldInUser(User user, String fieldPath, Object value) {
    String[] parts = fieldPath.split("\\.");

    if (parts.length == 1) {
      updateRootField(user, parts[0], value);
    } else if (parts.length >= 2) {
      updateNestedField(user, parts, value);
    }
  }

  private void updateRootField(User user, String fieldName, Object value) {
    switch (fieldName) {
      case "email":
        user.setEmail((String) value);
        break;
      case "birthDate":
        user.setBirthDate((String) value);
        break;
      case "sex":
        user.setSex((String) value);
        break;
      case "phoneBook":
        @SuppressWarnings("unchecked")
        java.util.List<PhoneBookEntry> phoneBook = (java.util.List<PhoneBookEntry>) value;
        user.setPhoneBook(phoneBook);
        break;
      case "currentName":
        user.setCurrentName((Name) value);
        break;
      case "personalAddress":
        user.setPersonalAddress((Address) value);
        break;
      case "mailAddress":
        user.setMailAddress((Address) value);
        break;
      case "motherName":
        user.setMotherName((Name) value);
        break;
      case "fatherName":
        user.setFatherName((Name) value);
        break;
      default:
        break;
    }
  }

  private void updateNestedField(User user, String[] parts, Object value) {
    String root = parts[0];
    String field = parts.length > 1 ? parts[1] : null;

    switch (root) {
      case "currentName":
        if (user.getCurrentName() == null) user.setCurrentName(new Name());
        updateNameField(user.getCurrentName(), field, value);
        break;
      case "personalAddress":
        if (user.getPersonalAddress() == null) user.setPersonalAddress(new Address());
        updateAddressField(user.getPersonalAddress(), field, value);
        break;
      case "mailAddress":
        if (user.getMailAddress() == null) user.setMailAddress(new Address());
        updateAddressField(user.getMailAddress(), field, value);
        break;
      case "motherName":
        if (user.getMotherName() == null) user.setMotherName(new Name());
        updateNameField(user.getMotherName(), field, value);
        break;
      case "fatherName":
        if (user.getFatherName() == null) user.setFatherName(new Name());
        updateNameField(user.getFatherName(), field, value);
        break;
      default:
        break;
    }
  }

  private void updateNameField(Name name, String field, Object value) {
    if (field == null) return;
    switch (field) {
      case "first": name.setFirst((String) value); break;
      case "middle": name.setMiddle((String) value); break;
      case "last": name.setLast((String) value); break;
      case "suffix": name.setSuffix((String) value); break;
      case "maiden": name.setMaiden((String) value); break;
      default: break;
    }
  }

  private void updateAddressField(Address address, String field, Object value) {
    if (field == null) return;
    switch (field) {
      case "line1": address.setLine1((String) value); break;
      case "line2": address.setLine2((String) value); break;
      case "city": address.setCity((String) value); break;
      case "state": address.setState((String) value); break;
      case "zip": address.setZip((String) value); break;
      case "county": address.setCounty((String) value); break;
      default: break;
    }
  }

  private void deleteFieldFromUser(User user, String fieldPath) {
    String[] parts = fieldPath.split("\\.");

    if (parts.length == 1) {
      deleteRootField(user, parts[0]);
    } else if (parts.length >= 2) {
      deleteNestedField(user, parts);
    }
  }

  private void deleteRootField(User user, String fieldName) {
    switch (fieldName) {
      case "sex": user.setSex(null); break;
      case "mailAddress": user.setMailAddress(null); break;
      case "motherName": user.setMotherName(null); break;
      case "fatherName": user.setFatherName(null); break;
      case "nameHistory": user.setNameHistory(null); break;
      default: break;
    }
  }

  private void deleteNestedField(User user, String[] parts) {
    String root = parts[0];
    String field = parts.length > 1 ? parts[1] : null;

    switch (root) {
      case "currentName":
        if (user.getCurrentName() != null && field != null) {
          updateNameField(user.getCurrentName(), field, null);
        }
        break;
      case "personalAddress":
        if (user.getPersonalAddress() != null && field != null) {
          updateAddressField(user.getPersonalAddress(), field, null);
        }
        break;
      case "mailAddress":
        if (user.getMailAddress() != null && field != null) {
          updateAddressField(user.getMailAddress(), field, null);
        }
        break;
      case "motherName":
        if (user.getMotherName() != null && field != null) {
          updateNameField(user.getMotherName(), field, null);
        }
        break;
      case "fatherName":
        if (user.getFatherName() != null && field != null) {
          updateNameField(user.getFatherName(), field, null);
        }
        break;
      default:
        break;
    }
  }
}
