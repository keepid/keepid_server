package Form;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum USStateOrCity {
  AL("Alabama", false),
  AK("Alaska", false),
  AZ("Arizona", false),
  AR("Arkansas", false),
  CA("California", false),
  CO("Colorado", false),
  CT("Connecticut", false),
  DE("Delaware", false),
  FL("Florida", false),
  GA("Georgia", false),
  HI("Hawaii", false),
  ID("Idaho", false),
  IL("Illinois", false),
  IN("Indiana", false),
  IA("Iowa", false),
  KS("Kansas", false),
  KY("Kentucky", false),
  LA("Louisiana", false),
  ME("Maine", false),
  MD("Maryland", false),
  MA("Massachusetts", false),
  MI("Michigan", false),
  MN("Minnesota", false),
  MS("Mississippi", false),
  MO("Missouri", false),
  MT("Montana", false),
  NE("Nebraska", false),
  NV("Nevada", false),
  NH("New Hampshire", false),
  NJ("New Jersey", true),
  NM("New Mexico", false),
  NY("New York", false),
  NC("North Carolina", false),
  ND("North Dakota", false),
  OH("Ohio", false),
  OK("Oklahoma", false),
  OR("Oregon", false),
  PA("Pennsylvania", true),
  RI("Rhode Island", false),
  SC("South Carolina", false),
  SD("South Dakota", false),
  TN("Tennessee", false),
  TX("Texas", false),
  UT("Utah", false),
  VT("Vermont", false),
  VA("Virginia", false),
  WA("Washington", false),
  WV("West Virginia", false),
  WI("Wisconsin", false),
  WY("Wyoming", false),
  NYC("New York City", true),
  FED("Federal", true);

  private final String fullName;
  private final boolean isActive;

  USStateOrCity(String fullName, boolean isActive) {
    this.fullName = fullName;
    this.isActive = isActive;
  }

  public String getFullName() {
    return fullName;
  }

  public boolean isActive() {
    return this.isActive;
  }

  @Override
  public String toString() {
    return this.fullName;
  }

  public static List<USStateOrCity> getActiveStatesOrCities() {
    return Arrays.stream(USStateOrCity.values())
        .filter(USStateOrCity::isActive)
        .collect(Collectors.toList());
  }
}
