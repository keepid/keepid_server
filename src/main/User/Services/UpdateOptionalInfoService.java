package User.Services;

import Config.Message;
import Config.Service;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import User.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.json.JSONObject;

public class UpdateOptionalInfoService implements Service {
    OptionalUserInformationDao optionalUserInformationDao;
    OptionalUserInformation optionalUserInformation;

    public UpdateOptionalInfoService(OptionalUserInformationDao dao, OptionalUserInformation optionalUserInformation) {
        this.optionalUserInformationDao = dao;
        this.optionalUserInformation = optionalUserInformation;
    }

    @Override
    public Message executeAndGetResponse() {
        optionalUserInformationDao.update(optionalUserInformation);
        return UserMessage.SUCCESS;
    }
}
