package Mail;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledEmailDispatcher {

    @Scheduled(cron = "0 0 8 * * *") // Every day at 8 AM
    public void dispatchDailyReminders() {
        // You can query unfinished activities here and send emails
        System.out.println("Running scheduled email job...");

        // e.g. ActivityDao.getUnfinishedApplications().forEach(EmailNotifier::handle);
    }
    @Scheduled(fixedRate = 10000) // every 10 seconds
    public void testEmailPing() {
        System.out.println("Running scheduled task: " + System.currentTimeMillis());
    }
}

