package Database.PhoneUpload;

import Config.DeploymentLevel;

public class PhoneUploadSessionDaoFactory {
  public static PhoneUploadSessionDao create(DeploymentLevel deploymentLevel) {
    if (deploymentLevel == DeploymentLevel.IN_MEMORY) {
      return new PhoneUploadSessionDaoTestImpl(deploymentLevel);
    }
    return new PhoneUploadSessionDaoImpl(deploymentLevel);
  }
}
