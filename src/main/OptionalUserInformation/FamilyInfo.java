package OptionalUserInformation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class FamilyInfo implements Serializable {
  private static final long serialVersionUID = 1L;
  @NonNull private List<Person> parents = Collections.emptyList();
  @NonNull private List<Person> legalGuardians= Collections.emptyList();
  @NonNull private MaritalStatus maritalStatus;
  private Person spouse;
  @NonNull private List<Person> children = Collections.emptyList();
  @NonNull private List<Person> siblings = Collections.emptyList();

  public Map<String, Object> toMap() {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.convertValue(this, new TypeReference<>() {});
  }

  public JSONObject serialize() {
    return new JSONObject(this);
  }
}
