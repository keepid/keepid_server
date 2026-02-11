package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import Security.EmailSender;
import Security.EmailSenderFactory;
import Security.EmailExceptions;
import Security.EmailUtil;
import User.UserInformation.*;
import User.OptionalInformation;
import User.User;
import User.UserMessage;
import User.UserValidationMessage;
import Validation.ValidationException;
import Validation.ValidationUtils;
import com.mongodb.MongoWriteException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
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
        // Get user from database
        Optional<User> optionalUser = userDao.get(username);
        if (optionalUser.isEmpty()) {
            log.error("User not found: " + username);
            return UserMessage.USER_NOT_FOUND;
        }
        this.user = optionalUser.get();
        String originalEmail = normalizeEmail(this.user.getEmail());

        try {
            JSONObject[] splitUpdates = splitUpdateRequest(updateRequest);
            JSONObject dotNotationUpdates = splitUpdates[0];
            JSONObject nestedObjectUpdates = splitUpdates[1];

            processDotNotationUpdates(dotNotationUpdates);
            
            // Reload user after dot notation updates to sync in-memory object with MongoDB
            // This ensures nested object updates below work with the latest data
            if (dotNotationUpdates.length() > 0) {
                this.user = userDao.get(username).orElseThrow();
            }
            
            processNestedObjectUpdates(nestedObjectUpdates);

            // Save updated user (only if we did nested object updates, dot notation already persisted)
            if (nestedObjectUpdates.length() > 0) {
                userDao.update(user);
            }
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

    private JSONObject[] splitUpdateRequest(JSONObject request) {
        JSONObject dotNotationUpdates = new JSONObject();
        JSONObject nestedObjectUpdates = new JSONObject();

        for (String key : JSONObject.getNames(request)) {
            if (key.contains(".")) {
                dotNotationUpdates.put(key, request.get(key));
            } else {
                nestedObjectUpdates.put(key, request.get(key));
            }
        }

        return new JSONObject[] { dotNotationUpdates, nestedObjectUpdates };
    }

    private void processDotNotationUpdates(JSONObject dotNotationUpdates) throws ValidationException {
        if (dotNotationUpdates.length() > 0) {
            updateFieldsWithDotNotation(dotNotationUpdates);
        }
    }

    private void processNestedObjectUpdates(JSONObject nestedObjectUpdates) throws ValidationException {
        if (hasRootLevelFields(nestedObjectUpdates)) {
            updateRootLevelFields(nestedObjectUpdates);
        }

        if (nestedObjectUpdates.has("optionalInformation")) {
            updateOptionalInformation(nestedObjectUpdates.getJSONObject("optionalInformation"));
        }
    }

    private boolean hasRootLevelFields(JSONObject request) {
        return request.has("email") || request.has("phone") ||
                request.has("address") || request.has("city") ||
                request.has("state") || request.has("zipcode") ||
                request.has("firstName") || request.has("lastName");
    }

    private void updateFieldsWithDotNotation(JSONObject dotNotationUpdates) throws ValidationException {
        for (String fieldPath : JSONObject.getNames(dotNotationUpdates)) {
            Object value = dotNotationUpdates.get(fieldPath);

            // Validate field path
            if (fieldPath == null || fieldPath.trim().isEmpty()) {
                continue;
            }

            // Special validation: cannot update firstName/lastName in Person (they come
            // from root level)
            if (fieldPath.equals("optionalInformation.person.firstName") ||
                    fieldPath.equals("optionalInformation.person.lastName")) {
                log.warn("Ignoring update to firstName/lastName in Person - these come from root level User fields");
                continue;
            }

            // Convert value to appropriate type and validate
            Object convertedValue = convertValueForField(fieldPath, value);
            if (convertedValue == null && value != null && !value.equals(JSONObject.NULL)) {
                // Validation failed
                continue;
            }

            // Use MongoDB $set for field-level update
            try {
                userDao.updateField(username, fieldPath, convertedValue);
            } catch (MongoWriteException e) {
                throw new ValidationException(UserMessage.EMAIL_ALREADY_EXISTS.toJSON());
            } catch (Exception e) {
                log.error("Error updating field '{}': {}", fieldPath, e.getMessage());
                throw new ValidationException(UserMessage.INVALID_PARAMETER.toJSON());
            }
        }
    }

    private Object convertValueForField(String fieldPath, Object value) throws ValidationException {
        if (value == null || value.equals(JSONObject.NULL)) {
            return null;
        }

        // Handle different field types based on path
        if (fieldPath.endsWith(".isVeteran") || fieldPath.endsWith(".isProtectedVeteran") ||
                fieldPath.endsWith(".isEthnicityHispanicLatino") || fieldPath.endsWith(".differentBirthName") ||
                fieldPath.endsWith(".haveDisability")) {
            if (value instanceof Boolean) {
                return value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        } else if (fieldPath.endsWith(".birthDate")) {
            if (value instanceof String) {
                try {
                    return new SimpleDateFormat("yyyy-MM-dd").parse((String) value);
                } catch (Exception e) {
                    throw new ValidationException(
                            UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_BIRTHDATE));
                }
            } else if (value instanceof Date) {
                return value;
            }
        } else if (fieldPath.endsWith(".race")) {
            if (value instanceof String) {
                try {
                    return Race.valueOf((String) value);
                } catch (IllegalArgumentException e) {
                    throw new ValidationException(UserMessage.INVALID_PARAMETER.toJSON());
                }
            } else if (value instanceof Race) {
                return value;
            }
        } else if (fieldPath.endsWith(".citizenship")) {
            if (value instanceof String) {
                try {
                    return Citizenship.valueOf((String) value);
                } catch (IllegalArgumentException e) {
                    throw new ValidationException(UserMessage.INVALID_PARAMETER.toJSON());
                }
            } else if (value instanceof Citizenship) {
                return value;
            }
        } else if (fieldPath.contains(".email") || fieldPath.endsWith("email")) {
            String email = value.toString().trim().toLowerCase();
            if (email != null && !email.isEmpty() && !ValidationUtils.isValidEmail(email)) {
                throw new ValidationException(
                        UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_EMAIL));
            }
            return email;
        } else if (fieldPath.contains(".phone") || fieldPath.endsWith("phone")) {
            String phone = value.toString();
            if (phone != null && !phone.isEmpty() && !ValidationUtils.isValidPhoneNumber(phone)) {
                throw new ValidationException(
                        UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_PHONENUMBER));
            }
            return phone;
        }

        // Default: return as string
        return value.toString();
    }

    private void updateRootLevelFields(JSONObject request) throws ValidationException {
        updateEmailIfPresent(request);
        updatePhoneIfPresent(request);
        updateAddressIfPresent(request);
        updateCityIfPresent(request);
        updateStateIfPresent(request);
        updateZipcodeIfPresent(request);
    }

    private void updateEmailIfPresent(JSONObject request) throws ValidationException {
        if (request.has("email")) {
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
    }

    private void updatePhoneIfPresent(JSONObject request) throws ValidationException {
        if (request.has("phone")) {
            String phone = getValidatedString(request, "phone", ValidationUtils::isValidPhoneNumber,
                    UserValidationMessage.INVALID_PHONENUMBER);
            if (phone != null) {
                user.setPhone(phone);
            }
        }
    }

    private void updateAddressIfPresent(JSONObject request) throws ValidationException {
        if (request.has("address")) {
            String address = getValidatedString(request, "address", ValidationUtils::isValidAddress,
                    UserValidationMessage.INVALID_ADDRESS);
            if (address != null) {
                user.setAddress(address);
            }
        }
    }

    private void updateCityIfPresent(JSONObject request) throws ValidationException {
        if (request.has("city")) {
            String city = getValidatedString(request, "city", ValidationUtils::isValidCity,
                    UserValidationMessage.INVALID_CITY);
            if (city != null) {
                user.setCity(city);
            }
        }
    }

    private void updateStateIfPresent(JSONObject request) throws ValidationException {
        if (request.has("state")) {
            String state = getValidatedString(request, "state", ValidationUtils::isValidUSState,
                    UserValidationMessage.INVALID_STATE);
            if (state != null) {
                user.setState(state);
            }
        }
    }

    private void updateZipcodeIfPresent(JSONObject request) throws ValidationException {
        if (request.has("zipcode")) {
            String zipcode = getValidatedString(request, "zipcode", ValidationUtils::isValidZipCode,
                    UserValidationMessage.INVALID_ZIPCODE);
            if (zipcode != null) {
                user.setZipcode(zipcode);
            }
        }
    }

    private String getValidatedString(JSONObject request, String key,
            java.util.function.Function<String, Boolean> validator,
            UserValidationMessage errorMessage) throws ValidationException {
        String value = request.optString(key, null);
        if (value != null && !value.equals(JSONObject.NULL.toString())) {
            if (!validator.apply(value)) {
                throw new ValidationException(
                        UserValidationMessage.toUserMessageJSON(errorMessage));
            }
            return value;
        }
        return null;
    }

    private void updateOptionalInformation(JSONObject optionalInfoJSON) throws ValidationException {

        // Initialize optionalInformation if it doesn't exist
        if (user.getOptionalInformation() == null) {
            user.setOptionalInformation(new OptionalInformation());
        }
        OptionalInformation optionalInfo = user.getOptionalInformation();

        // Update person
        if (optionalInfoJSON.has("person")) {
            updatePerson(optionalInfoJSON.getJSONObject("person"), optionalInfo);
        }

        // Update basicInfo
        if (optionalInfoJSON.has("basicInfo")) {
            updateBasicInfo(optionalInfoJSON.getJSONObject("basicInfo"), optionalInfo);
        }

        // Update demographicInfo
        if (optionalInfoJSON.has("demographicInfo")) {
            updateDemographicInfo(optionalInfoJSON.getJSONObject("demographicInfo"), optionalInfo);
        }

        // Update familyInfo
        if (optionalInfoJSON.has("familyInfo")) {
            updateFamilyInfo(optionalInfoJSON.getJSONObject("familyInfo"), optionalInfo);
        }

        // Update veteranStatus
        if (optionalInfoJSON.has("veteranStatus")) {
            updateVeteranStatus(optionalInfoJSON.getJSONObject("veteranStatus"), optionalInfo);
        }
    }

    private void updatePerson(JSONObject personJSON, OptionalInformation optionalInfo) throws ValidationException {
        Person person = optionalInfo.getPerson();
        if (person == null) {
            person = new Person();
            optionalInfo.setPerson(person);
        }

        // Note: firstName and lastName are NOT updated here - they come from root level
        // User fields
        // Person.firstName/lastName should remain null for the user's own Person object
        // to avoid duplication and data inconsistency

        if (personJSON.has("middleName")) {
            person.setMiddleName(getStringOrNull(personJSON, "middleName"));
        }

        if (personJSON.has("ssn")) {
            person.setSsn(getStringOrNull(personJSON, "ssn"));
        }

        if (personJSON.has("birthDate")) {
            String birthDateStr = getStringOrNull(personJSON, "birthDate");
            if (birthDateStr != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    Date birthDate = sdf.parse(birthDateStr);
                    person.setBirthDate(birthDate);
                } catch (Exception e) {
                    throw new ValidationException(
                            UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_BIRTHDATE));
                }
            } else {
                person.setBirthDate(null);
            }
        }
    }

    private void updateBasicInfo(JSONObject basicInfoJSON, OptionalInformation optionalInfo)
            throws ValidationException {
        BasicInfo basicInfo = getOrCreateBasicInfo(optionalInfo);

        updateBasicInfoSimpleFields(basicInfoJSON, basicInfo);
        updateBasicInfoValidatedFields(basicInfoJSON, basicInfo);
        updateBasicInfoAddressFields(basicInfoJSON, basicInfo);
        updateBasicInfoRemainingFields(basicInfoJSON, basicInfo);
    }

    private BasicInfo getOrCreateBasicInfo(OptionalInformation optionalInfo) {
        BasicInfo basicInfo = optionalInfo.getBasicInfo();
        if (basicInfo == null) {
            basicInfo = new BasicInfo();
            optionalInfo.setBasicInfo(basicInfo);
        }
        return basicInfo;
    }

    private void updateBasicInfoSimpleFields(JSONObject basicInfoJSON, BasicInfo basicInfo) {
        if (basicInfoJSON.has("genderAssignedAtBirth")) {
            basicInfo.setGenderAssignedAtBirth(getStringOrNull(basicInfoJSON, "genderAssignedAtBirth"));
        }
    }

    private void updateBasicInfoValidatedFields(JSONObject basicInfoJSON, BasicInfo basicInfo)
            throws ValidationException {
        updateValidatedEmailField(basicInfoJSON, basicInfo);
        updateValidatedPhoneField(basicInfoJSON, basicInfo);
    }

    private void updateValidatedEmailField(JSONObject basicInfoJSON, BasicInfo basicInfo) throws ValidationException {
        if (basicInfoJSON.has("emailAddress")) {
            String email = getStringOrNull(basicInfoJSON, "emailAddress");
            if (email != null && !ValidationUtils.isValidEmail(email)) {
                throw new ValidationException(
                        UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_EMAIL));
            }
            basicInfo.setEmailAddress(email);
        }
    }

    private void updateValidatedPhoneField(JSONObject basicInfoJSON, BasicInfo basicInfo) throws ValidationException {
        if (basicInfoJSON.has("phoneNumber")) {
            String phone = getStringOrNull(basicInfoJSON, "phoneNumber");
            if (phone != null && !ValidationUtils.isValidPhoneNumber(phone)) {
                throw new ValidationException(
                        UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_PHONENUMBER));
            }
            basicInfo.setPhoneNumber(phone);
        }
    }

    private void updateBasicInfoAddressFields(JSONObject basicInfoJSON, BasicInfo basicInfo)
            throws ValidationException {
        if (basicInfoJSON.has("mailingAddress")) {
            basicInfo.setMailingAddress(parseAddress(basicInfoJSON.getJSONObject("mailingAddress")));
        }

        if (basicInfoJSON.has("residentialAddress")) {
            basicInfo.setResidentialAddress(parseAddress(basicInfoJSON.getJSONObject("residentialAddress")));
        }
    }

    private void updateBasicInfoRemainingFields(JSONObject basicInfoJSON, BasicInfo basicInfo) {
        updateFieldIfPresent(basicInfoJSON, "differentBirthName", basicInfo::setDifferentBirthName, Boolean.class);
        updateFieldIfPresent(basicInfoJSON, "firstName", basicInfo::setFirstName, String.class);
        updateFieldIfPresent(basicInfoJSON, "middleName", basicInfo::setMiddleName, String.class);
        updateFieldIfPresent(basicInfoJSON, "lastName", basicInfo::setLastName, String.class);
        updateFieldIfPresent(basicInfoJSON, "suffix", basicInfo::setSuffix, String.class);
        updateFieldIfPresent(basicInfoJSON, "birthFirstName", basicInfo::setBirthFirstName, String.class);
        updateFieldIfPresent(basicInfoJSON, "birthMiddleName", basicInfo::setBirthMiddleName, String.class);
        updateFieldIfPresent(basicInfoJSON, "birthLastName", basicInfo::setBirthLastName, String.class);
        updateFieldIfPresent(basicInfoJSON, "birthSuffix", basicInfo::setBirthSuffix, String.class);
        updateFieldIfPresent(basicInfoJSON, "ssn", basicInfo::setSsn, String.class);
        updateFieldIfPresent(basicInfoJSON, "haveDisability", basicInfo::setHaveDisability, Boolean.class);
        updateFieldIfPresent(basicInfoJSON, "stateIdNumber", basicInfo::setStateIdNumber, String.class);
    }

    private void updateDemographicInfo(JSONObject demographicInfoJSON, OptionalInformation optionalInfo) {
        DemographicInfo demographicInfo = getOrCreateDemographicInfo(optionalInfo);

        try {
            DemographicInfo updated = parseDemographicInfoFromJSON(demographicInfoJSON);
            mergeDemographicInfoFields(updated, demographicInfo);
        } catch (Exception e) {
            log.error("Error updating demographicInfo: " + e.getMessage(), e);
        }
    }

    private DemographicInfo getOrCreateDemographicInfo(OptionalInformation optionalInfo) {
        DemographicInfo demographicInfo = optionalInfo.getDemographicInfo();
        if (demographicInfo == null) {
            demographicInfo = new DemographicInfo();
            optionalInfo.setDemographicInfo(demographicInfo);
        }
        return demographicInfo;
    }

    private DemographicInfo parseDemographicInfoFromJSON(JSONObject demographicInfoJSON) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(demographicInfoJSON.toString(), DemographicInfo.class);
    }

    private void mergeDemographicInfoFields(DemographicInfo updated, DemographicInfo existing) {
        if (updated.getLanguagePreference() != null) {
            existing.setLanguagePreference(updated.getLanguagePreference());
        }
        if (updated.getIsEthnicityHispanicLatino() != null) {
            existing.setIsEthnicityHispanicLatino(updated.getIsEthnicityHispanicLatino());
        }
        if (updated.getRace() != null) {
            existing.setRace(updated.getRace());
        }
        if (updated.getCityOfBirth() != null) {
            existing.setCityOfBirth(updated.getCityOfBirth());
        }
        if (updated.getStateOfBirth() != null) {
            existing.setStateOfBirth(updated.getStateOfBirth());
        }
        if (updated.getCountryOfBirth() != null) {
            existing.setCountryOfBirth(updated.getCountryOfBirth());
        }
        if (updated.getCitizenship() != null) {
            existing.setCitizenship(updated.getCitizenship());
        }
    }

    private void updateFamilyInfo(JSONObject familyInfoJSON, OptionalInformation optionalInfo) {
        FamilyInfo familyInfo = optionalInfo.getFamilyInfo();
        if (familyInfo == null) {
            familyInfo = new FamilyInfo();
            optionalInfo.setFamilyInfo(familyInfo);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            FamilyInfo updated = objectMapper.readValue(familyInfoJSON.toString(), FamilyInfo.class);
            // Merge fields
            if (updated.getParents() != null) {
                familyInfo.setParents(updated.getParents());
            }
            if (updated.getLegalGuardians() != null) {
                familyInfo.setLegalGuardians(updated.getLegalGuardians());
            }
            if (updated.getMaritalStatus() != null) {
                familyInfo.setMaritalStatus(updated.getMaritalStatus());
            }
            if (updated.getSpouse() != null) {
                familyInfo.setSpouse(updated.getSpouse());
            }
            if (updated.getChildren() != null) {
                familyInfo.setChildren(updated.getChildren());
            }
            if (updated.getSiblings() != null) {
                familyInfo.setSiblings(updated.getSiblings());
            }
        } catch (Exception e) {
            log.error("Error updating familyInfo: " + e.getMessage(), e);
        }
    }

    private void updateVeteranStatus(JSONObject veteranStatusJSON, OptionalInformation optionalInfo) {
        VeteranStatus veteranStatus = optionalInfo.getVeteranStatus();
        if (veteranStatus == null) {
            veteranStatus = VeteranStatus.builder()
                    .isVeteran(false)
                    .isProtectedVeteran(false)
                    .build();
            optionalInfo.setVeteranStatus(veteranStatus);
        }

        if (veteranStatusJSON.has("isVeteran")) {
            // Use builder to update boolean fields
            boolean isVeteran = veteranStatusJSON.getBoolean("isVeteran");
            veteranStatus = veteranStatus.toBuilder().isVeteran(isVeteran).build();
            optionalInfo.setVeteranStatus(veteranStatus);
        }
        if (veteranStatusJSON.has("isProtectedVeteran")) {
            boolean isProtectedVeteran = veteranStatusJSON.getBoolean("isProtectedVeteran");
            veteranStatus = veteranStatus.toBuilder().isProtectedVeteran(isProtectedVeteran).build();
            optionalInfo.setVeteranStatus(veteranStatus);
        }
        if (veteranStatusJSON.has("branch")) {
            veteranStatus.setBranch(getStringOrNull(veteranStatusJSON, "branch"));
        }
        if (veteranStatusJSON.has("yearsOfService")) {
            veteranStatus.setYearsOfService(getStringOrNull(veteranStatusJSON, "yearsOfService"));
        }
        if (veteranStatusJSON.has("rank")) {
            veteranStatus.setRank(getStringOrNull(veteranStatusJSON, "rank"));
        }
        if (veteranStatusJSON.has("discharge")) {
            veteranStatus.setDischarge(getStringOrNull(veteranStatusJSON, "discharge"));
        }
    }

    private Address parseAddress(JSONObject addressJSON) throws ValidationException {
        if (addressJSON == null || addressJSON.length() == 0) {
            return null;
        }

        Address address = new Address();
        address.setStreetAddress(parseAndValidateAddressField(addressJSON, "streetAddress",
                ValidationUtils::isValidAddress, UserValidationMessage.INVALID_ADDRESS));
        address.setCity(parseAndValidateAddressField(addressJSON, "city",
                ValidationUtils::isValidCity, UserValidationMessage.INVALID_CITY));
        address.setState(parseAndValidateAddressField(addressJSON, "state",
                ValidationUtils::isValidUSState, UserValidationMessage.INVALID_STATE));
        address.setZip(parseAndValidateAddressField(addressJSON, "zip",
                ValidationUtils::isValidZipCode, UserValidationMessage.INVALID_ZIPCODE));
        address.setApartmentNumber(getStringOrNull(addressJSON, "apartmentNumber"));

        return address;
    }

    private String parseAndValidateAddressField(JSONObject addressJSON, String key,
            java.util.function.Function<String, Boolean> validator,
            UserValidationMessage errorMessage) throws ValidationException {
        String value = getStringOrNull(addressJSON, key);
        if (value != null && !validator.apply(value)) {
            throw new ValidationException(
                    UserValidationMessage.toUserMessageJSON(errorMessage));
        }
        return value;
    }

    private String getStringOrNull(JSONObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        Object value = json.get(key);
        if (value == null || JSONObject.NULL.equals(value)) {
            return null;
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private <T> void updateFieldIfPresent(JSONObject json, String key, java.util.function.Consumer<T> setter,
            Class<T> type) {
        if (json.has(key)) {
            Object value = json.get(key);
            if (value == null || JSONObject.NULL.equals(value)) {
                setter.accept(null);
            } else if (type == String.class) {
                setter.accept((T) value.toString());
            } else if (type == Boolean.class) {
                setter.accept((T) Boolean.valueOf(value.toString()));
            }
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }

    private void notifyOnEmailChange(String originalEmail, String updatedEmail) {
        if (updatedEmail.isEmpty() || updatedEmail.equals(originalEmail)) {
            return;
        }
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
