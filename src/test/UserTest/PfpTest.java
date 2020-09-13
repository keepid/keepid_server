package UserTest;

import TestUtils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class PfpTest {

  private static String currentPfpFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources";

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
  }

  //  @AfterClass
  //  public static void tearDown() {
  //    TestUtils.tearDownTestDB();
  //  }

  @Test
  public void uploadValidPDFTestExists() {
    TestUtils.login("cathyAsClient", "cathyAsClient");
    uploadTestPfp();
    getPfp();
    TestUtils.logout();
  }

  public static void uploadTestPfp() {
    File examplePDF = new File(currentPfpFolderPath + File.separator + "pfp.JPG");
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload-pfp")
            .header("Content-Disposition", "attachment")
            .field("file", examplePDF)
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  public static HttpResponse getPfp() {

    HttpResponse get = Unirest.post(TestUtils.getServerUrl() + "/load-pfp").asBytes();
    return get;
  }
}
