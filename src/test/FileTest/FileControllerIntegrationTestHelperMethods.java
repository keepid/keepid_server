package FileTest;

import TestUtils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class FileControllerIntegrationTestHelperMethods {
  private static String resourcesFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources";

  public static void uploadTestPDF() {
    File examplePDF =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");

    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload-file")
            .header("Content-Disposition", "attachment")
            .field("fileType", "APPLICATION_PDF")
            .field("file", examplePDF, "application/pdf")
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  public static void uploadOrgDocumentPDF() {
    File examplePDF =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");

    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload-file")
            .header("Content-Disposition", "attachment")
            .field("fileType", "ORG_DOCUMENT")
            .field("file", examplePDF, "application/pdf")
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  public static void uploadOrgDocumentPDF(String overrideFilename) throws IOException {
    Path source = Paths.get(resourcesFolderPath, "CIS_401_Final_Progress_Report.pdf");
    Path tempTarget = Paths.get(resourcesFolderPath, overrideFilename);
    Files.copy(source, tempTarget);
    File uploadFile = tempTarget.toFile();
    try {
      HttpResponse<String> uploadResponse =
          Unirest.post(TestUtils.getServerUrl() + "/upload-file")
              .header("Content-Disposition", "attachment")
              .field("fileType", "ORG_DOCUMENT")
              .field("file", uploadFile, "application/pdf")
              .asString();
      JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
      assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
    } finally {
      Files.deleteIfExists(tempTarget);
    }
  }
}
