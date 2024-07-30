package PDF;

import Config.Message;
import Database.File.FileDao;
import Database.User.UserDao;
import File.FileType;
import PDF.Services.DownloadAndReUploadPdfService;
import Security.EncryptionController;
import com.mongodb.client.MongoDatabase;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class V2BackfillController {
  private EncryptionController encryptionController;
  private MongoDatabase db;
  private FileDao fileDao;
  private UserDao userDao;

  public V2BackfillController(MongoDatabase db, FileDao fileDao, UserDao userDao) {
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
        //        String fileId = "668c7c3ee603c8759aa5da4a";
        String username = "SAMPLE-CLIENT";
        //        String orgName = userDao.get(username).get().getOrganization();
        DownloadAndReUploadPdfService downloadAndReUploadPdfService =
            new DownloadAndReUploadPdfService(
                encryptionController,
                db,
                fileDao,
                userDao,
                username,
                PDFType.IDENTIFICATION_DOCUMENT,
                FileType.IDENTIFICATION_PDF);
        Message response = downloadAndReUploadPdfService.executeAndGetResponse();
        ctx.result(response.toJSON().toString());
      };
}
