package File;

public enum IdCategoryType {
    DRIVERS_LICENSE_PHOTO_ID("Drivers License / Photo ID"),
    BIRTH_CERTIFICATE("Birth Certificate"),
    SOCIAL_SECURITY_CARD("Social Security Card"),
    VACCINE_CARD("Vaccine Card"),
    MEDICAID_CARD("Medical Insurance Card"),
    VETERAN_ID_CARD("Veteran ID Card"),
    SCHOOL_TRANSCRIPT("School Transcript"),
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
        switch (idCategoryTypeString) {
            case "Drivers License / Photo ID":
                return IdCategoryType.DRIVERS_LICENSE_PHOTO_ID;
            case "Birth Certificate":
                return IdCategoryType.BIRTH_CERTIFICATE;
            case "Social Security Card":
                return IdCategoryType.SOCIAL_SECURITY_CARD;
            case "Vaccine Card":
                return IdCategoryType.VACCINE_CARD;
            case "Medicaid Card":
                return IdCategoryType.MEDICAID_CARD;
            case "Veteran ID Card":
                return IdCategoryType.VETERAN_ID_CARD;
            case "School Transcript":
                return IdCategoryType.SCHOOL_TRANSCRIPT;
            case "Other":
                return IdCategoryType.OTHER;
            case "None":
                return IdCategoryType.NONE;
            default:
                return IdCategoryType.NONE;
        }
    }
}
