package Database.Mail;

import Config.DeploymentLevel;

public class MailDaoFactory {
  public static MailDao create(DeploymentLevel deploymentLevel) {
    if (deploymentLevel == DeploymentLevel.IN_MEMORY) {
      return new MailDaoTestImpl(deploymentLevel);
    }
    return new MailDaoImpl(deploymentLevel);
  }
}
