package Database.Notification;

import Config.DeploymentLevel;
import Notification.Notification;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;

public class NotificationDaoTestImpl implements NotificationDao {
    Map<ObjectId, Notification> notificationMap;

    public NotificationDaoTestImpl(DeploymentLevel deploymentLevel) {
        if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
            throw new IllegalStateException(
                    "Should not run in memory test database in production or staging");
        }
        notificationMap = new LinkedHashMap<>();
    }

    @Override
    public List<Notification> getByClientUsername(String clientUsername) {
        return notificationMap.values().stream()
                .filter(n -> n.getClientUsername().equals(clientUsername))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Notification> get(ObjectId id) {
        return Optional.ofNullable(notificationMap.get(id));
    }

    @Override
    public List<Notification> getAll() {
        return notificationMap.values().stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    @Override
    public int size() {
        return notificationMap.size();
    }

    @Override
    public void save(Notification notification) {
        if (notification.getId() == null) {
            notification.setId(new ObjectId());
        }
        notificationMap.put(notification.getId(), notification);
    }

    @Override
    public void update(Notification notification) {
        notificationMap.put(notification.getId(), notification);
    }

    @Override
    public void delete(Notification notification) {
        notificationMap.remove(notification.getId());
    }

    @Override
    public void clear() {
        notificationMap.clear();
    }
}
