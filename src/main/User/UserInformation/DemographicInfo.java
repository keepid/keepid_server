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
public class DemographicInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  private String languagePreference;
  // false is Non-Hispanic/Latino, true is Hispanic/Latino
  private Boolean isEthnicityHispanicLatino;
  private Race race;
  private String cityOfBirth;
  private String stateOfBirth;
  private String countryOfBirth;
  private Citizenship citizenship;

  public Map<String, Object> toMap() {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.convertValue(this, new TypeReference<>() {});
  }

  public JSONObject serialize() {
    return new JSONObject(this);
  }
}
