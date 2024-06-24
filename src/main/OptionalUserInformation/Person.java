package OptionalUserInformation;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.json.JSONObject;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Person implements Serializable {
  private static final long serialVersionUID = 1L;

  private String firstName;
  private String middleName;
  private String lastName;
  private String ssn;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private String birthDate;

  public JSONObject serialize() {
    return new JSONObject(this);
  }
}
