package TestUtils;

import Config.AppConfigV2;
import Config.DeploymentLevel;
import Config.MongoConfig;
import Security.GoogleCredentials;
import User.UserType;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
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
  private static final String masterKeyUri = Objects.requireNonNull(System.getenv("MASTERKEYURI"));
  private static final String credentials =
      Objects.requireNonNull(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));

  public static void startServer() {
    if (app == null) {
      Injector injector = Guice.createInjector(new TestModule());
      AppConfigV2 appConfigV2 = injector.getInstance(AppConfigV2.class);
      app = appConfigV2.appFactory(DeploymentLevel.TEST);
      SignInUser();
    }
  }

  // TODO(xander) fix
  static void SignInUser() {
    JSONObject body = new JSONObject();
    body.put("password", "password");
    body.put("username", "username");
    body.put("privilegeLevel", UserType.Worker.toString());
    body.put("firstname", "asdf");
    body.put("lastname", "asdf");
    body.put("birthDate", "01/02/1991");
    body.put("email", "asdf@as.com");
    body.put("phonenumber", "1112221122");
    body.put("address", "asdf");
    body.put("city", "asdf");
    body.put("state", "IL");
    body.put("zipcode", "12112");
    body.put("twoFactorOn", "asdf");
    body.put("personRole", UserType.Client.toString());

    HttpResponse<String> loginResponse =
        Unirest.post(SERVER_TEST_URL + "/create-user").body(body.toString()).asString();
    JSONObject loginResponseJSON = TestUtils.responseStringToJSON(loginResponse.getBody());
    assertThat(loginResponseJSON.getString("status")).isEqualTo("AUTH_SUCCESS");
  }

  public static String getServerUrl() {
    return SERVER_TEST_URL;
  }

  // A private method for hashing passwords.
  private static String hashPassword(String plainPass) {
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
            .field("pdfType", "FORM")
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
    PDDocument pdfDocument = PDDocument.load(inputStream);
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
