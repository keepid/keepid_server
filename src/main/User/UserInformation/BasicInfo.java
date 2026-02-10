package User.UserInformation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class BasicInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  // M is male, F is female
  private String genderAssignedAtBirth;
  private Address mailingAddress;
  private Address residentialAddress;
  private String emailAddress;
  private String phoneNumber;
  private Boolean differentBirthName;
  private String firstName;
  private String middleName;
  private String lastName;
  private String suffix;
  private String birthFirstName;
  private String birthMiddleName;
  private String birthLastName;
  private String birthSuffix;
  private String ssn;
  private Boolean haveDisability;
  private String stateIdNumber;

  public Map<String, Object> toMap() {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.convertValue(this, new TypeReference<>() {});
  }

  public JSONObject serialize() {
    return new JSONObject(this);
  }
}
