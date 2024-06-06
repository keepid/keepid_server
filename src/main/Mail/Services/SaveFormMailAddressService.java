package Mail.Services;

import Config.Message;
import Config.Service;
import Database.Mail.MailDao;
import Mail.Mail;
import Mail.FormMailAddress;
import Mail.MailMessage;

import com.lob.api.ApiClient;
import com.lob.api.Configuration;
import com.lob.api.auth.*;
import com.lob.api.client.AddressesApi;
import com.lob.api.client.LettersApi;
import org.openapitools.client.model.*;

public class SaveFormMailAddressService implements Service {
    private MailDao mailDao;
    private Mail mail;

    public SaveFormMailAddressService(MailDao mailDao, Mail mail){
        this.mailDao = mailDao;
        this.mail = mail;
    }

    @Override
    public Message executeAndGetResponse() {
        mailDao.save(mail); //Save created mail
        FormMailAddress address = mail.getMailing_address();

        ApiClient lobClient = Configuration.getDefaultApiClient();

        // Configure HTTP basic authorization: basicAuth
        HttpBasicAuth basicAuth = (HttpBasicAuth) lobClient.getAuthentication("basicAuth");
        basicAuth.setUsername("<YOUR_LOB_API_KEY>");

        AddressesApi addressesApi = new AddressesApi(lobClient);
        LettersApi lettersApi = new LettersApi(lobClient);
        AddressEditable addressEditable = new AddressEditable();
        addressEditable.setName(address.getName());
        addressEditable.setAddressLine1(address.getStreet1());
        addressEditable.setAddressLine2(address.getStreet2());
        addressEditable.setAddressState(address.getState());
        addressEditable.setAddressCity(address.getCity());
        addressEditable.setAddressZip(address.getZipcode());
        addressEditable.setDescription(address.getDescription());
        addressEditable.setCompany(address.getOffice_name()); //Is office name company?

        try {

            Address to = addressesApi.create(addressEditable);
            Address from = addressesApi.get(""); //sender address id on lob account

            AddressEditable fromEditable = new AddressEditable();
            fromEditable.setName(from.getName());
            fromEditable.setAddressLine1(from.getAddressLine1());
            fromEditable.setAddressLine2(from.getAddressLine2());
            fromEditable.setAddressState(from.getAddressState());
            fromEditable.setAddressCity(from.getAddressCity());
            fromEditable.setAddressZip(from.getAddressZip());
            fromEditable.setDescription(from.getDescription());
            fromEditable.setCompany(from.getCompany());

            LetterEditable letterEditable = new LetterEditable();
            letterEditable.setTo(addressEditable);
            letterEditable.setFrom(fromEditable);
            letterEditable.setFile(""); //To be confirmed
            letterEditable.setColor(false); //To be confirmed
            letterEditable.setMailType(MailType.STANDARD);

            Letter letter = lettersApi.create(letterEditable, ""); //Key goes there


            this.mail.setLob_id(letter.getId());
            this.mail.setLob_created_at(letter.getDateCreated()); //Date type not match
            mailDao.update(this.mail);

            return MailMessage.MAIL_SUCCESS;
            
        } catch (Exception e) {
            return MailMessage.FAILED_WHEN_SENDING_MAIL;
        }
        return null;
    }
    
}
