package User.Onboarding;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class OnboardingTask {
  private int id;
  private boolean isComplete;
  private String title;
  private String link;
  private String linkText;
}
