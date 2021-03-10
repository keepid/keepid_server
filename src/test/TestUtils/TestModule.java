package TestUtils;

import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoImpl;
import Database.MongoConfig;
import Database.Organization.OrgDao;
import Database.Organization.OrgDaoImpl;
import Database.Report.ReportDao;
import Database.Report.ReportDaoImpl;
import Database.Token.TokenDao;
import Database.Token.TokenDaoImpl;
import Database.User.UserDao;
import Database.User.UserDaoImpl;
import com.google.inject.AbstractModule;

public class TestModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(OrgDao.class).to(OrgDaoImpl.class);
    bind(UserDao.class).to(UserDaoImpl.class);
    bind(TokenDao.class).to(TokenDaoImpl.class);
    bind(ActivityDao.class).to(ActivityDaoImpl.class);
    bind(ReportDao.class).to(ReportDaoImpl.class);
    bind(MongoConfig.class).to(MongoTestConfig.class);
  }
}
