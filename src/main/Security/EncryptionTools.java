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
import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class EncryptionTools {
  public MongoDatabase db;
  public static final String credentials =
      Objects.requireNonNull(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
  public static final String masterKeyUri = Objects.requireNonNull(System.getenv("MASTERKEYURI"));

  public EncryptionTools(MongoDatabase db) {
    this.db = db;
  }

  // Used for maintaining keys in mongodb, use with caution.
  public void generateAndUploadKeySet() throws GeneralSecurityException, IOException {
    AeadConfig.register();

    try {
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

    KeysetHandle handle = KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"));
    String keysetFileName = "key.json";
    handle.write(JsonKeysetWriter.withPath(keysetFileName), kekAead);

    ObjectMapper objectMapper = new ObjectMapper();
    File file = new File("key.json");
    Map<String, Object> map =
        objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
    JSONObject keyJson = new JSONObject(map);

    MongoCollection<Document> keyHandles = this.db.getCollection("keys", Document.class);
    keyHandles.deleteMany(new Document());
    //    Document keyDocument = new Document("key.json");
    keyHandles.insertOne(Document.parse(keyJson.toString()));
  }
}
