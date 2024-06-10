package Mail;

import Config.Message;
import Database.Mail.MailDao;
import Mail.Services.SaveFormMailAddressService;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.util.Date;

@Slf4j
public class MailController{
    private MailDao mailDao;

    public MailController(MailDao mailDao){
        this.mailDao = mailDao;
    }

    public Handler getFormMailAddresses =
            ctx -> {
                FormMailAddress[] formMailAddresses = FormMailAddress.values();
                JSONObject response = new JSONObject(formMailAddresses);
                ctx.result(response.toString());
            };

    public Handler saveMail =
            ctx -> {
                JSONObject request = new JSONObject(ctx.body());
                ObjectMapper objectMapper = new ObjectMapper();
                try{
                    FormMailAddress formMailAddress = objectMapper.readValue(request.getString("mailing_address"), FormMailAddress.class);
                    Mail mail = new Mail(null, null, formMailAddress, null, null);
                    SaveFormMailAddressService saveFormMailAddressService = new SaveFormMailAddressService(mailDao, mail);
                    Message response = saveFormMailAddressService.executeAndGetResponse();
                    ctx.result(response.toJSON().toString());
                } catch ( JsonMappingException jsonMappingException ){
                    Message response = MailMessage.FAILED_WHEN_MAPPING_FORM_MAIL_ADDRESS;
                    ctx.result(response.toJSON().toString());
                }

            };
}
