import Config.AppConfigV2;
import Config.DeploymentLevel;
import Database.Organization.OrgModule;
import Database.User.UserModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ProductionApp {
  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new UserModule(), new OrgModule());
    AppConfigV2 appConfigV2 = injector.getInstance(AppConfigV2.class);
    appConfigV2.appFactory(DeploymentLevel.TEST);
  }
}
