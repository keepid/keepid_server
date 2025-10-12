package Database.Form;

import Database.Dao;
import Form.Form;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;

public interface FormDao extends Dao<Form> {
  Optional<Form> get(ObjectId id);

  Optional<Form> getByFileId(ObjectId fileId);

  List<Form> get(String username);

  List<Form> getWeeklyApplications();

  void save(Form form);

  void delete(ObjectId id);
}
