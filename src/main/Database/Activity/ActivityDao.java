package Database.Activity;

import Activity.Activity;
import Database.Dao;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityDao extends Dao<Activity> {

  List<Activity> getAllFromUser(String username);

  List<Activity> getAllFromUserBetweenInclusive(
      String username, LocalDateTime startTime, LocalDateTime endTime);
}
