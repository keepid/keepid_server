package File;

public enum IdCategoryType {
    DRIVERS_LICENSE_PHOTO_ID("Drivers License / Photo ID"),
    BIRTH_CERTIFICATE("Birth Certificate"),
    SOCIAL_SECURITY_CARD("Social Security Card"),
    VACCINE_CARD("Vaccine Card"),
    MEDICAID_CARD("Medical Insurance Card"),
    VETERAN_ID_CARD("Veteran ID Card"),
    SCHOOL_TRANSCRIPT("School Transcript"),
    ID_ME_RECOVERY_CODES("ID.me Recovery Codes"),
    OTHER("Other"),
    NONE("None");

    private final String idCategoryType;

    IdCategoryType(String idCategoryType) {
        this.idCategoryType = idCategoryType;
    }

    public String toString() {
        return this.idCategoryType;
    }

    public static IdCategoryType createFromString(String idCategoryTypeString) {
        if (idCategoryTypeString == null) {
            return IdCategoryType.NONE;
        }
        switch (idCategoryTypeString.trim()) {
            case "Drivers License / Photo ID":
            case "DRIVERS_LICENSE_PHOTO_ID":
                return IdCategoryType.DRIVERS_LICENSE_PHOTO_ID;
            case "Birth Certificate":
            case "BIRTH_CERTIFICATE":
                return IdCategoryType.BIRTH_CERTIFICATE;
            case "Social Security Card":
            case "SOCIAL_SECURITY_CARD":
                return IdCategoryType.SOCIAL_SECURITY_CARD;
            case "Medical Insurance Card":
            case "MEDICAID_CARD":
                return IdCategoryType.MEDICAID_CARD;
            case "ID.me Recovery Codes":
            case "ID_ME_RECOVERY_CODES":
                return IdCategoryType.ID_ME_RECOVERY_CODES;
            case "Other":
            case "Other: specify":
            case "OTHER":
                return IdCategoryType.OTHER;
            case "None":
            case "NONE":
                return IdCategoryType.NONE;
            default:
                return IdCategoryType.NONE;
        }
    }
}
