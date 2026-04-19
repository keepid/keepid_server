package Database.Packet;

import Config.DeploymentLevel;

public class PacketDaoFactory {
  public static PacketDao create(DeploymentLevel deploymentLevel) {
    if (deploymentLevel == DeploymentLevel.IN_MEMORY) {
      return new PacketDaoTestImpl(deploymentLevel);
    }
    return new PacketDaoImpl(deploymentLevel);
  }
}
