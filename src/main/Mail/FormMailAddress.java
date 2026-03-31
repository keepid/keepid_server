package Mail;


import java.math.BigDecimal;
import java.util.Set;

public class FormMailAddress {

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

  public FormMailAddress() {}

  public FormMailAddress(
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
    sb.append("name=").append(this.name);
    sb.append(", description=").append(this.description);
    sb.append(", office_name=").append(this.office_name);
    sb.append(", nameForCheck=").append(this.nameForCheck);
    sb.append(", maybeCheckAmount=").append(this.maybeCheckAmount != null ? this.maybeCheckAmount.toString() : "null");
    sb.append(", street1=").append(this.street1);
    sb.append(", street2=").append(this.street2);
    sb.append(", city=").append(this.city);
    sb.append(", state=").append(this.state);
    sb.append(", zipcode=").append(this.zipcode);
    sb.append("}");
    return sb.toString();
  }

  public BigDecimal getMaybeCheckAmount() {
    return maybeCheckAmount != null ? maybeCheckAmount : BigDecimal.ZERO;
  }

  public void setMaybeCheckAmount(BigDecimal maybeCheckAmount) {
    this.maybeCheckAmount = maybeCheckAmount;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getOffice_name() {
    return office_name;
  }

  public void setOffice_name(String office_name) {
    this.office_name = office_name;
  }

  public String getNameForCheck() {
    return nameForCheck;
  }

  public void setNameForCheck(String nameForCheck) {
    this.nameForCheck = nameForCheck;
  }

  public String getStreet1() {
    return street1;
  }

  public void setStreet1(String street1) {
    this.street1 = street1;
  }

  public String getStreet2() {
    return street2;
  }

  public void setStreet2(String street2) {
    this.street2 = street2;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getZipcode() {
    return zipcode;
  }

  public void setZipcode(String zipcode) {
    this.zipcode = zipcode;
  }

  public Set<String> getAcceptable_states() {
    return acceptable_states;
  }

  public void setAcceptable_states(Set<String> acceptable_states) {
    this.acceptable_states = acceptable_states;
  }

  public Set<String> getAcceptable_counties() {
    return acceptable_counties;
  }

  public void setAcceptable_counties(Set<String> acceptable_counties) {
    this.acceptable_counties = acceptable_counties;
  }
}
