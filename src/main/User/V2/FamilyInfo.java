package User.V2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class FamilyInfo {
  private List<Person> parents = Collections.emptyList();
  private MaritalStatus maritalStatus;
  private Person spouse;
  private List<Person> children = Collections.emptyList();

  public Map<String, Object> toMap() {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.convertValue(this, new TypeReference<>() {});
  }
}
