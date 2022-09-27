package PDFTest;

import Security.EncryptionUtils;
import TestUtils.TestUtils;
import User.UserType;
import com.opencsv.CSVReader;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class PDFTestUtils {
  public static String username = "adminBSM";
  public static String password = "somepassword1";

  public static String resourcesFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources";

  // ------------------ GENERAL HELPER METHODS ------------------------ //

  public static String uploadFileAndGetFileId(File file, String pdfType)
      throws IOException, GeneralSecurityException {
    // upload file
    EncryptionUtils encryptionUtils = TestUtils.getEncryptionUtils();
    InputStream fileStream = FileUtils.openInputStream(file);

    // File tmp = File.createTempFile("test1", "tmp");
    // FileUtils.copyInputStreamToFile(encryptionUtils.encryptFile(fileStream, username), tmp);
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
            .header("Content-Disposition", "attachment")
            .field("pdfType", pdfType)
            .field("file", file, "application/pdf")
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");

    // get file id
    JSONObject body = new JSONObject();
    body.put("pdfType", "BLANK_FORM");
    body.put("annotated", false);
    HttpResponse<String> getForm =
        Unirest.post(TestUtils.getServerUrl() + "/get-documents").body(body.toString()).asString();
    JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());

    String fileId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
    return fileId;
  }

  public static String getPDFFileId() {
      JSONObject body = new JSONObject();
      body.put("pdfType", "IDENTIFICATION_DOCUMENT");
      HttpResponse<String> getForm =
              Unirest.post(TestUtils.getServerUrl() + "/get-documents")
                      .body(body.toString()).asString();
      JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());

      JSONArray documents = getFormJSON.getJSONArray("documents");

      int lastFileIndex = documents.length() -1;
      String fileId = documents.getJSONObject(lastFileIndex).getString("id");

      return fileId;
  }

  public static File downloadPDF(String id, String pdfType) {
    JSONObject body = new JSONObject();
    body.put("fileId", id);
    body.put("pdfType", pdfType);

    try {
      File tmpFile = File.createTempFile("downloaded_pdf", ".pdf");

      HttpResponse<byte[]> downloadFileResponse =
              Unirest.post(TestUtils.getServerUrl() + "/download")
                      .body(body.toString())
                      .asBytes();

      FileUtils.writeByteArrayToFile(tmpFile, downloadFileResponse.getBody());
      return tmpFile;
    } catch (IOException exception) {
      return null;
    }
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

  public static void clearAllDocumentsForUser(String username, String password) {
    TestUtils.login(username, password);
    String[] pdfTypes = {"BLANK_FORM", "IDENTIFICATION_DOCUMENT", "COMPLETED_APPLICATION"};
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

  // ------------------ SPECIFIC HELPER METHODS ------------------------ //

  public static void uploadTestPDF() {
    File examplePDF = new File(resourcesFolderPath + File.separator
            + "AnnotatedPDFs" + File.separator + "Application_for_a_Birth_Certificate_Annotated.pdf");

    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
                .header("Content-Disposition", "attachment")
                .field("pdfType", "COMPLETED_APPLICATION")
                .field("file", examplePDF, "application/pdf")
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  public static void uploadTestFormPDF() {
    File examplePDF =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
            .header("Content-Disposition", "attachment")
            .field("pdfType", "BLANK_FORM")
            .field("file", examplePDF, "application/pdf")
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");

    JSONObject body = new JSONObject();
    body.put("pdfType", "BLANK_FORM");
    body.put("annotated", false);
    HttpResponse<String> getForm =
        Unirest.post(TestUtils.getServerUrl() + "/get-documents").body(body.toString()).asString();
    JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());
    // check that form has annotated = false in DB
    assertThat(getFormJSON.getJSONArray("documents").getJSONObject(0).getBoolean("annotated"))
        .isEqualTo(false);
  }

  public static void uploadTestAnnotatedFormPDF() {
    // upload unannotated form
    File examplePDF =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
            .field("pdfType", "BLANK_FORM")
            .header("Content-Disposition", "attachment")
            .field("file", examplePDF, "application/pdf")
            .field("privilegeLevel", UserType.Developer.toString())
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");

    // download unannotated form
    JSONObject body = new JSONObject();
    body.put("pdfType", "BLANK_FORM");
    body.put("annotated", false);
    HttpResponse<String> getForm =
        Unirest.post(TestUtils.getServerUrl() + "/get-documents").body(body.toString()).asString();
    JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());
    // check that form has annotated = false in DB
    assertThat(getFormJSON.getJSONArray("documents").getJSONObject(0).getBoolean("annotated"))
        .isEqualTo(false);
    String fileId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");

    // reupload same form, now annotated
    examplePDF =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");
    uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload-annotated")
            .field("pdfType", "BLANK_FORM")
            .header("Content-Disposition", "attachment")
            .field("file", examplePDF, "application/pdf")
            .field("fileId", fileId)
            .asString();
    uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");

    // download newly annotated form
    body = new JSONObject();
    body.put("pdfType", "BLANK_FORM");
    body.put("annotated", true);
    getForm =
        Unirest.post(TestUtils.getServerUrl() + "/get-documents").body(body.toString()).asString();
    getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());
    // check that form has annotated = TRUE in DB
    assertThat(getFormJSON.getJSONArray("documents").getJSONObject(0).getBoolean("annotated"))
        .isEqualTo(true);
  }

  public static JSONObject getFormAnswersTestPDFForm(JSONObject responseJSON) {
    JSONObject formAnswers = new JSONObject();
    JSONArray fields = responseJSON.getJSONArray("fields");
    for (int i = 0; i < fields.length(); i++) {
      JSONObject field = fields.getJSONObject(i);
      if (field.getString("fieldType").equals("TextField")) {
        if (field.getString("fieldName").equals("currentdate_af_date")) {
          formAnswers.put(field.getString("fieldName"), "7/14/20");
        } else {
          formAnswers.put(field.getString("fieldName"), "1");
        }
      } else if ((field.getString("fieldType").equals("CheckBox"))) {
        if (field.getString("fieldName").equals("Ribeye Steaks")) {
          formAnswers.put(field.getString("fieldName"), false);
        } else {
          formAnswers.put(field.getString("fieldName"), true);
        }
      } else if ((field.getString("fieldType").equals("PushButton"))) {
        formAnswers.put(field.getString("fieldName"), true);
      } else if ((field.getString("fieldType").equals("RadioButton"))) {
        formAnswers.put(field.getString("fieldName"), "Yes");
      } else if ((field.getString("fieldType").equals("ComboBox"))) {
        formAnswers.put(field.getString("fieldName"), "Choice2");
      } else if ((field.getString("fieldType").equals("ListBox"))) {
        JSONArray l = new JSONArray();
        l.put("Choice2");
        formAnswers.put(field.getString("fieldName"), l);
      } else if ((field.getString("fieldType").equals("SignatureField"))) {
        formAnswers.put(field.getString("fieldName"), new PDSignature());
      }
    }
    return formAnswers;
  }

  public static JSONArray csvToJSON(String filename) throws IOException {
    Reader reader = new FileReader(resourcesFolderPath + File.separator + filename);
    CSVReader csvReader = new CSVReader(reader);
    List<String[]> list = csvReader.readAll();
    Iterator<String[]> listIterator = list.iterator();
    JSONArray resultArray = new JSONArray();
    String[] columnNames = listIterator.next();
    while (listIterator.hasNext()) {
      String[] row = listIterator.next();
      JSONObject rowResult = new JSONObject();

      for (int i = 0; i < columnNames.length; i++) {
        rowResult.put(columnNames[i], row[i]);
      }
      resultArray.put(rowResult);
    }
    reader.close();
    csvReader.close();
    return resultArray;
  }

  public static void checkFieldsEquality(JSONArray correctFields, JSONArray fields) {
    int i = 0;
    while (i < correctFields.length()) {
      assertEquals(
          correctFields.get(i).toString().replace("\"", ""),
          fields.get(i).toString().replace("\"", ""));
      i++;
    }
  }
}
