package Mail;

import Config.Message;
import Database.Mail.MailDao;
import Mail.Services.SaveFormMailAddressService;
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
                ObjectId form_id = new ObjectId(request.getString("form_id"));
                ObjectId file_id = new ObjectId(request.getString("file_id"));
                ObjectMapper objectMapper = new ObjectMapper();
                FormMailAddress formMailAddress = objectMapper.readValue(request.getString("mailing_address"), FormMailAddress.class);
                String lob_id = request.getString("lob_id");
                Date lob_created_at = objectMapper.readValue(request.getString("lob_created_at"), Date.class);

                Mail mail = new Mail(form_id, file_id, formMailAddress, lob_id, lob_created_at);
                SaveFormMailAddressService saveFormMailAddressService = new SaveFormMailAddressService(mailDao, mail);
                Message response = saveFormMailAddressService.executeAndGetResponse();
                ctx.result(response.toJSON().toString());
            };
}
