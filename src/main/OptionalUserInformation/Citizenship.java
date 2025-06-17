package OptionalUserInformation;

public enum Citizenship {
  US_CITIZEN("U.S. Citizen"),
  LEGAL_ALLOWED_WORK("Legal (Allowed to work)"),
  LEGAL_NOT_ALLOWED_WORK("Legal (Not allowed to work)"),
  OTHER("Other"),
  UNSELECTED("Unselected");

  private final String displayName;

  Citizenship(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
