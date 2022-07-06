package PDF;

public enum PDFType {
  COMPLETED_APPLICATION("COMPLETED_APPLICATION"),
  IDENTIFICATION_DOCUMENT("IDENTIFICATION_DOCUMENT"),
  BLANK_FORM("BLANK_FORM");

  private String pdfType;

  PDFType(String pdfType) {
    this.pdfType = pdfType;
  }

  public String toString() {
    return this.pdfType;
  }

  public static PDFType createFromString(String pdfTypeString) {
    switch (pdfTypeString) {
      case "COMPLETED_APPLICATION":
        return PDFType.COMPLETED_APPLICATION;
      case "IDENTIFICATION_DOCUMENT":
        return PDFType.IDENTIFICATION_DOCUMENT;
      case "BLANK_FORM":
        return PDFType.BLANK_FORM;
      default:
        return null;
    }
  }
}
