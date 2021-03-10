package Database;

import com.mongodb.client.MongoDatabase;

public interface MongoConfig {
  MongoDatabase getDatabase();
}
