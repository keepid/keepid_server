package Database.Notification;

import Database.Dao;
import Notification.Notification;
import java.util.List;

public interface NotificationDao extends Dao<Notification> {

    List<Notification> getByClientUsername(String clientUsername);
}
