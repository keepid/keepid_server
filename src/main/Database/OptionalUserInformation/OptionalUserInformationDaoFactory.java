package Database.OptionalUserInformation;

import Config.DeploymentLevel;

public class OptionalUserInformationDaoFactory {
    public static OptionalUserInformationDao create(DeploymentLevel deploymentLevel) {
        if (deploymentLevel == DeploymentLevel.IN_MEMORY) {
            return new OptionalUserInformationDaoTestImpl(deploymentLevel) {};
        }
        return new OptionalUserInformationDaoImpl(deploymentLevel);
    }


}
