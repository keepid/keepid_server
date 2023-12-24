package User.Services;

import Config.Message;
import Config.Service;
import Database.OptionalUserInformation.OptionalUserInformationDao;
import User.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

public class GetOptionalInfoService implements Service {
    OptionalUserInformationDao optionalUserInformationDao;
    String username;
    OptionalUserInformation optionalUserInformation;

    public GetOptionalInfoService(OptionalUserInformationDao dao, String username) {
        this.optionalUserInformationDao = dao;
        this.username = username;
        this.optionalUserInformation = null;
    }

    @Override
    public Message executeAndGetResponse() {
        Optional<OptionalUserInformation> userOptional = optionalUserInformationDao.get(username);
        if (userOptional.isPresent()) {
            this.optionalUserInformation = userOptional.get();
            return UserMessage.SUCCESS;
        }
        return UserMessage.USER_NOT_FOUND;
    }

    public JSONObject getOptionalInformationFields(){
        checkState(optionalUserInformation != null, "OptionalInfo must exist");
        JSONObject jsonObject = new JSONObject(optionalUserInformation);
        return jsonObject;
    }
}