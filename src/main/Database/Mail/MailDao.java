package Database.Mail;

import Database.Dao;
import Mail.Mail;
import org.bson.types.ObjectId;

public interface MailDao extends Dao<Mail> {
  void delete(String mailId);

  void delete(ObjectId objectId);
}
