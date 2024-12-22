package Form.Services;

import Config.Message;
import Config.Service;
import Form.FormMessage;
import Form.ApplicationRegistry;
import org.json.JSONObject;

import java.util.Objects;

public class GetApplicationRegistryService implements Service {
    String type;
    String state;
    String situation;
    String person;
    String applicationRegistry;

    public GetApplicationRegistryService(String type, String state, String situation, String person) {
        this.type = type;
        this.state = state;
        this.situation = situation;
        this.person = person;
    }

    public String getJsonInformation() {
        Objects.requireNonNull(this.applicationRegistry);
        return this.applicationRegistry;
    }

    @Override
    public Message executeAndGetResponse() throws Exception {
        ApplicationRegistry appReg;
        try {
            appReg = ApplicationRegistry.valueOf(type + "$" + state + "$" + situation);
        } catch (IllegalArgumentException e) {
            return FormMessage.INVALID_PARAMETER;
        }
        this.applicationRegistry = appReg.toString();
        return FormMessage.SUCCESS;
    }
}
