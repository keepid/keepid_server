import Config.AppConfigV2;
import Config.DeploymentLevel;
import Database.Activity.ActivityModule;
import Database.Organization.OrgModule;
import Database.Report.ReportModule;
import Database.Token.TokenModule;
import Database.User.UserModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class App {
  public static void main(String[] args) {
    Injector injector =
        Guice.createInjector(
            new UserModule(),
            new OrgModule(),
            new TokenModule(),
            new ReportModule(),
            new ActivityModule());
    AppConfigV2 appConfigV2 = injector.getInstance(AppConfigV2.class);
    appConfigV2.appFactory(DeploymentLevel.STAGING);
  }
}
