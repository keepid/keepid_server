package OptionalUserInformation;

public enum Race {
  NATIVE_HAWAIIAN("Native Hawaiian"),
  ALASKA_NATIVE("Alaska Native"),
  ASIAN("Asian"),
  AMERICAN_INDIAN("American Indian"),
  BLACK_AFRICAN_AMERICAN("Black or African American"),
  OTHER_PACIFIC_ISLANDER("Other Pacific Islander"),
  WHITE("White / Caucasian"),
  UNSELECTED("Unselected");

  private final String displayName;

  Race(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
