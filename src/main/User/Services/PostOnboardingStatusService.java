package User.Services;

import Config.Message;
import Config.Service;
import Database.User.UserDao;
import User.Onboarding.OnboardingStatus;
import User.User;
import User.UserMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Getter
@Slf4j
@AllArgsConstructor
public class PostOnboardingStatusService implements Service {
  private UserDao userDao;
  private String username;
  private OnboardingStatus onboardingStatus;


  @Override
  public Message executeAndGetResponse() throws Exception {
    log.info("Started postOnboardingStatus service");

    log.info("Retrieving user UserDao");
    Optional<User> optionalUser = userDao.get(username);
    if (optionalUser.isEmpty()) {
      log.info("User not found");
      return UserMessage.USER_NOT_FOUND;
    }
    if (!OnboardingStatus.isValidOnboardingSituation(onboardingStatus.getSituation())) {
      log.info("Invalid onboarding situation {}", onboardingStatus.getSituation());
      return UserMessage.AUTH_FAILURE;
    }
    User user = optionalUser.get();
    OnboardingStatus prevOnboardingStatus = user.getOnboardingStatus();
    log.info("User's previous onboardingStatus: {}", prevOnboardingStatus);

    log.info("Set user's onboarding status to: {}", onboardingStatus);
    userDao.update(user.setOnboardingStatus(onboardingStatus));

    return UserMessage.AUTH_SUCCESS;
  }
}
