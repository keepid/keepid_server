package OptionalUserInformation;

import lombok.*;
import org.json.JSONObject;

import java.io.Serializable;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Address implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @NonNull String streetAddress;
  String apartmentNumber;
  @NonNull String city;
  @NonNull String state;
  @NonNull String zip;

  public JSONObject serialize() {
    return new JSONObject(this);
  }
}
