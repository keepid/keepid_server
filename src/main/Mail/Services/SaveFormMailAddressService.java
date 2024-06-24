package Mail.Services;

import Config.Message;
import Config.Service;
import Database.Mail.MailDao;
import Mail.FormMailAddress;
import Mail.Mail;
import Mail.MailMessage;
import com.lob.api.ApiClient;
import com.lob.api.Configuration;
import com.lob.api.auth.*;
import com.lob.api.client.AddressesApi;
import com.lob.api.client.LettersApi;
import org.openapitools.client.model.*;
import org.threeten.bp.DateTimeUtils;

public class SaveFormMailAddressService implements Service {
  private MailDao mailDao;
  private Mail mail;
  public final String LOB_API_KEY = System.getenv("LOB_API_KEY");

  public SaveFormMailAddressService(MailDao mailDao, Mail mail) {
    this.mailDao = mailDao;
    this.mail = mail;
  }

  @Override
  public Message executeAndGetResponse() {
    mailDao.save(mail); // Save created mail
    FormMailAddress address = mail.getMailingAddress();

    ApiClient lobClient = Configuration.getDefaultApiClient();

    HttpBasicAuth basicAuth = (HttpBasicAuth) lobClient.getAuthentication("basicAuth");
    basicAuth.setUsername(LOB_API_KEY);

    AddressesApi addressesApi = new AddressesApi(lobClient);
    LettersApi lettersApi = new LettersApi(lobClient);
    AddressEditable toAddress = new AddressEditable();
    toAddress.setName(address.getName());
    toAddress.setAddressLine1(address.getStreet1());
    toAddress.setAddressLine2(address.getStreet2());
    toAddress.setAddressState(address.getState());
    toAddress.setAddressCity(address.getCity());
    toAddress.setAddressZip(address.getZipcode());
    toAddress.setDescription(address.getDescription());
    toAddress.setCompany(address.getOffice_name()); // Is office name company?

    try {

      Address from = addressesApi.get(""); // sender address id on lob account
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
      letterEditable.setTo(toAddress);
      letterEditable.setFrom(fromEditable);
      letterEditable.setFile(""); // To be confirmed
      letterEditable.setColor(false); // To be confirmed
      letterEditable.setMailType(MailType.STANDARD);

      Letter letter = lettersApi.create(letterEditable, ""); // Key goes there
      this.mail.setLobId(letter.getId());
      this.mail.setLobCreatedAt(DateTimeUtils.toDate(letter.getDateCreated().toInstant()));
      mailDao.update(this.mail);

      return MailMessage.MAIL_SUCCESS;

    } catch (Exception e) {
      return MailMessage.FAILED_WHEN_SENDING_MAIL;
    }
  }
}
