package Database.Activity;

import Config.DeploymentLevel;

public class ActivityDaoFactory {
  public static ActivityDao create(DeploymentLevel deploymentLevel) {
    if (deploymentLevel == DeploymentLevel.IN_MEMORY) {
      return new ActivityDaoTestImpl(deploymentLevel);
    }
    return new ActivityDaoImpl(deploymentLevel);
  }
}
