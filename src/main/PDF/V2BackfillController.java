package PDF;

import Config.Message;
import Database.File.FileDao;
import Database.Organization.OrgDao;
import Database.User.UserDao;
import PDF.Services.DownloadAndReUploadPdfsService;
import Security.EncryptionController;
import com.mongodb.client.MongoDatabase;
import io.javalin.http.Handler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class V2BackfillController {
  private EncryptionController encryptionController;
  private MongoDatabase db;
  private FileDao fileDao;
  private UserDao userDao;
  private OrgDao orgDao;

  public V2BackfillController(MongoDatabase db, FileDao fileDao, UserDao userDao, OrgDao orgDao) {
    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {
      log.error("Generating encryption controller failed");
    }
    this.db = db;
    this.fileDao = fileDao;
    this.userDao = userDao;
    this.orgDao = orgDao;
  }

  public Handler backfillAllFiles =
      ctx -> {
        //        String fileId = "668c7c3ee603c8759aa5da4a";
        //        String username = "SAMPLE-CLIENT";
        //        String orgName = userDao.get(username).get().getOrganization();
        JSONObject req = new JSONObject(ctx.body());
        Boolean deleteParam = req.getBoolean("delete");
        Boolean filterParam = req.getBoolean("filter");
        Boolean downloadParam = req.getBoolean("download");
        String backfillType = req.getString("backfillType");
        String downloadStrings = req.getString("downloadStrings");
        DownloadAndReUploadPdfsService downloadAndReUploadPdfService =
            new DownloadAndReUploadPdfsService(encryptionController, db, fileDao, userDao, orgDao, deleteParam, filterParam, downloadParam, backfillType, downloadStrings);
        Message response = downloadAndReUploadPdfService.executeAndGetResponse();
        ctx.result(response.toResponseString());
      };
}
