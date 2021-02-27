package Database.User;

import com.google.inject.AbstractModule;

public class UserModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(UserDao.class).to(UserDaoImpl.class);
  }
}
