package OptionalUserInformation.Services;

import Activity.UserActivity.ChangeOptionalUserInformationActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import OptionalUserInformation.*;

public class UpdateOptionalInfoService implements Service {
  OptionalUserInformationDao optionalUserInformationDao;
  ActivityDao activityDao;
  OptionalUserInformation optionalUserInformation;

  public UpdateOptionalInfoService(
      OptionalUserInformationDao dao,
      ActivityDao activityDao,
      OptionalUserInformation optionalUserInformation) {
    this.optionalUserInformationDao = dao;
    this.activityDao = activityDao;
    this.optionalUserInformation = optionalUserInformation;
  }

  @Override
  public Message executeAndGetResponse() {
    if (optionalUserInformationDao.get(optionalUserInformation.getUsername()).isEmpty()) {
      return UserMessage.USER_NOT_FOUND;
    }
    optionalUserInformationDao.update(optionalUserInformation);
    recordChangeOptionalUserInformation();
    return UserMessage.SUCCESS;
  }

  private void recordChangeOptionalUserInformation() {
    ChangeOptionalUserInformationActivity a =
        new ChangeOptionalUserInformationActivity(optionalUserInformation.getUsername());
    activityDao.save(a);
  }
}
