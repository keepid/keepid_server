package Mail;

import Config.Message;
import Database.File.FileDao;
import Database.User.UserDao;
import Mail.Services.DownloadAndReUploadPdfService;
import Security.EncryptionController;
import com.mongodb.client.MongoDatabase;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileBackfillController {
  private EncryptionController encryptionController;
  private MongoDatabase db;
  private FileDao fileDao;
  private UserDao userDao;

  public FileBackfillController(MongoDatabase db, FileDao fileDao, UserDao userDao) {
    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {
      log.error("generating encryption controller failed");
    }
    this.db = db;
    this.fileDao = fileDao;
    this.userDao = userDao;
  }

  public Handler backfillSingleFile =
      ctx -> {
        String fileId = "668c7c3ee603c8759aa5da4a";
        String username = "SAMPLE-CLIENT";
        String orgName = userDao.get(username).get().getOrganization();
        DownloadAndReUploadPdfService downloadAndReUploadPdfService =
            new DownloadAndReUploadPdfService(
                encryptionController, db, fileDao, fileId, username, orgName);
        Message response = downloadAndReUploadPdfService.executeAndGetResponse();
        ctx.result(response.toJSON().toString());
      };
}
