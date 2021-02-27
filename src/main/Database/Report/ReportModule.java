package Database.Report;

import com.google.inject.AbstractModule;

public class ReportModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ReportDao.class).to(ReportDaoImpl.class);
  }
}
