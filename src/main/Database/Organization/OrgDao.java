package Database.Organization;

import Database.Dao;
import Organization.Organization;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;

public interface OrgDao extends Dao<Organization> {

  Optional<Organization> get(String orgName);

  List<Organization> getAll();

  void delete(String orgName);

  void delete(ObjectId objectId);
}
