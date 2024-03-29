package User.V2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class BasicInfo {

  private String middleName;
  private String suffix;
  // false is male, true is female
  private Boolean genderAssignedAtBirth;
  private Address mailingAddress;
  private Address residentialAddress;
  private String emailAddress;
  private String phoneNumber;
  private Boolean differentBirthName;
  private String birthFirstName;
  private String birthMiddleName;
  private String birthLastName;
  private String birthSuffix;
  @NonNull private String ssn;
  private Boolean haveDisability;
  private String idNumber;

  public Map<String, Object> toMap() {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.convertValue(this, new TypeReference<>() {});
  }
}
