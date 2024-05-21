package Mail.Services;

import Config.Message;
import Config.Service;
import Database.Mail.MailDao;
import Mail.Mail;

public class SaveFormMailAddressService implements Service {
    private MailDao mailDao;
    private Mail mail;

    public SaveFormMailAddressService(MailDao mailDao, Mail mail){
        this.mailDao = mailDao;
        this.mail = mail;
    }

    @Override
    public Message executeAndGetResponse() {
        mailDao.save(mail);
        //More about lob
        return null;
    }
    
}
