package User.Onboarding;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
@ToString
public class OnboardingStatus {
  private String situation = "none";
  private boolean minimized = false;

  public static boolean isValidOnboardingSituation(String situation) {
    return situation.equals("none") || situation.equals("apply-id") || situation.equals("upload-id");
  }
}
