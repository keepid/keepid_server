package Database.Mail;

import Database.Dao;
import Mail.Mail;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;

public interface MailDao extends Dao<Mail> {
  void delete(String mailId);

  void delete(ObjectId objectId);

  List<Mail> getByFileId(ObjectId fileId);

  List<Mail> getByOrganization(String organizationName);

  List<Mail> getByOrganization(String organizationName, Date from, Date to);
}
