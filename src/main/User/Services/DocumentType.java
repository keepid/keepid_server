package User.Services;

public enum DocumentType {
    SOCIAL_SECURITY_CARD,
    DRIVER_LICENSE,
    BIRTH_CERTIFICATE,
    VACCINE_CARD,
    ERROR;

    public static DocumentType documentTypeFromString(String s){
        s = s.toLowerCase();
        switch (s) {
            case "social_security_card":
                return SOCIAL_SECURITY_CARD;
            case "driver_license":
                return DRIVER_LICENSE;
            case "birth_certificate":
                return BIRTH_CERTIFICATE;
            case "vaccine_card":
                return VACCINE_CARD;
            default:
                return ERROR;
        }
    }

    public static String stringFromDocumentType(DocumentType docType){
        switch (docType) {
            case SOCIAL_SECURITY_CARD:
                return "social_security_card";
            case DRIVER_LICENSE:
                return "driver_license";
            case BIRTH_CERTIFICATE:
                return "birth_certificate";
            case VACCINE_CARD:
                return "vaccine_card";
            default:
                return "error";
        }
    }
}
