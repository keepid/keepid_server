package Database.Mail;

import Mail.Mail;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;

public class MailDaoImpl implements MailDao {

  @Override
  public Optional<Mail> get(ObjectId id) {
    return Optional.empty();
  }

  @Override
  public List<Mail> getAll() {
    return null;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public void clear() {}

  @Override
  public void delete(Mail file) {}

  @Override
  public void update(Mail file) {}

  @Override
  public void save(Mail file) {}
}
