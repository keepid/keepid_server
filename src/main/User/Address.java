package User;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.json.JSONObject;

import java.util.Objects;

public class Address {

  @BsonProperty(value = "line1")
  private String line1;

  @BsonProperty(value = "line2")
  private String line2;

  @BsonProperty(value = "city")
  private String city;

  @BsonProperty(value = "state")
  private String state;

  @BsonProperty(value = "zip")
  private String zip;

  @BsonProperty(value = "county")
  private String county;

  public Address() {}

  public Address(String line1, String city, String state, String zip) {
    this.line1 = line1;
    this.city = city;
    this.state = state;
    this.zip = zip;
  }

  public Address(String line1, String line2, String city, String state, String zip, String county) {
    this.line1 = line1;
    this.line2 = line2;
    this.city = city;
    this.state = state;
    this.zip = zip;
    this.county = county;
  }

  public String getLine1() {
    return line1;
  }

  public void setLine1(String line1) {
    this.line1 = line1;
  }

  public String getLine2() {
    return line2;
  }

  public void setLine2(String line2) {
    this.line2 = line2;
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

  public String getZip() {
    return zip;
  }

  public void setZip(String zip) {
    this.zip = zip;
  }

  public String getCounty() {
    return county;
  }

  public void setCounty(String county) {
    this.county = county;
  }

  public JSONObject serialize() {
    JSONObject json = new JSONObject();
    json.put("line1", line1);
    json.put("line2", line2);
    json.put("city", city);
    json.put("state", state);
    json.put("zip", zip);
    json.put("county", county);
    return json;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (line1 != null) sb.append(line1);
    if (line2 != null && !line2.isBlank()) sb.append(", ").append(line2);
    if (city != null) sb.append(", ").append(city);
    if (state != null) sb.append(", ").append(state);
    if (zip != null) sb.append(" ").append(zip);
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Address address = (Address) o;
    return Objects.equals(line1, address.line1)
        && Objects.equals(line2, address.line2)
        && Objects.equals(city, address.city)
        && Objects.equals(state, address.state)
        && Objects.equals(zip, address.zip)
        && Objects.equals(county, address.county);
  }

  @Override
  public int hashCode() {
    return Objects.hash(line1, line2, city, state, zip, county);
  }
}
