import Config.AppConfigV2;
import Config.DeploymentLevel;
import Database.Token.TokenModule;
import Organization.OrgModule;
import User.UserModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class App {
  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new UserModule(), new OrgModule(), new TokenModule());
    AppConfigV2 appConfigV2 = injector.getInstance(AppConfigV2.class);
    appConfigV2.appFactory(DeploymentLevel.TEST);
  }
}
