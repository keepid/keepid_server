package User;

import Database.User.UserDao;
import Database.User.UserDaoImpl;
import com.google.inject.AbstractModule;

public class UserModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(UserDao.class).to(UserDaoImpl.class);
  }
}
