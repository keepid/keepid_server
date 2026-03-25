package Database.Notification;

import Config.DeploymentLevel;

public class NotificationDaoFactory {
    public static NotificationDao create(DeploymentLevel deploymentLevel) {
        if (deploymentLevel == DeploymentLevel.IN_MEMORY) {
            return new NotificationDaoTestImpl(deploymentLevel);
        }
        return new NotificationDaoImpl(deploymentLevel);
    }
}
