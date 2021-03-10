package Config;

import Database.MongoConfig;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.Objects;

public class MongoStagingConfig implements MongoConfig {
  public static final String MONGO_DB_STAGING = "staging-db";
  public static final String MONGO_URI = Objects.requireNonNull(System.getenv("MONGO_URI"));
  private final MongoClient mongoClient;

  public MongoStagingConfig() {
    this.mongoClient = startConnection();
  }

  private MongoClient startConnection() {
    ConnectionString connectionString = new ConnectionString(MONGO_URI);
    CodecRegistry pojoCodecRegistry =
        CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build());
    CodecRegistry codecRegistry =
        CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
    MongoClientSettings clientSettings =
        MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .codecRegistry(codecRegistry)
            .build();
    return MongoClients.create(clientSettings);
  }

  @Override
  public MongoDatabase getDatabase() {
    if (mongoClient == null) {
      throw new IllegalStateException("Please start a client before getting a database");
    }
    return mongoClient.getDatabase(MONGO_DB_STAGING);
  }
}
