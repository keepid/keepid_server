package User.Services;

import Database.Organization.OrgDao;
import Database.User.UserDao;
import Organization.Organization;
import User.User;
import User.UserType;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

@Slf4j
public final class OrganizationIdResolver {

  private OrganizationIdResolver() {}

  /**
   * Returns the user's organization ObjectId, backfilling {@link User#setOrganizationId} from the
   * legacy organization name when missing and persisting.
   */
  public static Optional<ObjectId> resolveAndPersistIfMissing(User user, OrgDao orgDao, UserDao userDao) {
    if (user.getUserType() == UserType.Developer) {
      return Optional.empty();
    }
    if (user.getOrganizationId() != null) {
      return Optional.of(user.getOrganizationId());
    }
    if (user.getOrganization() == null || user.getOrganization().isBlank()) {
      return Optional.empty();
    }
    Optional<Organization> org = orgDao.get(user.getOrganization());
    if (org.isEmpty()) {
      log.warn("No organization row for user {} organization string {}", user.getUsername(), user.getOrganization());
      return Optional.empty();
    }
    ObjectId id = org.get().getId();
    user.setOrganizationId(id);
    userDao.update(user);
    return Optional.of(id);
  }
}
