package Security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.KmsAeadKeyManager;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class EncryptionTools {
  public MongoDatabase db;
  public static final String credentials =
      Objects.requireNonNull(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
  public static final String masterKeyUri = Objects.requireNonNull(System.getenv("MASTERKEYURI"));
  public static final String credential_contents = Objects.requireNonNull(System.getenv("GOOGLE_APPLICATION_CREDENTIALS_CONTENTS"));

  public EncryptionTools(MongoDatabase db) {
    this.db = db;
  }

  public boolean generateGoogleCredentials() {
    File file = new File(credentials);
    if (file.length() == 0 || !file.isFile()) {
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map =
            objectMapper.readValue(credential_contents, new TypeReference<Map<String, Object>>() {
            });
        JSONObject keyJson = new JSONObject(map);
        Files.writeString(Path.of(credentials), keyJson.toString());
      } catch (Exception e) {
        System.out.println("exception: " + e);
      }
    }
    return true;
  }

  // Used for maintaining keys in mongodb, use with caution.
  public void generateAndUploadKeySet() throws GeneralSecurityException, IOException {
    AeadConfig.register();

    try {
      System.out.println(credentials);
      System.out.println(masterKeyUri);
      GcpKmsClient.register(Optional.of(masterKeyUri), Optional.of(credentials));
    } catch (Exception ex) {
      System.err.println("Error initializing GCP client: " + ex);
      System.exit(1);
    }

    Aead kekAead = null;
    try {
      KeysetHandle handle = KeysetHandle.generateNew(KmsAeadKeyManager.createKeyTemplate(masterKeyUri));
      kekAead = handle.getPrimitive(Aead.class);
    } catch (GeneralSecurityException ex) {
      System.err.println("Error creating primitive: %s " + ex);
      System.exit(1);
    }
    File file = new File("key.json");
    if (!file.exists()) {
      KeysetHandle handle = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"));
      String keysetFileName = "key.json";
      handle.write(JsonKeysetWriter.withPath(keysetFileName), kekAead);
    }
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> map =
        objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {
        });
    JSONObject keyJson = new JSONObject(map);

    MongoCollection<Document> keyHandles = this.db.getCollection("keys", Document.class);
    keyHandles.deleteMany(new Document());
    //    Document keyDocument = new Document("key.json");
    keyHandles.insertOne(Document.parse(keyJson.toString()));
  }
}
