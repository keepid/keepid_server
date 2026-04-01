package Database.Mail;

import Config.DeploymentLevel;
import Mail.Mail;
import java.util.*;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;

public class MailDaoTestImpl implements MailDao {
  Map<String, Mail> mailMap;

  public MailDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in memory test database in production or staging");
    }
    mailMap = new LinkedHashMap<>();
  }

  @Override
  public Optional<Mail> get(ObjectId mailId) {
    return Optional.ofNullable(mailMap.get(mailId.toString()));
  }

  @Override
  public List<Mail> getAll() {
    return new ArrayList<>(mailMap.values());
  }

  @Override
  public int size() {
    return mailMap.size();
  }

  @Override
  public void clear() {
    mailMap.clear();
  }

  @Override
  public void delete(Mail mail) {
    mailMap.remove(mail.getId().toString());
  }

  @Override
  public void delete(String mailId) {
    mailMap.remove(mailId);
  }

  @Override
  public void delete(ObjectId objectId) {
    mailMap.remove(objectId.toString());
  }

  @Override
  public void update(Mail mail) {
    if (mailMap.containsKey(mail.getId().toString())) {
      mailMap.put(mail.getId().toString(), mail);
    }
  }

  @Override
  public void save(Mail mail) {
    mailMap.put(mail.getId().toString(), mail);
  }

  @Override
  public List<Mail> getByFileId(ObjectId fileId) {
    return mailMap.values().stream()
        .filter(m -> m.getFileId() != null && m.getFileId().equals(fileId))
        .collect(Collectors.toList());
  }

  @Override
  public List<Mail> getByOrganization(String organizationName) {
    return mailMap.values().stream()
        .filter(m -> organizationName.equals(m.getOrganizationName()))
        .collect(Collectors.toList());
  }

  @Override
  public List<Mail> getByOrganization(String organizationName, Date from, Date to) {
    return mailMap.values().stream()
        .filter(
            m ->
                organizationName.equals(m.getOrganizationName())
                    && m.getLobCreatedAt() != null
                    && !m.getLobCreatedAt().before(from)
                    && !m.getLobCreatedAt().after(to))
        .collect(Collectors.toList());
  }
}
