package Organization;

import Database.Organization.OrgDao;
import Database.Organization.OrgDaoImpl;
import com.google.inject.AbstractModule;

public class OrgModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(OrgDao.class).to(OrgDaoImpl.class);
  }
}
