package Database.Mail;

import static com.mongodb.client.model.Filters.eq;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Mail.Mail;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;

public class MailDaoImpl implements MailDao {
  private static final String MAIL_ID_KEY = "_id";
  private MongoCollection<Mail> mailCollection;

  public MailDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    mailCollection = db.getCollection("mail", Mail.class);
  }

  @Override
  public Optional<Mail> get(ObjectId id) {
    return Optional.ofNullable(mailCollection.find(eq(MAIL_ID_KEY, id)).first());
  }

  @Override
  public List<Mail> getAll() {
    return mailCollection.find().into(new ArrayList<>());
  }

  @Override
  public int size() {
    return (int) mailCollection.countDocuments();
  }

  @Override
  public void clear() {
    mailCollection.drop();
  }

  @Override
  public void delete(Mail mail) {
    mailCollection.deleteOne(eq(MAIL_ID_KEY, mail.getId()));
  }

  @Override
  public void delete(String mailId) {
    mailCollection.deleteOne(eq(MAIL_ID_KEY, new ObjectId(mailId)));
  }

  public void delete(ObjectId mail) {
    mailCollection.deleteOne(eq(MAIL_ID_KEY, mail));
  }

  @Override
  public void update(Mail mail) {
    mailCollection.replaceOne(eq(MAIL_ID_KEY, mail.getId()), mail);
  }

  @Override
  public void save(Mail mail) {
    mailCollection.insertOne(mail);
  }
}
