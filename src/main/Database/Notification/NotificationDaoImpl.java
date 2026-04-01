package Database.Notification;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Notification.Notification;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;

public class NotificationDaoImpl implements NotificationDao {
    private final MongoCollection<Notification> notificationCollection;

    public NotificationDaoImpl(DeploymentLevel deploymentLevel) {
        MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
        if (db == null) {
            throw new IllegalStateException("DB cannot be null");
        }
        notificationCollection = db.getCollection("notification", Notification.class);
    }

    @Override
    public List<Notification> getByClientUsername(String clientUsername) {
        return notificationCollection
                .find(eq("clientUsername", clientUsername))
                .sort(descending("sentAt"))
                .into(new ArrayList<>());
    }

    @Override
    public Optional<Notification> get(ObjectId id) {
        return Optional.ofNullable(notificationCollection.find(eq("_id", id)).first());
    }

    @Override
    public List<Notification> getAll() {
        return notificationCollection.find().into(new ArrayList<>());
    }

    @Override
    public int size() {
        return (int) notificationCollection.countDocuments();
    }

    @Override
    public void save(Notification notification) {
        notificationCollection.insertOne(notification);
    }

    @Override
    public void update(Notification notification) {
        notificationCollection.replaceOne(eq("_id", notification.getId()), notification);
    }

    @Override
    public void delete(Notification notification) {
        notificationCollection.deleteOne(eq("_id", notification.getId()));
    }

    @Override
    public void clear() {
        notificationCollection.deleteMany(new Document());
    }
}
