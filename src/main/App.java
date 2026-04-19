import Config.AppConfig;
import Config.DeploymentLevel;
import java.util.Locale;
import java.util.Optional;

public class App {
  private static DeploymentLevel resolveDeploymentLevel() {
    String configuredLevel = Optional.ofNullable(System.getenv("DEPLOYMENT_LEVEL")).orElse("").trim();
    if (!configuredLevel.isEmpty()) {
      return DeploymentLevel.valueOf(configuredLevel.toUpperCase(Locale.ROOT));
    }

    // Heroku dynos always expose DYNO; defaulting to PRODUCTION there prevents accidental staging boots.
    if (System.getenv("DYNO") != null) {
      return DeploymentLevel.PRODUCTION;
    }

    return DeploymentLevel.STAGING;
  }

  public static void main(String[] args) {
    AppConfig.appFactory(resolveDeploymentLevel());
  }
}
