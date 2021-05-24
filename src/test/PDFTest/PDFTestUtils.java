package PDFTest;

import Security.EncryptionUtils;
import TestUtils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.LinkedList;

import static org.assertj.core.api.Assertions.assertThat;

public class PDFTestUtils {
  private static EncryptionUtils encryptionUtils;
  public static String username = "adminBSM";

  public static String currentPDFFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "PDFTest";

  public static String resourcesFolderPath =
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

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  public static String uploadFileAndGetFileId(File file, String pdfType)
      throws IOException, GeneralSecurityException {
    // upload file
    EncryptionUtils encryptionUtils = TestUtils.getEncryptionUtils();
    InputStream fileStream = FileUtils.openInputStream(file);

    File tmp = File.createTempFile("test1", "tmp");
    FileUtils.copyInputStreamToFile(encryptionUtils.encryptFile(fileStream, username), tmp);
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
            .header("Content-Disposition", "attachment")
            .field("pdfType", pdfType)
            .field("file", file)
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");

    // get file id
    JSONObject body = new JSONObject();
    body.put("pdfType", "FORM");
    body.put("annotated", false);
    HttpResponse<String> getForm =
        Unirest.post(TestUtils.getServerUrl() + "/get-documents").body(body.toString()).asString();
    JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());

    String fileId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
    return fileId;
  }

  public static void deletePDF(String id, String pdfType) {
    JSONObject body = new JSONObject();
    body.put("pdfType", pdfType);
    body.put("fileId", id);
    HttpResponse<String> deleteResponse =
        Unirest.post(TestUtils.getServerUrl() + "/delete-document")
            .body(body.toString())
            .asString();
    JSONObject deleteResponseJSON = TestUtils.responseStringToJSON(deleteResponse.getBody());
    assertThat(deleteResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  public static void checkForFields(
      JSONObject applicationsQuestionsResponseJSON, LinkedList<String[][]> fieldsToCheck) {
    for (String[][] arr : fieldsToCheck) {
      String fieldType = arr[0][0];
      String fieldName = arr[1][0];
      JSONArray fieldValueOptions = new JSONArray(arr[2]);
      boolean found_type = false;
      boolean found_name = false;
      for (int i = 0; i < applicationsQuestionsResponseJSON.getJSONArray("fields").length(); i++) {
        String curr_name =
            applicationsQuestionsResponseJSON
                .getJSONArray("fields")
                .getJSONObject(i)
                .get("fieldName")
                .toString();
        String curr_type =
            applicationsQuestionsResponseJSON
                .getJSONArray("fields")
                .getJSONObject(i)
                .get("fieldType")
                .toString();
        if (curr_name.equals(fieldName) && curr_type.equals(fieldType)) {
          found_name = true;
          found_type = true;
          JSONArray curr_options =
              applicationsQuestionsResponseJSON
                  .getJSONArray("fields")
                  .getJSONObject(i)
                  .getJSONArray("fieldValueOptions");
          assertThat(curr_options.toList()).isEqualTo(fieldValueOptions.toList());
        }
      }
      assertThat(found_name).isTrue();
      assertThat(found_type).isTrue();
    }
  }

  public static void clearAllDocuments() {
    String[] pdfTypes = {"FORM", "FORM", "APPLICATION"};
    boolean[] annotated = {false, true, false};
    for (int j = 0; j < pdfTypes.length; j++) {
      JSONObject body = new JSONObject();
      body.put("pdfType", pdfTypes[j]);
      body.put("annotated", annotated[j]);
      HttpResponse<String> getAllDocuments =
          Unirest.post(TestUtils.getServerUrl() + "/get-documents")
              .body(body.toString())
              .asString();
      JSONObject getAllDocumentsJSON = TestUtils.responseStringToJSON(getAllDocuments.getBody());
      assertThat(getAllDocumentsJSON.getString("status")).isEqualTo("SUCCESS");
      JSONArray arr = getAllDocumentsJSON.getJSONArray("documents");
      for (int i = 0; i < arr.length(); i++) {
        String fileId = arr.getJSONObject(i).getString("id");
        deletePDF(fileId, pdfTypes[j]);
      }
    }
  }
}
