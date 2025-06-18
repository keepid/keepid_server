package Database.Activity;

import Activity.Activity;
import Database.Dao;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityDao extends Dao<Activity> {

  List<Activity> getAllFromUser(String username);

  List<Activity> getAllFromUserBetweenInclusive(
      String username, LocalDateTime startTime, LocalDateTime endTime);
  // New: Find up to N unnotified activities (for email reminders)
  List<Activity> findUnnotified(int limit);

  // New: Update an activity (to mark as notified)
  void update(Activity activity);
  List<Activity> getUnnotifiedActivities();
}

