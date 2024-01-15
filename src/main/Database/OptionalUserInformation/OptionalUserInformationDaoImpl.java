package Database.OptionalUserInformation;

import Config.DeploymentLevel;
import Config.MongoConfig;
import UserV2.OptionalUserInformation;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class OptionalUserInformationDaoImpl implements OptionalUserInformationDao{

    private final MongoCollection<OptionalUserInformation> optUserInfoCollection;

    public OptionalUserInformationDaoImpl(DeploymentLevel deploymentLevel){
        MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
        if (db == null) {
            throw new IllegalStateException("DB cannot be null");
        }
        optUserInfoCollection = db.getCollection("optionalUserInformation", OptionalUserInformation.class);
    }

    @Override
    public Optional<OptionalUserInformation> get(ObjectId id){
        return Optional.ofNullable(optUserInfoCollection.find(eq("_id", id)).first());
    }

    @Override
    public Optional<OptionalUserInformation> get(String username){
        return Optional.ofNullable(optUserInfoCollection.find(eq("username", username)).first());
    }
    
    @Override
    public void delete(String username) {
        optUserInfoCollection.deleteOne(eq("username", username));
    }

    @Override
    public void update(OptionalUserInformation optionalUserInformation) {
        optUserInfoCollection.replaceOne(eq("username", optionalUserInformation.getUsername()), optionalUserInformation);
    }

    @Override
    public List<OptionalUserInformation> getAll(){
        return optUserInfoCollection.find().into(new ArrayList<>());
    }

    @Override
    public int size() {
        return (int)optUserInfoCollection.countDocuments();
    }

    @Override
    public void save(OptionalUserInformation optionalUserInformation) {
        optUserInfoCollection.insertOne(optionalUserInformation);
    }

    @Override
    public void delete(OptionalUserInformation optionalUserInformation) {
        optUserInfoCollection.deleteOne(eq("username", optionalUserInformation.getUsername()));
    }

    @Override
    public void clear() {
        optUserInfoCollection.drop();
    }
}
