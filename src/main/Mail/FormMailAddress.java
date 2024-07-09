package Mail;

import com.google.common.collect.ImmutableSet;
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
      ImmutableSet.of("PA"),
      ImmutableSet.of("Chester"));

  private String name;
  private String description;
  private String office_name;
  private String nameForCheck;
  private String street1;
  private String street2;
  private String city;
  private String state;
  private String zipcode;
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
    this.acceptable_states = acceptable_states;
    this.acceptable_counties = acceptable_counties;
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
