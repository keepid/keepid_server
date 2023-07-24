package PDF;

public enum PDFTypeV2 {
  ANNOTATED_APPLICATION("ANNOTATED_APPLICATION"),
  BLANK_APPLICATION("BLANK_APPLICATION"),
  CLIENT_UPLOADED_DOCUMENT("CLIENT_UPLOADED_DOCUMENT");

  private final String pdfType;

  PDFTypeV2(String pdfType) {
    this.pdfType = pdfType;
  }

  public String toString() {
    return this.pdfType;
  }

  public static PDFTypeV2 createFromString(String pdfTypeString) {
    switch (pdfTypeString) {
      case "ANNOTATED_APPLICATION":
        return PDFTypeV2.ANNOTATED_APPLICATION;
      case "BLANK_APPLICATION":
        return PDFTypeV2.BLANK_APPLICATION;
      case "CLIENT_UPLOADED_DOCUMENT":
        return PDFTypeV2.CLIENT_UPLOADED_DOCUMENT;
      default:
        return null;
    }
  }
}
