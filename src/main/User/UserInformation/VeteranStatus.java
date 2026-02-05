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
public class VeteranStatus implements Serializable {
  private static final long serialVersionUID = 1L;

  @NonNull private boolean isVeteran;
  @NonNull private boolean isProtectedVeteran;
  private String branch;
  private String yearsOfService;
  private String rank;
  private String discharge;

  public Map<String, Object> toMap() {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.convertValue(this, new TypeReference<>() {});
  }

  public JSONObject serialize() {
    JSONObject json = new JSONObject();
    json.put("isVeteran", isVeteran);
    json.put("isProtectedVeteran", isProtectedVeteran);
    if (branch != null) {
      json.put("branch", branch);
    }
    if (yearsOfService != null) {
      json.put("yearsOfService", yearsOfService);
    }
    if (rank != null) {
      json.put("rank", rank);
    }
    if (discharge != null) {
      json.put("discharge", discharge);
    }
    return json;
  }
}
