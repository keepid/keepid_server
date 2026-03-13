package Database.InteractiveFormConfig;

import Config.DeploymentLevel;

public class InteractiveFormConfigDaoFactory {
  public static InteractiveFormConfigDao create(DeploymentLevel deploymentLevel) {
    if (deploymentLevel == DeploymentLevel.IN_MEMORY) {
      return new InteractiveFormConfigDaoTestImpl(deploymentLevel);
    }
    return new InteractiveFormConfigDaoImpl(deploymentLevel);
  }
}
