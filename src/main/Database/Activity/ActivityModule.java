package Database.Activity;

import com.google.inject.AbstractModule;

public class ActivityModule extends AbstractModule {
  @Override
  public void configure() {
    bind(ActivityDao.class).to(ActivityDaoImpl.class);
  }
}
