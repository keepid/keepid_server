package TestUtils;

import Config.AppConfig;
import Config.DeploymentLevel;
import Config.MongoConfig;
import Security.EncryptionTools;
import Security.EncryptionUtils;
import com.mongodb.client.MongoDatabase;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class TestUtils {
  private static final Optional<Integer> SERVER_TEST_PORT =
      Optional.ofNullable(System.getenv("TEST_PORT")).map(Integer::valueOf);
  private static final String SERVER_TEST_URL = "http://localhost:" + SERVER_TEST_PORT.orElse(7001);
  private static Javalin app;
  private static EncryptionUtils encryptionUtils;

  public static Javalin startServer() {
    if (SERVER_TEST_PORT.isEmpty()) {
      throw new IllegalStateException(
          "Please run test with env file. You can do this by going to the edit configurations "
              + "menu next to the run test button in the top right hand corner of IntelliJ.");
    }
    if (app == null) {
      try {
        MongoConfig.getMongoClient();
        MongoDatabase db = MongoConfig.getDatabase(DeploymentLevel.TEST);
        EncryptionTools encryptionTools = new EncryptionTools(db);
        encryptionTools.generateGoogleCredentials();
        try {
          encryptionTools.generateAndUploadKeySet();
        } catch (Exception e) {
          System.out.println("exception: " + e);
        }
        EncryptionUtils.initialize();
        encryptionUtils = EncryptionUtils.getInstance();
      } catch (Exception e) {
        System.err.println(e.getStackTrace());
        System.exit(0);
      }
      app = AppConfig.appFactory(DeploymentLevel.TEST);
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (app != null) {
          app.stop();
          app = null;
        }
      }));
      return app;
    }
    return app;
  }

  public static void stopServer() {
    if (app != null) {
      app.stop();
      app = null;
    }
  }

  public static String getServerUrl() {
    return SERVER_TEST_URL;
  }

  public static EncryptionUtils getEncryptionUtils() {
    return encryptionUtils;
  }

  // Tears down the test database by clearing all collections.
  // The server is intentionally kept alive so other test classes that
  // share the same JVM can reuse it (encryption keys stay in memory).
  public static void tearDownTestDB() {
    MongoConfig.dropDatabase(DeploymentLevel.TEST);
  }

  // A private method for hashing passwords (lightweight params for test speed).
  public static String hashPassword(String plainPass) {
    Argon2 argon2 = Argon2Factory.create();
    char[] passwordArr = plainPass.toCharArray();
    String passwordHash = null;
    try {
      passwordHash = argon2.hash(1, 1024, 1, passwordArr);
      argon2.wipeArray(passwordArr);
    } catch (Exception e) {
      argon2.wipeArray(passwordArr);
    }
    return passwordHash;
  }

  public static void login(String username, String password) {
    JSONObject body = new JSONObject();
    body.put("password", password);
    body.put("username", username);
    HttpResponse<String> loginResponse =
        Unirest.post(SERVER_TEST_URL + "/login").body(body.toString()).asString();
    JSONObject loginResponseJSON = TestUtils.responseStringToJSON(loginResponse.getBody());
    assertThat(loginResponseJSON.getString("status")).isEqualTo("AUTH_SUCCESS");
  }

  public static void logout() {
    HttpResponse<String> logoutResponse = Unirest.get(SERVER_TEST_URL + "/logout").asString();
    JSONObject logoutResponseJSON = TestUtils.responseStringToJSON(logoutResponse.getBody());
    assertThat(logoutResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  public static void uploadFile(String username, String password, String filename) throws IOException {
    login(username, password);
    String filePath =
        Paths.get("").toAbsolutePath().toString()
            + File.separator
            + "src"
            + File.separator
            + "test"
            + File.separator
            + "resources"
            + File.separator
            + filename;
    String mimeType = Files.probeContentType(Path.of(filePath));

    File file = new File(filePath);
    HttpResponse<String> uploadResponse =
        Unirest.post(getServerUrl() + "/upload")
            .field("pdfType", "FORM")
            .header("Content-Disposition", "attachment")
            .field("file", file, mimeType)
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
    logout();
  }

  public static JSONObject responseStringToJSON(String response) {
    if (response == null) throw new JSONException("null response");

    String s = stripBomAndWhitespace(response);

    // Parse the top-level value without assumptions
    Object v = new JSONTokener(s).nextValue();

    // Case 1: it's already a JSON object
    if (v instanceof JSONObject jo) return jo;

    // Case 2: it's a JSON *string literal* that itself contains JSON text
    if (v instanceof String str) {
      String inner = stripBomAndWhitespace(str);
      if (inner.startsWith("{")) return new JSONObject(inner);
      throw new JSONException("Quoted string does not contain an object: " + preview(inner));
    }
    throw new JSONException("Expected object; got " + v.getClass().getSimpleName() + ": " + preview(s));
  }

  private static String stripBomAndWhitespace(String s) {
    s = s.strip();                 // trim whitespace
    if (s.startsWith("\uFEFF"))    // remove UTF-8 BOM if present
      s = s.substring(1);
    return s;
  }

  private static String preview(String s) {
    return s.length() <= 60 ? s : s.substring(0, 57) + "...";
  }

  public static JSONObject getFieldValues(InputStream inputStream) throws IOException {
    PDDocument pdfDocument = Loader.loadPDF(inputStream);
    JSONObject fieldValues = new JSONObject();
    PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm();
    List<PDField> fields = new LinkedList<>();
    fields.addAll(acroForm.getFields());
    while (!fields.isEmpty()) {
      PDField field = fields.get(0);
      if (field instanceof PDNonTerminalField) {
        // If the field has children
        List<PDField> childrenFields = ((PDNonTerminalField) field).getChildren();
        fields.addAll(childrenFields);
      } else {
        fieldValues.put(field.getFullyQualifiedName(), field.getValueAsString());
      }

      // Delete field just gotten so we do not infinite recurse
      fields.remove(0);
    }
    pdfDocument.close();
    return fieldValues;
  }

  public static void assertPDFEquals(InputStream inputStream1, InputStream inputStream2) {
    try {
      PDDocument pdfDocument1 = Loader.loadPDF(inputStream1);
      PDDocument pdfDocument2 = Loader.loadPDF(inputStream2);

      PDPageTree pdfDocumentPages1 = pdfDocument1.getPages();
      PDPageTree pdfDocumentPages2 = pdfDocument2.getPages();
      Iterator<PDPage> pdfDocumentIterator1 = pdfDocumentPages1.iterator();
      Iterator<PDPage> pdfDocumentIterator2 = pdfDocumentPages2.iterator();

      while (pdfDocumentIterator1.hasNext() && pdfDocumentIterator2.hasNext()) {
        Iterator<PDStream> contentIterator1 = pdfDocumentIterator1.next().getContentStreams();
        Iterator<PDStream> contentIterator2 = pdfDocumentIterator2.next().getContentStreams();

        while (contentIterator1.hasNext() && contentIterator2.hasNext()) {
          assertArrayEquals(contentIterator1.next().toByteArray(), contentIterator2.next().toByteArray());
        }

        assertFalse(contentIterator1.hasNext());
        assertFalse(contentIterator2.hasNext());
      }

      assertFalse(pdfDocumentIterator1.hasNext());
      assertFalse(pdfDocumentIterator2.hasNext());
    } catch (IOException exception) {
      fail("Invalid PDF Document");
    }
  }

  public static Set<String> validFieldTypes =
      new HashSet<>(
          Arrays.asList(
              "CheckBox",
              "PushButton",
              "RadioButton",
              "ComboBox",
              "ListBox",
              "TextField",
              "SignatureField"));
}
