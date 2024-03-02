package Database.OptionalUserInformation;

import Config.DeploymentLevel;
import OptionalUserInformation.OptionalUserInformation;
import org.bson.types.ObjectId;

import java.util.*;

public class OptionalUserInformationDaoTestImpl implements OptionalUserInformationDao{

    Map<String, OptionalUserInformation> optionalUserInformationMap;

    public OptionalUserInformationDaoTestImpl(DeploymentLevel deploymentLevel) {
        if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
            throw new IllegalStateException(
                    "Should not run in memory test database in production or staging");
        }
        optionalUserInformationMap = new LinkedHashMap<>();
    }

    @Override
    public Optional<OptionalUserInformation> get(String username) {
        return Optional.ofNullable(optionalUserInformationMap.get(username));
    }

    @Override
    public Optional<OptionalUserInformation> get(ObjectId id) {
        for (OptionalUserInformation optionalUserInformation : optionalUserInformationMap.values()) {
            if (optionalUserInformation.getId().equals(id)) {
                return Optional.of(optionalUserInformation);
            }
        }
        return Optional.empty();
    }

    @Override
    public void delete(String username){
        optionalUserInformationMap.remove(username);
    }

    @Override
    public void update(OptionalUserInformation optionalUserInformation){
        optionalUserInformationMap.put(optionalUserInformation.getUsername(), optionalUserInformation);
    }

    @Override
    public List<OptionalUserInformation> getAll(){
        Collection<OptionalUserInformation> collection = optionalUserInformationMap.values();
        return new ArrayList<>(collection);
    }

    @Override
    public int size(){
        return optionalUserInformationMap.size();
    }

    @Override
    public void save(OptionalUserInformation optionalUserInformation){
        optionalUserInformationMap.put(optionalUserInformation.getUsername(), optionalUserInformation);
    }

    @Override
    public void delete(OptionalUserInformation optionalUserInformation){
        optionalUserInformationMap.remove(optionalUserInformation.getUsername());
    }

    @Override
    public void clear(){
        optionalUserInformationMap.clear();
    }
}
