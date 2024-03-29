package Security;

import User.User;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Objects;

import static com.mongodb.client.model.Filters.eq;

@Slf4j
public class EncryptionUtils {
  private static EncryptionUtils instance;
  private MongoDatabase db;
  private Aead aead;

  public static final Charset CHARSET = StandardCharsets.ISO_8859_1;
  public static final String masterKeyUri = Objects.requireNonNull(System.getenv("MASTERKEYURI"));

  public static final String credentials =
      Objects.requireNonNull(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));

  // This is just for because we aren't using encryption now, so we don't want to generate an AEAD
  private EncryptionUtils() {}

  private EncryptionUtils(MongoDatabase db) throws GeneralSecurityException, IOException {
    this.db = db;
    this.aead = generateAead();
  }

  // Remember to initialize before using!
  public static void initialize(MongoDatabase db) throws GeneralSecurityException, IOException {
    instance = new EncryptionUtils(db);
  }

  public static void initialize() {
    instance = new EncryptionUtils();
  }

  public static EncryptionUtils getInstance() {
    return instance;
  }

  // Generates an AEAD Object through Google Tink for encryption and decryption
  public Aead generateAead() throws GeneralSecurityException, IOException {
    AeadConfig.register();

    GoogleCredentials.generateCredentials();

    MongoCollection<Document> keyHandles = db.getCollection("keys", Document.class);
    Document keyDocument = keyHandles.find(eq("keyType", "encryption")).first();

    assert keyDocument != null;
    keyDocument.remove("_id");
    keyDocument.remove("keyType");

    JSONObject keyJson = new JSONObject(keyDocument);

    KeysetHandle keysetHandle =
        KeysetHandle.read(
            JsonKeysetReader.withJsonObject(keyJson),
            new GcpKmsClient().withCredentials(credentials).getAead(masterKeyUri));

    GoogleCredentials.deleteCredentials();

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

  public String encryptString(String inputString, String username) throws GeneralSecurityException {
    //    byte[] stringBytes = inputString.getBytes(CHARSET);
    //    byte[] aad = username.getBytes();
    //    String encryptedString =
    //        new String(getEncrypted(stringBytes, aad), CHARSET);
    //    return encryptedString;
    return inputString;
  }

  public String decryptString(String encryptedString, String username)
      throws GeneralSecurityException {
    //    byte[] aad = username.getBytes();
    //    byte[] encryptedBytes = encryptedString.getBytes(CHARSET);
    //
    //    byte[] decryptedString = getDecrypted(encryptedBytes, aad);
    //
    //    return new String(decryptedString, CHARSET);
    return encryptedString;
  }

  public InputStream encryptFile(InputStream fileStream, String username)
      throws IOException, GeneralSecurityException {
    //    try {
    //      byte[] fileBytes = IOUtils.toByteArray(fileStream);
    //      byte[] aad = username.getBytes();
    //
    //      InputStream encryptedStream = new ByteArrayInputStream(getEncrypted(fileBytes, aad));
    //
    //      return encryptedStream;
    //
    //    } catch (IOException | GeneralSecurityException e) {
    //      logger.error("Could not find file, or could not turn file into Byte Array");
    //      throw e;
    //    }
    return fileStream;
  }

  public InputStream decryptFile(InputStream encryptedFile, String username)
      throws GeneralSecurityException, IOException {
    //    byte[] aad = username.getBytes();
    //    byte[] encryptedBytes = encryptedFile.readAllBytes();
    //
    //    InputStream decryptedFileStream = new ByteArrayInputStream(getDecrypted(encryptedBytes,
    // aad));
    //
    //    return decryptedFileStream;
    return encryptedFile;
  }

  public void encryptUser(User user, String username) throws GeneralSecurityException, IOException {
    //    user.setAddress(encryptString(user.getAddress(), username));
    //    user.setBirthDate(encryptString(user.getBirthDate(), username));
    //    user.setCity(encryptString(user.getCity(), username));
    //    user.setEmail(encryptString(user.getEmail(), username));
    //    user.setPhone(encryptString(user.getPhone(), username));
    //    user.setZipcode(encryptString(user.getZipcode(), username));
  }

  public void decryptUser(User user, String username) throws GeneralSecurityException, IOException {
    //    user.setAddress(decryptString(user.getAddress(), username));
    //    user.setBirthDate(decryptString(user.getBirthDate(), username));
    //    user.setCity(decryptString(user.getCity(), username));
    //    user.setEmail(decryptString(user.getEmail(), username));
    //    user.setPhone(decryptString(user.getPhone(), username));
    //    user.setZipcode(decryptString(user.getZipcode(), username));
  }
}
