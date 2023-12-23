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
    ObjectMapper objectMapper;
    OptionalUserInformationDao optionalUserInformationDao;
    String json;

    public UpdateOptionalInfoService(ObjectMapper objectMapper, OptionalUserInformationDao dao, String json) {
        this.objectMapper = objectMapper;
        this.optionalUserInformationDao = dao;
        this.json = json;
    }

    @Override
    public Message executeAndGetResponse() {
        try{
            OptionalUserInformation userInfo = objectMapper.readValue(json, OptionalUserInformation.class);
            optionalUserInformationDao.update(userInfo);
            return UserMessage.SUCCESS;
        } catch (JsonProcessingException e) {
            return UserMessage.INVALID_PARAMETER;
        }
    }
}
