package User.Onboarding;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class OnboardingChecklistResponse {
  private String situation;
  private boolean minimized;
  private List<OnboardingTask> tasks;

  public OnboardingChecklistResponse(OnboardingStatus status) {
    this.situation = status.getSituation();
    this.minimized = status.isMinimized();
    this.tasks = new ArrayList<>();
  }
}
