package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import OptionalUserInformation.*;
import User.OptionalInformation;
import User.User;
import User.UserMessage;
import User.UserValidationMessage;
import Validation.ValidationException;
import Validation.ValidationUtils;
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
    private User user;

    public UpdateUserProfileService(UserDao userDao, String username, JSONObject updateRequest) {
        this.userDao = userDao;
        this.username = username;
        this.updateRequest = updateRequest;
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

        try {
            // Separate dot-notation field updates from nested object updates
            JSONObject dotNotationUpdates = new JSONObject();
            JSONObject nestedObjectUpdates = new JSONObject();

            // Split request into dot-notation fields and nested objects
            for (String key : JSONObject.getNames(updateRequest)) {
                if (key.contains(".")) {
                    // Dot notation field (e.g., "optionalInformation.demographicInfo.languagePreference")
                    dotNotationUpdates.put(key, updateRequest.get(key));
                } else {
                    // Regular nested object (e.g., "optionalInformation": {...})
                    nestedObjectUpdates.put(key, updateRequest.get(key));
                }
            }

            // Process dot-notation field updates first (these use MongoDB $set directly)
            if (dotNotationUpdates.length() > 0) {
                updateFieldsWithDotNotation(dotNotationUpdates);
            }

            // Process nested object updates (backward compatible with existing structure)
            if (nestedObjectUpdates.has("email") || nestedObjectUpdates.has("phone") || 
                nestedObjectUpdates.has("address") || nestedObjectUpdates.has("city") ||
                nestedObjectUpdates.has("state") || nestedObjectUpdates.has("zipcode") ||
                nestedObjectUpdates.has("firstName") || nestedObjectUpdates.has("lastName")) {
                updateRootLevelFields(nestedObjectUpdates);
            }

            if (nestedObjectUpdates.has("optionalInformation")) {
                updateOptionalInformation(nestedObjectUpdates.getJSONObject("optionalInformation"));
            }

            // Save updated user
            userDao.update(user);
            log.info("Successfully updated user profile for: " + username);
            return UserMessage.SUCCESS;
        } catch (ValidationException e) {
            log.error("Validation error: " + e.getMessage());
            return e;
        } catch (Exception e) {
            log.error("Error updating user profile: " + e.getMessage(), e);
            return UserMessage.AUTH_FAILURE;
        }
    }

    private void updateFieldsWithDotNotation(JSONObject dotNotationUpdates) throws ValidationException {
        for (String fieldPath : JSONObject.getNames(dotNotationUpdates)) {
            Object value = dotNotationUpdates.get(fieldPath);
            
            // Validate field path
            if (fieldPath == null || fieldPath.trim().isEmpty()) {
                continue;
            }

            // Special validation: cannot update firstName/lastName in Person (they come from root level)
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
            String email = value.toString();
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
        // Update email
        if (request.has("email")) {
            String email = request.optString("email", null);
            if (email != null && !email.equals(JSONObject.NULL.toString())) {
                if (!ValidationUtils.isValidEmail(email)) {
                    throw new ValidationException(
                            UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_EMAIL));
                }
                user.setEmail(email);
            }
        }

        // Update phone
        if (request.has("phone")) {
            String phone = request.optString("phone", null);
            if (phone != null && !phone.equals(JSONObject.NULL.toString())) {
                if (!ValidationUtils.isValidPhoneNumber(phone)) {
                    throw new ValidationException(
                            UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_PHONENUMBER));
                }
                user.setPhone(phone);
            }
        }

        // Update address fields
        if (request.has("address")) {
            String address = request.optString("address", null);
            if (address != null && !address.equals(JSONObject.NULL.toString())) {
                if (!ValidationUtils.isValidAddress(address)) {
                    throw new ValidationException(
                            UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_ADDRESS));
                }
                user.setAddress(address);
            }
        }

        if (request.has("city")) {
            String city = request.optString("city", null);
            if (city != null && !city.equals(JSONObject.NULL.toString())) {
                if (!ValidationUtils.isValidCity(city)) {
                    throw new ValidationException(
                            UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_CITY));
                }
                user.setCity(city);
            }
        }

        if (request.has("state")) {
            String state = request.optString("state", null);
            if (state != null && !state.equals(JSONObject.NULL.toString())) {
                if (!ValidationUtils.isValidUSState(state)) {
                    throw new ValidationException(
                            UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_STATE));
                }
                user.setState(state);
            }
        }

        if (request.has("zipcode")) {
            String zipcode = request.optString("zipcode", null);
            if (zipcode != null && !zipcode.equals(JSONObject.NULL.toString())) {
                if (!ValidationUtils.isValidZipCode(zipcode)) {
                    throw new ValidationException(
                            UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_ZIPCODE));
                }
                user.setZipcode(zipcode);
            }
        }
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
        BasicInfo basicInfo = optionalInfo.getBasicInfo();
        if (basicInfo == null) {
            basicInfo = new BasicInfo();
            optionalInfo.setBasicInfo(basicInfo);
        }

        // Merge fields
        if (basicInfoJSON.has("genderAssignedAtBirth")) {
            basicInfo.setGenderAssignedAtBirth(getStringOrNull(basicInfoJSON, "genderAssignedAtBirth"));
        }

        if (basicInfoJSON.has("emailAddress")) {
            String email = getStringOrNull(basicInfoJSON, "emailAddress");
            if (email != null && !ValidationUtils.isValidEmail(email)) {
                throw new ValidationException(
                        UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_EMAIL));
            }
            basicInfo.setEmailAddress(email);
        }

        if (basicInfoJSON.has("phoneNumber")) {
            String phone = getStringOrNull(basicInfoJSON, "phoneNumber");
            if (phone != null && !ValidationUtils.isValidPhoneNumber(phone)) {
                throw new ValidationException(
                        UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_PHONENUMBER));
            }
            basicInfo.setPhoneNumber(phone);
        }

        if (basicInfoJSON.has("mailingAddress")) {
            basicInfo.setMailingAddress(parseAddress(basicInfoJSON.getJSONObject("mailingAddress")));
        }

        if (basicInfoJSON.has("residentialAddress")) {
            basicInfo.setResidentialAddress(parseAddress(basicInfoJSON.getJSONObject("residentialAddress")));
        }

        // Update other BasicInfo fields
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
        DemographicInfo demographicInfo = optionalInfo.getDemographicInfo();
        if (demographicInfo == null) {
            demographicInfo = new DemographicInfo();
            optionalInfo.setDemographicInfo(demographicInfo);
        }

        // Use ObjectMapper for complex nested objects
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            DemographicInfo updated = objectMapper.readValue(demographicInfoJSON.toString(), DemographicInfo.class);
            // Merge: copy non-null fields from updated to existing
            if (updated.getLanguagePreference() != null) {
                demographicInfo.setLanguagePreference(updated.getLanguagePreference());
            }
            if (updated.getIsEthnicityHispanicLatino() != null) {
                demographicInfo.setIsEthnicityHispanicLatino(updated.getIsEthnicityHispanicLatino());
            }
            if (updated.getRace() != null) {
                demographicInfo.setRace(updated.getRace());
            }
            if (updated.getCityOfBirth() != null) {
                demographicInfo.setCityOfBirth(updated.getCityOfBirth());
            }
            if (updated.getStateOfBirth() != null) {
                demographicInfo.setStateOfBirth(updated.getStateOfBirth());
            }
            if (updated.getCountryOfBirth() != null) {
                demographicInfo.setCountryOfBirth(updated.getCountryOfBirth());
            }
            if (updated.getCitizenship() != null) {
                demographicInfo.setCitizenship(updated.getCitizenship());
            }
        } catch (Exception e) {
            log.error("Error updating demographicInfo: " + e.getMessage(), e);
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

        String streetAddress = getStringOrNull(addressJSON, "streetAddress");
        if (streetAddress != null && !ValidationUtils.isValidAddress(streetAddress)) {
            throw new ValidationException(
                    UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_ADDRESS));
        }
        address.setStreetAddress(streetAddress);

        String city = getStringOrNull(addressJSON, "city");
        if (city != null && !ValidationUtils.isValidCity(city)) {
            throw new ValidationException(UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_CITY));
        }
        address.setCity(city);

        String state = getStringOrNull(addressJSON, "state");
        if (state != null && !ValidationUtils.isValidUSState(state)) {
            throw new ValidationException(UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_STATE));
        }
        address.setState(state);

        String zip = getStringOrNull(addressJSON, "zip");
        if (zip != null && !ValidationUtils.isValidZipCode(zip)) {
            throw new ValidationException(
                    UserValidationMessage.toUserMessageJSON(UserValidationMessage.INVALID_ZIPCODE));
        }
        address.setZip(zip);

        address.setApartmentNumber(getStringOrNull(addressJSON, "apartmentNumber"));

        return address;
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
}
