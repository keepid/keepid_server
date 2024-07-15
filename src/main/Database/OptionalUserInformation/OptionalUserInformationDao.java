package Database.OptionalUserInformation;

import Database.Dao;
import OptionalUserInformation.OptionalUserInformation;

import java.util.Optional;

public interface OptionalUserInformationDao extends Dao<OptionalUserInformation> {

    Optional<OptionalUserInformation> get(String username);
    
    void delete(String username);
    
    void update(OptionalUserInformation optionalUserInformation);

}
