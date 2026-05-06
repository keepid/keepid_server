package Database.PhoneUpload;

import Database.Dao;
import PhoneUpload.PhoneUploadSession;
import java.util.Optional;

public interface PhoneUploadSessionDao extends Dao<PhoneUploadSession> {
  Optional<PhoneUploadSession> getByTokenHash(String tokenHash);
}
