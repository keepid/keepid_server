package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import Security.EmailSender;
import Security.EmailSenderFactory;
import Security.EmailExceptions;
import Security.EmailUtil;
import User.Address;
import User.Name;
import User.User;
import User.UserMessage;
import User.UserValidationMessage;
import Validation.ValidationException;
import Validation.ValidationUtils;
import com.mongodb.MongoWriteException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class UpdateUserProfileService implements Service {
    private final UserDao userDao;
    private final String username;
    private final JSONObject updateRequest;
    private final EmailSender emailSender;
    private User user;

    public UpdateUserProfileService(UserDao userDao, String username, JSONObject updateRequest) {
        this(userDao, username, updateRequest, EmailSenderFactory.smtp());
    }

    public UpdateUserProfileService(UserDao userDao, String username, JSONObject updateRequest, EmailSender emailSender) {
        this.userDao = userDao;
        this.username = username;
        this.updateRequest = updateRequest;
        this.emailSender = emailSender;
    }

    @Override
    public Message executeAndGetResponse() {
        Optional<User> optionalUser = userDao.get(username);
        if (optionalUser.isEmpty()) {
            log.error("User not found: " + username);
            return UserMessage.USER_NOT_FOUND;
        }
        this.user = optionalUser.get();
        String originalEmail = normalizeEmail(this.user.getEmail());

        try {
            processUpdates(updateRequest);
            userDao.update(user);
            notifyOnEmailChange(originalEmail, normalizeEmail(user.getEmail()));
            log.info("Successfully updated user profile for: " + username);
            return UserMessage.SUCCESS;
        } catch (ValidationException e) {
            log.error("Validation error: " + e.getMessage());
            return e;
        } catch (MongoWriteException e) {
            log.warn("Duplicate email while updating profile for {}: {}", username, e.getMessage());
            return UserMessage.EMAIL_ALREADY_EXISTS;
        } catch (Exception e) {
            log.error("Error updating user profile: " + e.getMessage(), e);
            return UserMessage.AUTH_FAILURE;
        }
    }

    private void processUpdates(JSONObject request) throws ValidationException {
        updateEmailIfPresent(request);
        updateCurrentNameIfPresent(request);
        updateNameHistoryIfPresent(request);
        updatePersonalAddressIfPresent(request);
        updateMailAddressIfPresent(request);
        updateSexIfPresent(request);
        updateMotherNameIfPresent(request);
        updateFatherNameIfPresent(request);
        updateBirthDateIfPresent(request);
    }

    private void updateEmailIfPresent(JSONObject request) throws ValidationException {
        if (!request.has("email")) return;
        Object rawEmail = request.get("email");
        if (rawEmail == null || JSONObject.NULL.equals(rawEmail)) {
            user.setEmail("");
            return;
        }

        String email = rawEmail.toString().trim().toLowerCase();
        if (!email.isEmpty() && !ValidationUtils.isValidEmail(email)) {
            throw new ValidationException(
                    UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_EMAIL));
        }
        if (!email.isEmpty()) {
            Optional<User> existing = userDao.getByEmail(email);
            if (existing.isPresent() && !existing.get().getUsername().equals(username)) {
                throw new ValidationException(UserMessage.EMAIL_ALREADY_EXISTS.toJSON());
            }
        }
        user.setEmail(email);
    }

    private void updateCurrentNameIfPresent(JSONObject request) throws ValidationException {
        if (!request.has("currentName")) return;
        Object raw = request.get("currentName");
        if (raw == null || JSONObject.NULL.equals(raw)) return;

        JSONObject nameJson = request.getJSONObject("currentName");
        Name currentName = user.getCurrentName();
        if (currentName == null) {
            currentName = new Name();
            user.setCurrentName(currentName);
        }

        if (nameJson.has("first")) {
            String first = getStringOrNull(nameJson, "first");
            if (first != null && !ValidationUtils.isValidFirstName(first)) {
                throw new ValidationException(
                        UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_FIRSTNAME));
            }
            currentName.setFirst(first);
        }
        if (nameJson.has("last")) {
            String last = getStringOrNull(nameJson, "last");
            if (last != null && !ValidationUtils.isValidLastName(last)) {
                throw new ValidationException(
                        UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_LASTNAME));
            }
            currentName.setLast(last);
        }
        if (nameJson.has("middle")) currentName.setMiddle(getStringOrNull(nameJson, "middle"));
        if (nameJson.has("suffix")) currentName.setSuffix(getStringOrNull(nameJson, "suffix"));
        if (nameJson.has("maiden")) currentName.setMaiden(getStringOrNull(nameJson, "maiden"));
    }

    private void updateNameHistoryIfPresent(JSONObject request) {
        if (!request.has("nameHistory")) return;
        Object raw = request.get("nameHistory");
        if (raw == null || JSONObject.NULL.equals(raw)) {
            user.setNameHistory(null);
            return;
        }
        org.json.JSONArray arr = request.getJSONArray("nameHistory");
        List<Name> history = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject nj = arr.getJSONObject(i);
            Name name = new Name(
                getStringOrNull(nj, "first"),
                getStringOrNull(nj, "middle"),
                getStringOrNull(nj, "last"),
                getStringOrNull(nj, "suffix"),
                getStringOrNull(nj, "maiden"));
            history.add(name);
        }
        user.setNameHistory(history);
    }

    private void updatePersonalAddressIfPresent(JSONObject request) throws ValidationException {
        if (!request.has("personalAddress")) return;
        Object raw = request.get("personalAddress");
        if (raw == null || JSONObject.NULL.equals(raw)) {
            user.setPersonalAddress(null);
            return;
        }
        user.setPersonalAddress(parseAddress(request.getJSONObject("personalAddress")));
    }

    private void updateMailAddressIfPresent(JSONObject request) throws ValidationException {
        if (!request.has("mailAddress")) return;
        Object raw = request.get("mailAddress");
        if (raw == null || JSONObject.NULL.equals(raw)) {
            user.setMailAddress(null);
            return;
        }
        user.setMailAddress(parseAddress(request.getJSONObject("mailAddress")));
    }

    private void updateSexIfPresent(JSONObject request) {
        if (!request.has("sex")) return;
        user.setSex(getStringOrNull(request, "sex"));
    }

    private void updateMotherNameIfPresent(JSONObject request) {
        if (!request.has("motherName")) return;
        Object raw = request.get("motherName");
        if (raw == null || JSONObject.NULL.equals(raw)) {
            user.setMotherName(null);
            return;
        }
        user.setMotherName(parseName(request.getJSONObject("motherName")));
    }

    private void updateFatherNameIfPresent(JSONObject request) {
        if (!request.has("fatherName")) return;
        Object raw = request.get("fatherName");
        if (raw == null || JSONObject.NULL.equals(raw)) {
            user.setFatherName(null);
            return;
        }
        user.setFatherName(parseName(request.getJSONObject("fatherName")));
    }

    private void updateBirthDateIfPresent(JSONObject request) throws ValidationException {
        if (!request.has("birthDate")) return;
        String birthDate = getStringOrNull(request, "birthDate");
        if (birthDate != null && !ValidationUtils.isValidBirthDate(birthDate)) {
            throw new ValidationException(
                    UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_BIRTHDATE));
        }
        user.setBirthDate(birthDate);
    }

    private Name parseName(JSONObject nameJson) {
        return new Name(
            getStringOrNull(nameJson, "first"),
            getStringOrNull(nameJson, "middle"),
            getStringOrNull(nameJson, "last"),
            getStringOrNull(nameJson, "suffix"),
            getStringOrNull(nameJson, "maiden"));
    }

    private Address parseAddress(JSONObject addressJSON) throws ValidationException {
        if (addressJSON == null || addressJSON.length() == 0) {
            return null;
        }
        Address address = new Address();
        String line1 = getStringOrNull(addressJSON, "line1");
        if (line1 != null && !line1.isEmpty() && !ValidationUtils.isValidAddress(line1)) {
            throw new ValidationException(
                    UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_ADDRESS));
        }
        address.setLine1(line1);
        address.setLine2(getStringOrNull(addressJSON, "line2"));

        String city = getStringOrNull(addressJSON, "city");
        if (city != null && !city.isEmpty() && !ValidationUtils.isValidCity(city)) {
            throw new ValidationException(
                    UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_CITY));
        }
        address.setCity(city);

        String state = getStringOrNull(addressJSON, "state");
        if (state != null && !state.isEmpty() && !ValidationUtils.isValidUSState(state)) {
            throw new ValidationException(
                    UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_STATE));
        }
        address.setState(state);

        String zip = getStringOrNull(addressJSON, "zip");
        if (zip != null && !zip.isEmpty() && !ValidationUtils.isValidZipCode(zip)) {
            throw new ValidationException(
                    UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_ZIPCODE));
        }
        address.setZip(zip);
        address.setCounty(getStringOrNull(addressJSON, "county"));
        return address;
    }

    private String getStringOrNull(JSONObject json, String key) {
        if (!json.has(key)) return null;
        Object value = json.get(key);
        if (value == null || JSONObject.NULL.equals(value)) return null;
        return value.toString();
    }

    private String normalizeEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase();
    }

    private void notifyOnEmailChange(String originalEmail, String updatedEmail) {
        if (updatedEmail.isEmpty() || updatedEmail.equals(originalEmail)) return;
        try {
            String message = EmailUtil.getAccountEmailChangedNotificationEmail();
            emailSender.sendEmail("Keep Id", updatedEmail, "Keep.id account email updated", message);
        } catch (EmailExceptions e) {
            log.warn("Unable to send email change notification to {}: {}", updatedEmail, e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error while sending email change notification to {}: {}", updatedEmail, e.getMessage());
        }
    }
}
