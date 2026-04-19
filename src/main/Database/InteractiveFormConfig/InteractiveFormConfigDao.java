package Database.InteractiveFormConfig;

import Database.Dao;
import Form.InteractiveFormConfig;
import java.util.Optional;
import org.bson.types.ObjectId;

public interface InteractiveFormConfigDao extends Dao<InteractiveFormConfig> {

  Optional<InteractiveFormConfig> getByFileId(ObjectId fileId);

  void upsertByFileId(InteractiveFormConfig config);

  void deleteByFileId(ObjectId fileId);
}
