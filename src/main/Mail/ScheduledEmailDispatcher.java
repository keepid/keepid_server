package Mail;

import Activity.Activity;
import Database.Activity.ActivityDao;

import java.time.LocalDateTime;
import java.util.List;

public class ScheduledEmailDispatcher {
    private final ActivityDao activityDao;

    public ScheduledEmailDispatcher(ActivityDao activityDao) {
        this.activityDao = activityDao;
    }

    public void dispatchDailyReminders() {
        System.out.println("Running scheduled reminder task: " + LocalDateTime.now());

        List<Activity> unnotified = activityDao.getUnnotifiedActivities();
        for (Activity activity : unnotified) {
            //
            EmailNotifier.handle(activity);

            activity.setNotified(true);
            activity.setNotifiedAt(LocalDateTime.now());
            activityDao.update(activity);
        }
    }
}

