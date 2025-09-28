package User.Services;

import Config.Message;
import Config.Service;
import Database.File.FileDao;
import Database.Form.FormDao;
import Database.User.UserDao;
import User.UserMessage;
import User.Onboarding.OnboardingChecklistResponse;
import User.Onboarding.OnboardingStatus;
import User.Onboarding.OnboardingTask;
import User.User;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
public class GetOnboardingChecklistService implements Service {
  private String username;
  private String originUri;
  private UserDao userDao;
  private FormDao formDao;
  private FileDao fileDao;
  private OnboardingChecklistResponse onboardingChecklistResponse;

  public GetOnboardingChecklistService(UserDao userDao, FormDao
      formDao, FileDao fileDao, String username, String originUri) {
    this.userDao = userDao;
    this.formDao = formDao;
    this.fileDao = fileDao;
    this.username = username;
    // TODO: validate origin
    this.originUri = originUri;
  }

  @Override
  public Message executeAndGetResponse() throws Exception {
    log.info("Started getOnboardingStatus service");

    log.info("Retrieving user from UserDao");
    Optional<User> optionalUser = userDao.get(username);
    if (optionalUser.isEmpty()) {
      log.info("User not found in UserDao");
      return UserMessage.USER_NOT_FOUND;
    }
    User user = optionalUser.get();
    OnboardingStatus onboardingStatus = user.getOnboardingStatus();
    log.info("Retrieved onboardingStatus: {}", onboardingStatus);

    if (onboardingStatus != null) {
      log.info("User's onboarding status is non-null, generating checklist");
      generateChecklist(onboardingStatus);
      log.info("Generated checklist: {}", onboardingChecklistResponse);
    } else {
      OnboardingStatus defaultOnboardingStatus = new OnboardingStatus();
      this.onboardingChecklistResponse = new OnboardingChecklistResponse(defaultOnboardingStatus);
      log.info("User's onboarding status is null, creating default onboardingStatus: {}",
          defaultOnboardingStatus);
      userDao.update(user.setOnboardingStatus(defaultOnboardingStatus));
    }

    return UserMessage.AUTH_SUCCESS;
  }

  private void generateChecklist(OnboardingStatus onboardingStatus) {
    this.onboardingChecklistResponse = new OnboardingChecklistResponse(onboardingStatus);
    if (onboardingStatus.isMinimized()) {
      log.info("User's onboarding status is minimized, no need to generate a checklist");
      return;
    }
    if (onboardingStatus.getSituation().equals("none")){
      log.info("User's onboarding status is none, no need to generate a checklist");
      return;
    }

    switch (onboardingStatus.getSituation()) {
      case "apply-id":
        createApplyIdChecklist();
        break;
      case "upload-id":
        createUploadIdChecklist();
        break;
      default:
        log.info("Unknown onboarding status situation: {}", onboardingStatus.getSituation());
    }
  }

  private void createApplyIdChecklist() {
    log.info("User's onboarding status is apply-id, checking if there is an existing ID " +
        "application");

    // Only step, check if they submitted an ID application
    OnboardingTask applyForId = new OnboardingTask();
    applyForId.setId(1);
    applyForId.setTitle("Apply for your government-issued ID");
    applyForId.setLink(originUri + "/applications");
    applyForId.setLinkText("Applications Portal");
    // ID applications are in form DAO?
    try {
      applyForId.setComplete(formDao.get(username) != null && !formDao.get(username).isEmpty());
    } catch (Exception e) {
      log.error("Error retrieving forms for user");
      applyForId.setComplete(false);
    }
    onboardingChecklistResponse.getTasks().add(applyForId);
  }

  private void createUploadIdChecklist() {
    log.info("User's onboarding status is upload-id, checking if there is an ID uploaded");

    // Only step, check if they uploaded an ID
    OnboardingTask uploadId = new OnboardingTask();
    uploadId.setId(1);
    uploadId.setTitle("Upload your government-issued ID");
    uploadId.setLink(originUri + "/my-documents");
    uploadId.setLinkText("My Documents");
    // ID uploads are in file DAO?
    try {
      uploadId.setComplete(fileDao.getAll(username) != null && !fileDao.getAll(username).isEmpty());
    } catch (Exception e) {
      log.error("Error retrieving files for user");
      uploadId.setComplete(false);
    }
    onboardingChecklistResponse.getTasks().add(uploadId);
  }
}
