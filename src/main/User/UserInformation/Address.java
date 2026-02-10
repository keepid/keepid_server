package User.UserInformation;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Serializable;
import lombok.*;
import org.json.JSONObject;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Address implements Serializable {
  private static final long serialVersionUID = 1L;

  String streetAddress;
  String apartmentNumber;
  String city;
  String state;
  String zip;

  public JSONObject serialize() {
    return new JSONObject(this);
  }

  @Override
  public String toString() {
    if (apartmentNumber != null) {
      return streetAddress + ", " + apartmentNumber + ", " + city + ", " + state + " " + zip;
    } else {
      return streetAddress + ", " + city + ", " + state + " " + zip;
    }
  }

  public boolean equals(Address other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (other.getClass() != this.getClass()) {
      return false;
    }
    if (this.streetAddress.equals(other.streetAddress)
        && this.city.equals(other.city)
        && this.state.equals(other.state)
        && this.zip.equals(other.zip)) {
      if (isEmpty(this.apartmentNumber) && isEmpty(other.apartmentNumber)) {
        return true;
      }
      return !isEmpty(apartmentNumber) && this.apartmentNumber.equals(other.apartmentNumber);
    }
    return false;
  }
}
