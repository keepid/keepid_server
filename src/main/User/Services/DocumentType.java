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
            case "social-security":
                return SOCIAL_SECURITY_CARD;
            case "drivers-license":
                return DRIVER_LICENSE;
            case "birth-certificate":
                return BIRTH_CERTIFICATE;
            case "vaccine-card":
                return VACCINE_CARD;
            default:
                return ERROR;
        }
    }

    public static String stringFromDocumentType(DocumentType docType){
        switch (docType) {
            case SOCIAL_SECURITY_CARD:
                return "social-security";
            case DRIVER_LICENSE:
                return "drivers-license";
            case BIRTH_CERTIFICATE:
                return "birth-certificate";
            case VACCINE_CARD:
                return "vaccine-card";
            default:
                return "error";
        }
    }
}
