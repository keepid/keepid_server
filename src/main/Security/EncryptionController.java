package Security;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Objects;

@Slf4j
public class EncryptionController {
  private MongoDatabase db;
  private Aead aead;

  public static final String masterKeyUri = Objects.requireNonNull(System.getenv("MASTERKEYURI"));

  public static final String credentials = Objects.requireNonNull(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));

  public EncryptionController(MongoDatabase db) throws GeneralSecurityException, IOException {
    this.db = db;
    this.aead = generateAead();
  }

  private Aead generateAead() throws GeneralSecurityException, IOException {
    log.info("DEBUG: Running new EncryptionController logic");
    log.info("Generating AEAD Prim");
    AeadConfig.register();
    MongoCollection<Document> keyHandles = db.getCollection("keys", Document.class);
    Document keyDocument = keyHandles.find().first();

    log.info("DEBUG: Database Name is: '{}'", db.getName());

    if (keyDocument == null) {
      System.out.println(db.getName());
      if (db.getName().equals("test-db")) {
        EncryptionTools tools = new EncryptionTools(db);
        tools.generateGoogleCredentials();
        try (BufferedReader reader = new BufferedReader(new FileReader("key.json"))) {
          String jsonString = reader.readLine(); // Read the single line
          if (jsonString != null) {
            try {
              JSONObject keyJson = new JSONObject(jsonString);
              keyJson.remove("_id");
              keyJson.remove("keyType");
              KeysetHandle keysetHandle = KeysetHandle.read(
                  JsonKeysetReader.withJsonObject(keyJson),
                  new GcpKmsClient().withCredentials(credentials).getAead(masterKeyUri));
              return keysetHandle.getPrimitive(Aead.class);
            } catch (JSONException e) {
              System.err.println("Error parsing JSON string: " + e.getMessage());
            }
          } else {
            System.out.println("File is empty or contains no lines.");
          }
        } catch (IOException e) {
          System.err.println("Error reading file: " + e.getMessage());
        }
      } else if (db.getName().equals("staging-db")) {
        // For Docker/Staging: If no key exists, generate one and save it.
        log.info("No key found for staging-db. Generating new key...");
        EncryptionTools tools = new EncryptionTools(db);
        tools.generateGoogleCredentials();
        tools.generateAndUploadKeySet();

        // Now fetch it again
        keyDocument = keyHandles.find().first();
        if (keyDocument == null) {
          throw new GeneralSecurityException("Failed to generate and save key for staging-db");
        }
        // Proceed to use the unexpected keyDocument below
      }
    }
    keyDocument.remove("_id");
    keyDocument.remove("keyType");

    JSONObject keyJson = new JSONObject(keyDocument);

    KeysetHandle keysetHandle = KeysetHandle.read(
        JsonKeysetReader.withJsonObject(keyJson),
        new GcpKmsClient().withCredentials(credentials).getAead(masterKeyUri));

    return keysetHandle.getPrimitive(Aead.class);
  }

  public byte[] getEncrypted(byte[] data, byte[] aad) throws GeneralSecurityException {
    try {
      byte[] ciphertext = aead.encrypt(data, aad);

      return ciphertext;

    } catch (GeneralSecurityException e) {
      log.error("General Security Exception thrown, encrpytion unsuccessful");
      throw e;
    }
  }

  public byte[] getDecrypted(byte[] ciphertext, byte[] aad) throws GeneralSecurityException {
    try {
      byte[] decrypted = aead.decrypt(ciphertext, aad);

      return decrypted;
    } catch (GeneralSecurityException e) {
      log.error("Decryption Unsuccessful, double check aead");
      throw e;
    }
  }

  public InputStream encryptFile(InputStream fileStream, String username)
      throws IOException, GeneralSecurityException {
    log.info("Encrypting File");
    try {
      byte[] fileBytes = IOUtils.toByteArray(fileStream);
      byte[] aad = username.getBytes();

      InputStream encryptedStream = new ByteArrayInputStream(getEncrypted(fileBytes, aad));

      return encryptedStream;

    } catch (IOException | GeneralSecurityException e) {
      log.error("Could not find file, or could not turn file into Byte Array");
      throw e;
    }
  }

  public InputStream decryptFile(InputStream encryptedFile, String username)
      throws GeneralSecurityException, IOException {
    log.info("Decrypting File");
    byte[] aad = username.getBytes();
    byte[] encryptedBytes = encryptedFile.readAllBytes();

    InputStream decryptedFileStream = new ByteArrayInputStream(getDecrypted(encryptedBytes, aad));

    return decryptedFileStream;
  }
}
