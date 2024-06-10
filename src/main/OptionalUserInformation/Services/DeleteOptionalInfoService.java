package OptionalUserInformation.Services;

import Config.Message;
import Config.Service;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import OptionalUserInformation.UserMessage;

public class DeleteOptionalInfoService implements Service {
    OptionalUserInformationDao optionalUserInformationDao;
    String username;

    public DeleteOptionalInfoService(OptionalUserInformationDao optionalUserInformationDao, String username){
        this.optionalUserInformationDao = optionalUserInformationDao;
        this.username = username;
    }

    public Message executeAndGetResponse(){
        if(optionalUserInformationDao.get(username).isEmpty()){
            return UserMessage.USER_NOT_FOUND;
        }
        optionalUserInformationDao.delete(username);
        return UserMessage.SUCCESS;
    }
}
