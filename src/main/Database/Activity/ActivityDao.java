package Database.Activity;

import Activity.Activity;
import Database.Dao;

import java.util.List;

public interface ActivityDao extends Dao<Activity> {

  List<Activity> getAllFromUser(String username);

  void deleteAllFromOrg(String orgName);
}
