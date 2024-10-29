package Mail;

import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;
import java.util.Set;

public enum FormMailAddress {
  PA_BIRTH_CERTIFICATE(
      "PA Birth Certificate.pdf",
      "Birth Certificate Address for Pennsylvania",
      "Department of Health Division of Vital Records",
      "VITAL RECORDS",
      "PO Box 1528",
      "",
      "New Castle",
      "PA",
      "16103",
      BigDecimal.valueOf(20.0),
      ImmutableSet.of("PA"),
      ImmutableSet.of("ANY")),
  PA_DRIVERS_LICENSE(
      "PA Drivers License.pdf",
      "Driver's License for Pennsylvania",
      "Bureau of Driver Licensing",
      "PennDOT",
      "P.O. Box 68272",
      "",
      "Harrisburg",
      "PA",
      "17106",
      BigDecimal.valueOf(42.5),
      ImmutableSet.of("PA"),
      ImmutableSet.of("ANY")),
  PA_VOTER_REGISTRATION_PHIL(
      "PA Voter Registration and PA Mail-In Ballot Request for Philadelphia",
      "PA Voter Registration and PA Mail-In Ballot Request specific to county of Philadelphia",
      "City Hall",
      "",
      "1400 John F Kennedy Blvd",
      "Room 142",
      "Philadelphia",
      "PA",
      "19107",
      BigDecimal.ZERO,
      ImmutableSet.of("PA"),
      ImmutableSet.of("Philadelphia")),
  PA_VOTER_REGISTRATION_MONT(
      "PA Voter Registration and PA Mail-In Ballot Request for Montgomery",
      "PA Voter Registration and PA Mail-In Ballot Request specific to county of Montgomery",
      "Montgomery County Voter Services",
      "",
      "425 Swede St",
      "Suite 602",
      "Norristown",
      "PA",
      "19401",
      BigDecimal.ZERO,
      ImmutableSet.of("PA"),
      ImmutableSet.of("Montgomery")),
  PA_VOTER_REGISTRATION_BUCK(
      "PA Voter Registration and PA Mail-In Ballot Request for Bucks",
      "PA Voter Registration and PA Mail-In Ballot Request specific to county of Bucks",
      "",
      "",
      "55 E Court St",
      "",
      "Doylestown",
      "PA",
      "18901",
      BigDecimal.ZERO,
      ImmutableSet.of("PA"),
      ImmutableSet.of("Bucks")),
  PA_VOTER_REGISTRATION_DELA(
      "PA Voter Registration and PA Mail-In Ballot Request for Delaware",
      "PA Voter Registration and PA Mail-In Ballot Request specific to county of Delaware",
      "Govt Center Bldg",
      "",
      "201 W Front St",
      "",
      "Media",
      "PA",
      "19063",
      BigDecimal.ZERO,
      ImmutableSet.of("PA"),
      ImmutableSet.of("Delaware")),
  PA_VOTER_REGISTRATION_CHEST(
      "PA Voter Registration and PA Mail-In Ballot Request for Chester",
      "PA Voter Registration and PA Mail-In Ballot Request specific to county of Chester",
      "",
      "",
      "601 Westtown Rd Ste 150",
      "PO Box 2747",
      "West Chester",
      "PA",
      "19380",
      BigDecimal.ZERO,
      ImmutableSet.of("PA"),
      ImmutableSet.of("Chester")),
  NY_VOTER_REGISTRATION_QUEENS(
      "NY Voter Registration for Queens",
      "NY Voter Registration specific to county of Queens",
      "Queens County Board of Elections",
      "",
      "118-35 Queens Boulevard",
      "11th Floor",
      "Forest Hills",
      "NY",
      "11375",
      BigDecimal.ZERO,
      ImmutableSet.of("NY"),
      ImmutableSet.of("Queens")),

  FL_BIRTH_CERTIFICATE(
        "FL Birth Certificate.pdf",
        "Birth Certificate Address for Florida",
        "FLORIDA DEPARTMENT OF HEALTH BUREAU OF VITAL STATISTICS",
        "VITAL STATISTICS",
        "P.O. BOX 210",
        "",
        "JACKSONVILLE",
        "FL",
        "32231-0042",
        BigDecimal.valueOf(10.0),
        ImmutableSet.of("FL"),
        ImmutableSet.of("ANY")),

  OH_BIRTH_CERTIFICATE(
        "OH Birth Certificate.pdf",
                "Birth Certificate Address for Ohio",
                "Ohio Department of Health Vital Statistics",
                "Bureau of Vital Statistics",
                "P.O. Box 15098 ",
                "",
                "Columbus",
                "OH",
              "43215-0098",
        BigDecimal.valueOf(21.50),
        ImmutableSet.of("OH"),
        ImmutableSet.of("ANY")),

  TX_BIRTH_CERTIFICATE(
        "TX Birth Certificate.pdf",
                "Birth Certificate Address for Texas",
                "DSHS - VSS",
                "DSHS – Vital Statistics",
                "P.O. Box 12040",
                "",
                "Austin",
                "TX",
                "78711-2040",
        BigDecimal.valueOf(22.0),
        ImmutableSet.of("TX"),
        ImmutableSet.of("ANY")),

  MD_BIRTH_CERTIFICATE(
          "MD Birth Certificate.pdf",
          "Birth Certificate Address for Maryland",
          "Maryland Department of Health",
          " DIVISION OF VITAL RECORDS",
          "P.O. Box 68760",
          "",
          "Baltimore",
          "MD",
          "21215-0036",
          BigDecimal.valueOf(10.0),
          ImmutableSet.of("MD"),
          ImmutableSet.of("ANY"))
  ;

  private String name;
  private String description;
  private String office_name;
  private String nameForCheck;
  private String street1;
  private String street2;
  private String city;
  private String state;
  private String zipcode;
  private BigDecimal maybeCheckAmount;
  private Set<String> acceptable_states;
  private Set<String> acceptable_counties;

  <E> FormMailAddress(
      String name,
      String description,
      String office_name,
      String nameForCheck,
      String street1,
      String street2,
      String city,
      String state,
      String zipcode,
      BigDecimal maybeCheckAmount,
      Set<String> acceptable_states,
      Set<String> acceptable_counties) {
    this.name = name;
    this.description = description;
    this.office_name = office_name;
    this.nameForCheck = nameForCheck;
    this.street1 = street1;
    this.street2 = street2;
    this.city = city;
    this.state = state;
    this.zipcode = zipcode;
    this.maybeCheckAmount = maybeCheckAmount;
    this.acceptable_states = acceptable_states;
    this.acceptable_counties = acceptable_counties;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("FormMailAddress {");
    sb.append(", name=").append(this.name);
    sb.append(", description=").append(this.description);
    sb.append(", office_name=").append(this.office_name);
    sb.append(", nameForCheck=").append(this.nameForCheck);
    sb.append(", maybeCheckAmount=").append(this.maybeCheckAmount.toString());
    sb.append(", street1=").append(this.street1);
    sb.append(", street2=").append(this.street2);
    sb.append(", city=").append(this.city);
    sb.append(", state=").append(this.state);
    sb.append(", zipcode=").append(this.zipcode);
    sb.append("}");
    return sb.toString();
  }

  public BigDecimal getMaybeCheckAmount() {
    return maybeCheckAmount;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getOffice_name() {
    return office_name;
  }

  public String getStreet1() {
    return street1;
  }

  public String getStreet2() {
    return street2;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getZipcode() {
    return zipcode;
  }

  public Set<String> getAcceptable_states() {
    return acceptable_states;
  }

  public Set<String> getAcceptable_counties() {
    return acceptable_counties;
  }

  public String getNameForCheck() {
    return nameForCheck;
  }
}
