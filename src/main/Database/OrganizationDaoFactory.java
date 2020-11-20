package Database;

import Config.DeploymentLevel;

public class OrganizationDaoFactory {
  public static OrganizationDao create(DeploymentLevel deploymentLevel) {
    if (deploymentLevel == DeploymentLevel.IN_MEMORY) {
      return new OrganizationDaoTestImpl(deploymentLevel);
    }
    return new OrganizationDaoImpl(deploymentLevel);
  }
}
