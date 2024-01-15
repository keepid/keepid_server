package UserV2.Services;

import Config.Message;
import Config.Service;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import UserV2.*;

public class UpdateOptionalInfoService implements Service {
    OptionalUserInformationDao optionalUserInformationDao;
    OptionalUserInformation optionalUserInformation;

    public UpdateOptionalInfoService(OptionalUserInformationDao dao, OptionalUserInformation optionalUserInformation) {
        this.optionalUserInformationDao = dao;
        this.optionalUserInformation = optionalUserInformation;
    }

    @Override
    public Message executeAndGetResponse() {
        if(optionalUserInformationDao.get(optionalUserInformation.getUsername()).isEmpty()){
            return UserMessage.USER_NOT_FOUND;
        }
        optionalUserInformationDao.update(optionalUserInformation);
        return UserMessage.SUCCESS;
    }
}
