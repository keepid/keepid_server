package PDFTest.PDFV2Test;

import Database.File.FileDao;
import Database.Form.FormDao;
import Database.User.UserDao;
import PDF.PdfControllerV2.UserParams;
import Security.EncryptionController;
import com.mongodb.client.MongoDatabase;

public class UploadAnnotatedPDFServiceUnitTests {
  private FileDao fileDao;
  private FormDao formDao;
  private UserDao userDao;
  private MongoDatabase db;
  private EncryptionController encryptionController;
  private UserParams clientOneUserParams;
}
