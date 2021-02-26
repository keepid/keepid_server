package Database.Activity;

import Activity.Activity;
import Organization.Organization;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor
public class ActivityDaoTestImpl implements ActivityDao {

  Map<String, Organization> orgMap;

  @Override
  public Optional<Activity> get(ObjectId id) {
    return null;
  }

  @Override
  public List<Activity> getAll() {
    return null;
  }

  @Override
  public List<Activity> getAllFromUser(String username) {
    return null;
  }

  @Override
  public int size() {
    return 1;
  }

  @Override
  public void save(Activity activity) {}

  @Override
  public void update(Activity activity) {}

  @Override
  public void delete(Activity activity) {}

  @Override
  public void deleteAllFromOrg(String orgName) {}

  @Override
  public void clear() {}
}
