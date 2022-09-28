package PDFTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.TestUtils;
import User.User;
import User.UserType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static PDFTest.PDFTestUtils.*;
import static TestUtils.EntityFactory.createUser;
import static TestUtils.TestUtils.assertPDFEquals;
import static org.assertj.core.api.Assertions.assertThat;

public class ImageToPdfServiceIntegrationTests {
    private UserDao userDao;

    @BeforeClass
    public static void setUp() {
        TestUtils.startServer();
    }

    @Before
    public void initialize() {
        this.userDao = UserDaoFactory.create(DeploymentLevel.TEST);
    }

    @After
    public void reset() {
        this.userDao.clear();
        TestUtils.logout();
    }

    @AfterClass
    public static void tearDown() {
        TestUtils.tearDownTestDB();
    }

    @Test
    public void uploadPNGImageToPDFTest() throws IOException {
        File expectedOutputFile = new File(resourcesFolderPath + File.separator + "1_converted.pdf");
        InputStream expectedOutputFileStream = FileUtils.openInputStream(expectedOutputFile);

        User user =
                createUser()
                        .withUserType(UserType.Client)
                        .withUsername(username)
                        .withPasswordToHash(password)
                        .buildAndPersist(userDao);
        TestUtils.login(username, password);

        File file = new File(resourcesFolderPath + File.separator + "1.png");
        HttpResponse<String> uploadResponse =
                Unirest.post(TestUtils.getServerUrl() + "/upload")
                        .field("pdfType", "IDENTIFICATION_DOCUMENT")
                        .header("Content-Disposition", "attachment")
                        .field("file", file, "image/jpeg")
                        .asString();

        JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
        assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");

        String fileId = getPDFFileId();
        File downloadedPDF = downloadPDF(fileId, "IDENTIFICATION_DOCUMENT");
        InputStream downloadedPDFInputStream = FileUtils.openInputStream(downloadedPDF);
        assertPDFEquals(expectedOutputFileStream, downloadedPDFInputStream);

        TestUtils.logout();
    }

    @Test
    public void uploadImageJPEGToPDFTest() throws IOException {
        File expectedOutputFile = new File(resourcesFolderPath + File.separator + "veteran-id-card-vic_converted.pdf");
        InputStream expectedOutputFileStream = FileUtils.openInputStream(expectedOutputFile);

        User user =
                createUser()
                        .withUserType(UserType.Client)
                        .withUsername(username)
                        .withPasswordToHash(password)
                        .buildAndPersist(userDao);
        TestUtils.login(username, password);

        File file = new File(resourcesFolderPath + File.separator + "veteran-id-card-vic.jpg");
        HttpResponse<String> uploadResponse =
                Unirest.post(TestUtils.getServerUrl() + "/upload")
                        .field("pdfType", "IDENTIFICATION_DOCUMENT")
                        .header("Content-Disposition", "attachment")
                        .field("file", file, "image/jpeg")
                        .asString();

        JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
        assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");

        String fileId = getPDFFileId();
        File downloadedPDF = downloadPDF(fileId, "IDENTIFICATION_DOCUMENT");
        InputStream downloadedPDFInputStream = FileUtils.openInputStream(downloadedPDF);
        assertPDFEquals(expectedOutputFileStream, downloadedPDFInputStream);

        TestUtils.logout();
    }

    @Test
    public void uploadInvalidImageToPDFTest() {
        User user =
                createUser()
                        .withUserType(UserType.Client)
                        .withUsername(username)
                        .withPasswordToHash(password)
                        .buildAndPersist(userDao);
        TestUtils.login(username, password);

        File file = new File(resourcesFolderPath + File.separator + "job_description.docx");
        HttpResponse<String> uploadResponse =
                Unirest.post(TestUtils.getServerUrl() + "/upload")
                        .field("pdfType", "IDENTIFICATION_DOCUMENT")
                        .header("Content-Disposition", "attachment")
                        .field("file", file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                        .asString();

        JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
        assertThat(uploadResponseJSON.getString("status")).isEqualTo("INVALID_PDF");
        TestUtils.logout();
    }
}
