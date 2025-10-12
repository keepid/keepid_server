package Form;

public enum PIDLSubtype {
  DL("Driver's License"),
  PI("Photo Id");

  private final String description;

  PIDLSubtype(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return this.description;
  }
}
