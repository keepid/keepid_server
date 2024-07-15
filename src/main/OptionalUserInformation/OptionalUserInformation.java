package OptionalUserInformation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class OptionalUserInformation implements Serializable {
  private static final long serialVersionUID = 1L; // Ensure serial version UID is specified

  private ObjectId id;
  private ObjectId userId;
  private String username;
  private Person person;
  private BasicInfo basicInfo;
  private DemographicInfo demographicInfo;
  private FamilyInfo familyInfo;
  private VeteranStatus veteranStatus;

  @JsonIgnore
  public Map<String, Object> getOptionalUserInformation() {
    Map<String, Object> result = new HashMap<>();
    if (person != null) {
      result.put("firstName", person.getFirstName());
      result.put("middleName", person.getMiddleName());
      result.put("lastName", person.getLastName());
      result.put("birthDate", person.getBirthDate());
      result.put("ssn", person.getSsn());
    }
    return result;
  }

  @JsonIgnore
  public Map<String, Object> toMap() {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.convertValue(this, new TypeReference<>() {});
  }

  @JsonIgnore
  public Map<String, Object> getFromString(String key) {
    try {
      switch (key) {
        case "Basic":
          return basicInfo.toMap();
        case "Demographic":
          return demographicInfo.toMap();
        case "Family":
          return familyInfo.toMap();
        case "Veteran":
          return veteranStatus.toMap();
        default:
          return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  public JSONObject serialize() {
    return new JSONObject(this);
  }
}
