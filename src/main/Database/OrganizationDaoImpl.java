package Database;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Organization.Organization;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class OrganizationDaoImpl implements OrganizationDao {
  private MongoCollection<Organization> OrganizationCollection;

  public OrganizationDaoImpl(DeploymentLevel deploymentLevel) {
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    if (db == null) {
      throw new IllegalStateException("DB cannot be null");
    }
    OrganizationCollection = db.getCollection("Organization", Organization.class);
  }

  @Override
  public Optional<Organization> get(String orgName) {
    return Optional.ofNullable(OrganizationCollection.find(eq("orgName", orgName)).first());
  }

  @Override
  public Optional<Organization> get(ObjectId id) {
    return Optional.ofNullable(OrganizationCollection.find(eq("_id", id)).first());
  }

  @Override
  public List<Organization> getAll() {
    return OrganizationCollection.find().into(new ArrayList<>());
  }

  @Override
  public int size() {
    return (int) OrganizationCollection.countDocuments();
  }

  @Override
  public void delete(Organization Organization) {
    OrganizationCollection.deleteOne(eq("orgName", Organization.getOrgName()));
  }

  @Override
  public void clear() {
    OrganizationCollection.drop();
  }

  @Override
  public void delete(String orgName) {
    OrganizationCollection.deleteOne(eq("orgName", orgName));
  }

  @Override
  public void update(Organization organization, String[] params) {
    // implement later
  }

  @Override
  public void save(Organization organization) {
    OrganizationCollection.insertOne(organization);
  }
}
