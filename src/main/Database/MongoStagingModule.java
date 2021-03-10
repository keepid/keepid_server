package Database;

import Config.MongoStagingConfig;
import com.google.inject.AbstractModule;

public class MongoStagingModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(MongoConfig.class).to(MongoStagingConfig.class);
  }
}
