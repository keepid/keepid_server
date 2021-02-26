package Organization;

import User.User;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class EnrollOrgRequest {

  private User user;
  private Organization organization;
}
