package Database.File;

import Config.DeploymentLevel;

public class FileDaoFactory {
  public static FileDao create(DeploymentLevel deploymentLevel) {
    if (deploymentLevel == DeploymentLevel.IN_MEMORY) {
      return new FileDaoTestImpl(deploymentLevel);
    }
    return new FileDaoImpl(deploymentLevel);
  }
}
