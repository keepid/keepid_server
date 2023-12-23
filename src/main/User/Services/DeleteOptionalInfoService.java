package User.Services;

import Config.Message;
import Config.Service;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import User.UserMessage;

public class DeleteOptionalInfoService implements Service {
    OptionalUserInformationDao optionalUserInformationDao;
    String username;

    public DeleteOptionalInfoService(OptionalUserInformationDao optionalUserInformationDao, String username){
        this.optionalUserInformationDao = optionalUserInformationDao;
        this.username = username;
    }

    public Message executeAndGetResponse(){
        optionalUserInformationDao.delete(username);
        return UserMessage.SUCCESS;
    }
}
