package Database.ApplicationRegistry;

import Config.DeploymentLevel;

public class ApplicationRegistryDaoFactory {
  public static ApplicationRegistryDao create(DeploymentLevel deploymentLevel) {
    if (deploymentLevel == DeploymentLevel.IN_MEMORY) {
      return new ApplicationRegistryDaoTestImpl(deploymentLevel);
    }
    return new ApplicationRegistryDaoImpl(deploymentLevel);
  }
}
