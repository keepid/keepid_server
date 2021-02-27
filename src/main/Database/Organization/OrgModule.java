package Database.Organization;

import com.google.inject.AbstractModule;

public class OrgModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(OrgDao.class).to(OrgDaoImpl.class);
  }
}
