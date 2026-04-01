package Mail;

import Config.DeploymentLevel;
import java.util.Objects;

public class MailSenderFactory {
  private MailSenderFactory() {}

  public static MailSender create(DeploymentLevel deploymentLevel) {
    switch (deploymentLevel) {
      case PRODUCTION:
        return new LobMailSender(
            Objects.requireNonNull(System.getenv("LOB_API_KEY_PROD"), "LOB_API_KEY_PROD not set"),
            Objects.requireNonNull(System.getenv("LOB_BANK_ACCOUNT_ID_PROD"), "LOB_BANK_ACCOUNT_ID_PROD not set"));
      case STAGING:
      case TEST:
        return new LobMailSender(
            Objects.requireNonNull(System.getenv("LOB_API_KEY_TEST"), "LOB_API_KEY_TEST not set"),
            Objects.requireNonNull(System.getenv("LOB_BANK_ACCOUNT_ID_TEST"), "LOB_BANK_ACCOUNT_ID_TEST not set"));
      case IN_MEMORY:
      default:
        return new NoOpMailSender();
    }
  }
}
