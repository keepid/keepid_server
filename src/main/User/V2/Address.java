package User.V2;

import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Address {

  String streetAddress;
  String apartmentNumber;
  String city;
  String state;
  String zip;
}
