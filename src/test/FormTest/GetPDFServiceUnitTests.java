package FormTest;

import Config.DeploymentLevel;
import Config.Message;
import Config.MongoConfig;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Form.Form;
import Form.Services.GetPDFService;
import PDF.PDFType;
import PDF.Services.GetFilesInformationPDFService;
import PDF.Services.UploadPDFService;
import Security.EncryptionController;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;

public class GetPDFServiceUnitTests {
  private static String resourcesFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources";

  MongoDatabase db;
  EncryptionController encryptionController;

  String username = "testUsername";
  String orgName = "testOrgName";
  UserType userType = UserType.Admin;
  PDFType pdfType = PDFType.FORM;
  String filename = "testFilename";
  String title = "testTitle";
  String fileContentType = "application/pdf";

  FormDao formDao;
  ObjectId formId;
  Form form;

  @Before
  public void setup() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
    db = MongoConfig.getDatabase(DeploymentLevel.TEST);
    try {
      this.encryptionController = new EncryptionController(db);
    } catch (Exception e) {
      e.printStackTrace();
    }
    formDao = FormDaoFactory.create(DeploymentLevel.TEST);
    form = EntityFactory.createForm().withUsername("testUsername").buildAndPersist(formDao);
    formId = form.getId();
  }

  private void customUploadPdf() {
    File file =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(file);
    } catch (Exception e) {
      e.printStackTrace();
    }
    UploadPDFService uploadPDFService =
        new UploadPDFService(
            db,
            username,
            orgName,
            userType,
            pdfType,
            filename,
            title,
            fileContentType,
            inputStream,
            encryptionController);
    Message response = uploadPDFService.executeAndGetResponse();
  }

  @After
  public void reset() {}

  @Test
  public void Test() {
    customUploadPdf();
    PDFType pdfType = PDFType.FORM;
    Bson filter = null;
    GetFilesInformationPDFService infoService =
        new GetFilesInformationPDFService(db, username, orgName, userType, pdfType, false);
    infoService.executeAndGetResponse();
    JSONArray array = infoService.getFiles();
    JSONObject firstElem = (JSONObject) (array.get(0));
    ObjectId id = new ObjectId(firstElem.get("id").toString());
    form.setPdfId(id);
    GetPDFService getPDFService =
        new GetPDFService(db, form, orgName, UserType.Admin, pdfType, encryptionController);
    Message getPdfResponse = getPDFService.executeAndGetResponse();
    System.out.println(getPdfResponse.toResponseString());
  }
}
