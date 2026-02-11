package Security;

import Config.DeploymentLevel;

public class EmailSenderFactory {
  private EmailSenderFactory() {}

  public static EmailSender forDeploymentLevel(DeploymentLevel deploymentLevel) {
    if (deploymentLevel == DeploymentLevel.TEST || deploymentLevel == DeploymentLevel.IN_MEMORY) {
      return new NoOpEmailSender();
    }
    return new SmtpEmailSender();
  }

  public static EmailSender smtp() {
    return new SmtpEmailSender();
  }
}
