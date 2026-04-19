package File;

public enum FileType {
  APPLICATION_PDF("APPLICATION_PDF"),
  IDENTIFICATION_PDF("IDENTIFICATION_PDF"),
  FORM("FORM"),
  PROFILE_PICTURE("PROFILE_PICTURE"),
  ORG_DOCUMENT("ORG_DOCUMENT"),
  MISC("MISC");

  private final String fileType;

  FileType(String fileType) {
    this.fileType = fileType;
  }

  public String toString() {
    return this.fileType;
  }

  public static FileType createFromString(String fileTypeString) {
    switch (fileTypeString.toUpperCase()) {
      case "APPLICATION_PDF":
      case "APPLICATION":
        return FileType.APPLICATION_PDF;
      case "IDENTIFICATION":
      case "IDENTIFICATION_PDF":
        return FileType.IDENTIFICATION_PDF;
      case "FORM":
        return FileType.FORM;
      case "PROFILE_PIC":
        return FileType.PROFILE_PICTURE;
      case "ORG_DOCUMENT":
        return FileType.ORG_DOCUMENT;
      case "MISC":
        return FileType.MISC;
      default:
        return null;
    }
  }

  public boolean isPDF() {
    return this == APPLICATION_PDF || this == IDENTIFICATION_PDF || this == FORM || this == ORG_DOCUMENT;
  }

  public boolean isProfilePic() {
    return this == PROFILE_PICTURE;
  }
}
