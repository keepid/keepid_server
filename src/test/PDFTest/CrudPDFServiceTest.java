package PDFTest;

import TestUtils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import static PDFTest.PDFTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

public class CrudPDFServiceTest {
  // ------------------ UPLOAD PDF TESTS ------------------------ //

  @Test
  public void uploadValidPDFTest() {
    TestUtils.login(username, username);
    uploadTestPDF();
    TestUtils.logout();
  }

  @Test
  public void uploadFormTest() {
    TestUtils.login(username, username);
    uploadTestFormPDF();
    TestUtils.logout();
  }

  @Test
  public void uploadAnnotatedPDFFormTest() {
    TestUtils.login(username, username);
    uploadTestAnnotatedFormPDF();
    TestUtils.logout();
  }

  @Test
  public void uploadValidPDFTestExists() {
    TestUtils.login(username, username);
    uploadTestPDF();
    searchTestPDF();
    TestUtils.logout();
  }

  @Test
  public void uploadValidPDFTestExistsAndDelete() {
    TestUtils.login(username, username);
    uploadTestPDF();
    JSONObject allDocuments = searchTestPDF();
    String idString = allDocuments.getJSONArray("documents").getJSONObject(0).getString("id");
    // delete(idString);
    TestUtils.logout();
  }

  @Test
  public void uploadInvalidPDFTypeTest() {
    TestUtils.login(username, username);
    File examplePDF =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
            .field("pdfType", "")
            .header("Content-Disposition", "attachment")
            .field("file", examplePDF)
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("INVALID_PDF_TYPE");
    TestUtils.logout();
  }

  @Test
  public void uploadNullPDFTest() {
    TestUtils.login(username, username);
    File examplePDF = null;
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
            .header("Content-Disposition", "attachment")
            .field("pdfType", "APPLICATION")
            .asString();

    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("INVALID_PDF");
    TestUtils.logout();
  }

  public static void uploadTestPDF() {
    File examplePDF =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");

    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
            .field("pdfType", "APPLICATION")
            .header("Content-Disposition", "attachment")
            .field("file", examplePDF)
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  public static void uploadTestFormPDF() {
    File examplePDF =
        new File(resourcesFolderPath + File.separator + "CIS_401_Final_Progress_Report.pdf");
    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload")
            .field("pdfType", "FORM")
            .header("Content-Disposition", "attachment")
            .field("file", examplePDF)
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");

    JSONObject body = new JSONObject();
    body.put("pdfType", "FORM");
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
            .field("pdfType", "FORM")
            .header("Content-Disposition", "attachment")
            .field("file", examplePDF)
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");

    // download unannotated form
    JSONObject body = new JSONObject();
    body.put("pdfType", "FORM");
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
            .field("pdfType", "FORM")
            .header("Content-Disposition", "attachment")
            .field("file", examplePDF)
            .field("fileId", fileId)
            .asString();
    uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");

    // download newly annotated form
    body = new JSONObject();
    body.put("pdfType", "FORM");
    body.put("annotated", true);
    getForm =
        Unirest.post(TestUtils.getServerUrl() + "/get-documents").body(body.toString()).asString();
    getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());
    // check that form has annotated = TRUE in DB
    assertThat(getFormJSON.getJSONArray("documents").getJSONObject(0).getBoolean("annotated"))
        .isEqualTo(true);
  }

  // Need to get it so it will only allow PDF and not docx+
  //  @Test
  //  public void uploadDocxTest() {
  //    TestUtils.login(username, username);
  //    File exampleDocx = new File(resourcesFolderPath + File.separator + "job_description.docx");
  //    HttpResponse<String> uploadResponse =
  //        Unirest.post(TestUtils.getServerUrl() + "/upload")
  //            .header("Content-Disposition", "attachment")
  //            .field("pdfType", "APPLICATION")
  //            .field("file", exampleDocx)
  //            .asString();
  //    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
  //    assertThat(uploadResponseJSON.getString("status")).isEqualTo("INVALID_PDF");
  //    TestUtils.logout();
  //  }

  // ------------------ DOWNLOAD PDF TESTS ------------------------ //

  @Test
  public void downloadTestFormTest() throws IOException, GeneralSecurityException {
    TestUtils.login(username, username);
    File testPdf = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
    String fileId = uploadFileAndGetFileId(testPdf, "FORM");

    JSONObject body = new JSONObject();
    body.put("fileId", fileId);
    body.put("pdfType", "FORM");
    HttpResponse<File> downloadFileResponse =
        Unirest.post(TestUtils.getServerUrl() + "/download")
            .body(body.toString())
            .asFile(resourcesFolderPath + File.separator + "downloaded_form.pdf");
    assertThat(downloadFileResponse.getStatus()).isEqualTo(200);
  }

  @Test
  public void downloadPDFTypeNullTest() throws IOException, GeneralSecurityException {
    TestUtils.login(username, username);
    File testPdf = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
    String fileId = uploadFileAndGetFileId(testPdf, "FORM");

    JSONObject body = new JSONObject();
    body.put("fileId", fileId);
    body.put("pdfType", (String) null);
    HttpResponse downloadFileResponse =
        Unirest.post(TestUtils.getServerUrl() + "/download").body(body.toString()).asString();
    assertThat(downloadFileResponse.getStatus()).isEqualTo(500);
  }

  // ------------------ GET PDF TESTS ------------------------ //

  @Test
  public void getDocumentsTargetUser() throws IOException, GeneralSecurityException {
    TestUtils.login("workerttfBSM", "workerttfBSM");
    clearAllDocuments();
    File applicationPDF = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
    String fileId = uploadFileAndGetFileId(applicationPDF, "FORM");
    TestUtils.logout();
    TestUtils.login(username, username);

    JSONObject body = new JSONObject();
    body.put("pdfType", "FORM");
    body.put("annotated", false);
    body.put("targetUser", "workerttfBSM");
    HttpResponse<String> getForm =
        Unirest.post(TestUtils.getServerUrl() + "/get-documents").body(body.toString()).asString();
    JSONObject getFormJSON = TestUtils.responseStringToJSON(getForm.getBody());
    String downId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
    // String fileId = getFormJSON.getJSONArray("documents").getJSONObject(0).getString("id");
    assertThat(fileId).isEqualTo(downId);
    TestUtils.logout();
  }
}
