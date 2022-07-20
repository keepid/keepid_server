package File;

public enum FileType {
  APPLICATION_PDF("APPLICATION_PDF"),
  IDENTIFICATION_PDF("IDENTIFICATION_PDF"),
  FORM_PDF("FORM"),
  PROFILE_PICTURE("PROFILE_PICTURE"),
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
      case "APPLICATION":
        return FileType.APPLICATION_PDF;
      case "IDENTIFICATION":
        return FileType.IDENTIFICATION_PDF;
      case "FORM":
        return FileType.FORM_PDF;
      case "PROFILE_PIC":
        return FileType.PROFILE_PICTURE;
      case "MISC":
        return FileType.MISC;
      default:
        return null;
    }
  }

  public boolean isPDF() {
    return this == APPLICATION_PDF || this == IDENTIFICATION_PDF || this == FORM_PDF;
  }

  public boolean isProfilePic() {
    return this == PROFILE_PICTURE;
  }
}
