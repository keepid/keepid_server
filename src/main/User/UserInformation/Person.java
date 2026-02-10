package User.UserInformation;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Getter
@Setter
@Builder(toBuilder = true)
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class Person implements Serializable {
  private static final long serialVersionUID = 1L;

  private String firstName;
  private String middleName;
  private String lastName;
  private String ssn;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private Date birthDate;

  public JSONObject serialize() {
    return new JSONObject(this);
  }
}
