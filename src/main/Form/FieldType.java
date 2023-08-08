package Form;

public enum FieldType {
  DATE_FIELD("dateField"),
  MULTILINE_TEXT_FIELD("multilineTextField"),
  READ_ONLY_FIELD("readOnlyField"),
  TEXT_FIELD("textField"),
  CHECKBOX("checkBox"),
  RADIO_BUTTON("radioButton"),
  COMBOBOX("comboBox"),
  LISTBOX("listBox"),
  SIGNATURE("signature");

  private String fieldType;

  FieldType(String fieldType) {
    this.fieldType = fieldType;
  }

  public String toString() {
    return this.fieldType;
  }

  public static FieldType createFromString(String fieldTypeString) {
    switch (fieldTypeString) {
      case "dateField":
        return FieldType.DATE_FIELD;
      case "readOnlyField":
        return FieldType.READ_ONLY_FIELD;
      case "multilineTextField":
        return FieldType.MULTILINE_TEXT_FIELD;
      case "textField":
        return FieldType.TEXT_FIELD;
      case "checkBox":
        return FieldType.CHECKBOX;
      case "radioButton":
        return FieldType.RADIO_BUTTON;
      case "comboBox":
        return FieldType.COMBOBOX;
      case "listBox":
        return FieldType.LISTBOX;
      case "signature":
        return FieldType.SIGNATURE;
      default:
        throw new IllegalStateException("Error: Illegal field type: " + fieldTypeString);
    }
  }
}
