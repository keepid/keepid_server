package User;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.Objects;

public class PhoneBookEntry {

  public static final String PRIMARY_LABEL = "primary";

  @BsonProperty(value = "label")
  private String label;

  @BsonProperty(value = "phoneNumber")
  private String phoneNumber;

  public PhoneBookEntry() {}

  public PhoneBookEntry(String label, String phoneNumber) {
    this.label = label;
    this.phoneNumber = phoneNumber;
  }

  public boolean hasPrimaryLabel() {
    return PRIMARY_LABEL.equalsIgnoreCase(label);
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PhoneBookEntry that = (PhoneBookEntry) o;
    return Objects.equals(label, that.label)
        && Objects.equals(phoneNumber, that.phoneNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, phoneNumber);
  }

  @Override
  public String toString() {
    return "PhoneBookEntry{"
        + "label='" + label + '\''
        + ", phoneNumber='" + phoneNumber + '\''
        + '}';
  }
}
