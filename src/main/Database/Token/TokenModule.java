package Database.Token;

import com.google.inject.AbstractModule;

public class TokenModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(TokenDao.class).to(TokenDaoImpl.class);
  }
}
