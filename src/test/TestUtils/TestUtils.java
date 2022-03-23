package TestUtils;

import Config.AppConfig;
import Config.DeploymentLevel;
import Config.MongoConfig;
import Security.EncryptionTools;
import Security.EncryptionUtils;
import Security.GoogleCredentials;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.bson.Document;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUtils {
  private static final int SERVER_TEST_PORT = Integer.parseInt(System.getenv("TEST_PORT"));
  private static final String SERVER_TEST_URL = "http://localhost:" + SERVER_TEST_PORT;
  private static Javalin app;
  private static EncryptionUtils encryptionUtils;
  private static final String masterKeyUri = Objects.requireNonNull(System.getenv("MASTERKEYURI"));
  private static final String credentials =
      Objects.requireNonNull(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));

  public static void startServer() {
    if (app == null) {
      try {
        MongoConfig.getMongoClient();
        MongoDatabase db = MongoConfig.getDatabase(DeploymentLevel.TEST);
        try {
          EncryptionTools encryptionTools = new EncryptionTools(db);
          encryptionTools.generateAndUploadKeySet();
        } catch (Exception e) {
          System.out.println("error generating and uploading key set");
          System.err.println(e.getStackTrace());
        }
        EncryptionUtils.initialize();
        encryptionUtils = EncryptionUtils.getInstance();
      } catch (Exception e) {
        System.err.println(e.getStackTrace());
        System.exit(0);
      }
      app = AppConfig.appFactory(DeploymentLevel.TEST);
    }
  }

  public static String getServerUrl() {
    return SERVER_TEST_URL;
  }

  public static void setUpTestDB() {
    // If there are entries in the database, they should be cleared before more are added.
    //    MongoDatabase testDB = MongoConfig.getDatabase(DeploymentLevel.TEST);
    //    // Add an AED to the test database
    //    assert testDB != null;
    //    MongoCollection<Document> keysCollection = testDB.getCollection("keys", Document.class);
    //    Document aed = new Document();
    //    aed.append("primaryKeyId", 1234567890);
    //    keysCollection.insertOne(aed);
  }

  public static EncryptionUtils getEncryptionUtils() {
    return encryptionUtils;
  }

  // Tears down the test database by clearing all collections.
  public static void tearDownTestDB() {
    //    MongoConfig.dropDatabase(DeploymentLevel.TEST);
  }

  // A private method for hashing passwords.
  public static String hashPassword(String plainPass) {
    Argon2 argon2 = Argon2Factory.create();
    char[] passwordArr = plainPass.toCharArray();
    String passwordHash = null;
    try {
      passwordHash = argon2.hash(10, 65536, 1, passwordArr);
      argon2.wipeArray(passwordArr);
    } catch (Exception e) {
      argon2.wipeArray(passwordArr);
    }
    return passwordHash;
  }

  public static Aead getAead() throws GeneralSecurityException, IOException {
    MongoConfig.getMongoClient();
    MongoDatabase db = MongoConfig.getDatabase(DeploymentLevel.TEST);
    assert db != null;
    MongoCollection<Document> keyCollection = db.getCollection("keys", Document.class);
    Document handleDoc = keyCollection.find(Filters.eq("keyType", "encryption")).first();

    handleDoc.remove("fieldname");
    KeysetHandle keysetHandle =
        KeysetHandle.read(
            JsonKeysetReader.withJsonObject(new JSONObject(handleDoc)),
            new GcpKmsClient().withCredentials(credentials).getAead(masterKeyUri));
    GoogleCredentials.deleteCredentials();
    return keysetHandle.getPrimitive(Aead.class);
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

  public static void uploadFile(String username, String password, String filename) {
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

    File file = new File(filePath);
    HttpResponse<String> uploadResponse =
        Unirest.post(getServerUrl() + "/upload")
            .field("pdfType", "BLANK_FORM")
            .header("Content-Disposition", "attachment")
            .field("file", file)
            .asString();
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
    logout();
  }

  public static JSONObject responseStringToJSON(String response) {
    if (response.charAt(0) == '"') {
      String strippedResponse = response.substring(1, response.length() - 1).replace("\\", "");
      return new JSONObject(strippedResponse);
    }
    return new JSONObject(response);
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
