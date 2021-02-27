package TestUtils;

import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoTestImpl;
import Database.Organization.OrgDao;
import Database.Organization.OrgDaoTestImpl;
import Database.Report.ReportDao;
import Database.Report.ReportDaoTestImpl;
import Database.Token.TokenDao;
import Database.Token.TokenDaoTestImpl;
import Database.User.UserDao;
import Database.User.UserDaoTestImpl;
import com.google.inject.AbstractModule;

public class TestModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(OrgDao.class).to(OrgDaoTestImpl.class);
    bind(UserDao.class).to(UserDaoTestImpl.class);
    bind(TokenDao.class).to(TokenDaoTestImpl.class);
    bind(ActivityDao.class).to(ActivityDaoTestImpl.class);
    bind(ReportDao.class).to(ReportDaoTestImpl.class);
  }
}
