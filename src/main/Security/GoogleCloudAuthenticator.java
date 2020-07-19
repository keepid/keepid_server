package Security;

import com.google.cloud.kms.v1.*;
import com.google.cloud.kms.v1.CryptoKeyVersion.CryptoKeyVersionState;
import com.google.protobuf.ByteString;
import com.google.protobuf.FieldMask;
import com.google.protobuf.util.FieldMaskUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.stream.Collectors;

public class GoogleCloudAuthenticator {

  public static void quickstart(String projectId, String location) throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the parent from the project and location.
      LocationName parent = LocationName.of(projectId, location);
      // Call the API.
      KeyManagementServiceClient.ListKeyRingsPagedResponse response = client.listKeyRings(parent);

      // Iterate over each key ring and print its name.
      System.out.println("key rings:");
      for (KeyRing keyRing : response.iterateAll()) {
        System.out.printf("%s%n", keyRing.getName());
      }
    }
  }

  public static void createKeyRing(String projectId, String locationId, String id)
      throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the parent name from the project and location.
      LocationName locationName = LocationName.of(projectId, locationId);
      // Build the key ring to create.
      KeyRing keyRing = KeyRing.newBuilder().build();
      // Create the key ring.
      KeyRing createdKeyRing = client.createKeyRing(locationName, id, keyRing);
      System.out.printf("Created key ring %s%n", createdKeyRing.getName());
    }
  }

  public static void createKeySymmetricEncryptDecrypt(
      String projectId, String locationId, String keyRingId, String id) throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the parent name from the project, location, and key ring.
      KeyRingName keyRingName = KeyRingName.of(projectId, locationId, keyRingId);

      // Build the symmetric key to create.
      CryptoKey key =
          CryptoKey.newBuilder()
              .setPurpose(CryptoKey.CryptoKeyPurpose.ENCRYPT_DECRYPT)
              .setVersionTemplate(
                  CryptoKeyVersionTemplate.newBuilder()
                      .setAlgorithm(
                          CryptoKeyVersion.CryptoKeyVersionAlgorithm.GOOGLE_SYMMETRIC_ENCRYPTION))
              .build();

      // Create the key.
      CryptoKey createdKey = client.createCryptoKey(keyRingName, id, key);
      System.out.printf("Created symmetric key %s%n", createdKey.getName());
    }
  }

  public static void createKeyAsymmetricDecrypt(
      String projectId, String locationId, String keyRingId, String id) throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the parent name from the project, location, and key ring.
      KeyRingName keyRingName = KeyRingName.of(projectId, locationId, keyRingId);

      // Build the asymmetric key to create.
      CryptoKey key =
          CryptoKey.newBuilder()
              .setPurpose(CryptoKey.CryptoKeyPurpose.ASYMMETRIC_DECRYPT)
              .setVersionTemplate(
                  CryptoKeyVersionTemplate.newBuilder()
                      .setAlgorithm(
                          CryptoKeyVersion.CryptoKeyVersionAlgorithm.RSA_DECRYPT_OAEP_2048_SHA256))
              .build();

      // Create the key.
      CryptoKey createdKey = client.createCryptoKey(keyRingName, id, key);
      System.out.printf("Created asymmetric key %s%n", createdKey.getName());
    }
  }
  // Create a new asymmetric key for the purpose of signing and verifying data.
  public static void createKeyAsymmetricSign(
      String projectId, String locationId, String keyRingId, String id) throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the parent name from the project, location, and key ring.
      KeyRingName keyRingName = KeyRingName.of(projectId, locationId, keyRingId);

      // Build the asymmetric key to create.
      CryptoKey key =
          CryptoKey.newBuilder()
              .setPurpose(CryptoKey.CryptoKeyPurpose.ASYMMETRIC_SIGN)
              .setVersionTemplate(
                  CryptoKeyVersionTemplate.newBuilder()
                      .setAlgorithm(
                          CryptoKeyVersion.CryptoKeyVersionAlgorithm.RSA_SIGN_PKCS1_2048_SHA256))
              .build();

      // Create the key.
      CryptoKey createdKey = client.createCryptoKey(keyRingName, id, key);
      System.out.printf("Created asymmetric key %s%n", createdKey.getName());
    }
  }
  // Enable a disabled key version to be used again.
  public static void enableKeyVersion(
      String projectId, String locationId, String keyRingId, String keyId, String keyVersionId)
      throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the key version name from the project, location, key ring, key,
      // and key version.
      CryptoKeyVersionName keyVersionName =
          CryptoKeyVersionName.of(projectId, locationId, keyRingId, keyId, keyVersionId);

      // Build the updated key version, setting it to enabled.
      CryptoKeyVersion keyVersion =
          CryptoKeyVersion.newBuilder()
              .setName(keyVersionName.toString())
              .setState(CryptoKeyVersionState.ENABLED)
              .build();

      // Create a field mask of updated values.
      FieldMask fieldMask = FieldMaskUtil.fromString("state");

      // Destroy the key version.
      CryptoKeyVersion response = client.updateCryptoKeyVersion(keyVersion, fieldMask);
      System.out.printf("Enabled key version: %s%n", response.getName());
    }
  }
  // Disable a key version from use.
  public static void disableKeyVersion(
      String projectId, String locationId, String keyRingId, String keyId, String keyVersionId)
      throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the key version name from the project, location, key ring, key,
      // and key version.
      CryptoKeyVersionName keyVersionName =
          CryptoKeyVersionName.of(projectId, locationId, keyRingId, keyId, keyVersionId);

      // Build the updated key version, setting it to disbaled.
      CryptoKeyVersion keyVersion =
          CryptoKeyVersion.newBuilder()
              .setName(keyVersionName.toString())
              .setState(CryptoKeyVersionState.DISABLED)
              .build();

      // Create a field mask of updated values.
      FieldMask fieldMask = FieldMaskUtil.fromString("state");

      // Destroy the key version.
      CryptoKeyVersion response = client.updateCryptoKeyVersion(keyVersion, fieldMask);
      System.out.printf("Disabled key version: %s%n", response.getName());
    }
  }

  // Schedule destruction of the given key version.
  public static void destroyKeyVersion(
      String projectId, String locationId, String keyRingId, String keyId, String keyVersionId)
      throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the key version name from the project, location, key ring, key,
      // and key version.
      CryptoKeyVersionName keyVersionName =
          CryptoKeyVersionName.of(projectId, locationId, keyRingId, keyId, keyVersionId);

      // Destroy the key version.
      CryptoKeyVersion response = client.destroyCryptoKeyVersion(keyVersionName);
      System.out.printf("Destroyed key version: %s%n", response.getName());
    }
  }

  // Schedule destruction of the given key version.
  public static void restoreKeyVersion(
      String projectId, String locationId, String keyRingId, String keyId, String keyVersionId)
      throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the key version name from the project, location, key ring, key,
      // and key version.
      CryptoKeyVersionName keyVersionName =
          CryptoKeyVersionName.of(projectId, locationId, keyRingId, keyId, keyVersionId);

      // Restore the key version.
      CryptoKeyVersion response = client.restoreCryptoKeyVersion(keyVersionName);
      System.out.printf("Restored key version: %s%n", response.getName());
    }
  }

  // Get the public key associated with an asymmetric key.
  public static void getPublicKey(
      String projectId, String locationId, String keyRingId, String keyId, String keyVersionId)
      throws IOException, GeneralSecurityException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the key version name from the project, location, key ring, key,
      // and key version.
      CryptoKeyVersionName keyVersionName =
          CryptoKeyVersionName.of(projectId, locationId, keyRingId, keyId, keyVersionId);

      // Get the public key.
      PublicKey publicKey = client.getPublicKey(keyVersionName);
      System.out.printf("Public key: %s%n", publicKey.getPem());
    }
  }

  // Encrypt data with a given key.
  public static byte[] encryptSymmetric(
      String projectId, String locationId, String keyRingId, String keyId, String plaintext)
      throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the key version name from the project, location, key ring, key,
      // and key version.
      CryptoKeyName keyVersionName = CryptoKeyName.of(projectId, locationId, keyRingId, keyId);

      // Encrypt the plaintext.
      EncryptResponse response = client.encrypt(keyVersionName, ByteString.copyFromUtf8(plaintext));
      System.out.printf("Ciphertext: %s%n", response.getCiphertext().toStringUtf8());
      return response.getCiphertext().toByteArray();
    }
  }

  // Decrypt data that was encrypted using a symmetric key.
  public static byte[] decryptSymmetric(
      String projectId, String locationId, String keyRingId, String keyId, byte[] ciphertext)
      throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the key version name from the project, location, key ring, and
      // key.
      CryptoKeyName keyName = CryptoKeyName.of(projectId, locationId, keyRingId, keyId);

      // Decrypt the response.
      DecryptResponse response = client.decrypt(keyName, ByteString.copyFrom(ciphertext));
      System.out.printf("Plaintext: %s%n", response.getPlaintext().toStringUtf8());
      return response.getPlaintext().toByteArray();
    }
  }

  // Converts a base64-encoded PEM certificate like the one returned from Cloud
  // KMS into a DER formatted certificate for use with the Java APIs.
  private static byte[] convertPemToDer(String pem) {
    BufferedReader bufferedReader = new BufferedReader(new StringReader(pem));
    String encoded =
        bufferedReader
            .lines()
            .filter(line -> !line.startsWith("-----BEGIN") && !line.startsWith("-----END"))
            .collect(Collectors.joining());
    return Base64.getDecoder().decode(encoded);
  }

  // Decrypt data that was encrypted using the public key component of the given
  // key version.
  public static byte[] decryptAsymmetric(
      String projectId,
      String locationId,
      String keyRingId,
      String keyId,
      String keyVersionId,
      byte[] ciphertext)
      throws IOException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the key version name from the project, location, key ring, key,
      // and key version.
      CryptoKeyVersionName keyVersionName =
          CryptoKeyVersionName.of(projectId, locationId, keyRingId, keyId, keyVersionId);

      // Decrypt the ciphertext.
      AsymmetricDecryptResponse response =
          client.asymmetricDecrypt(keyVersionName, ByteString.copyFrom(ciphertext));
      System.out.printf("Plaintext: %s%n", response.getPlaintext().toStringUtf8());
      return response.getPlaintext().toByteArray();
    }
  }
  // Encrypt data that was encrypted using the public key component of the given
  // key version.
  public static byte[] encryptAsymmetric(
      String projectId,
      String locationId,
      String keyRingId,
      String keyId,
      String keyVersionId,
      String plaintext)
      throws IOException, GeneralSecurityException {
    // Initialize client that will be used to send requests. This client only
    // needs to be created once, and can be reused for multiple requests. After
    // completing all of your requests, call the "close" method on the client to
    // safely clean up any remaining background resources.
    try (KeyManagementServiceClient client = KeyManagementServiceClient.create()) {
      // Build the key version name from the project, location, key ring, key,
      // and key version.
      CryptoKeyVersionName keyVersionName =
          CryptoKeyVersionName.of(projectId, locationId, keyRingId, keyId, keyVersionId);

      // Get the public key.
      PublicKey publicKey = client.getPublicKey(keyVersionName);

      // Convert the public PEM key to a DER key (see helper below).
      byte[] derKey = convertPemToDer(publicKey.getPem());
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derKey);
      java.security.PublicKey rsaKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);

      // Encrypt plaintext for the 'RSA_DECRYPT_OAEP_2048_SHA256' key.
      // For other key algorithms:
      // https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html
      Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
      OAEPParameterSpec oaepParams =
          new OAEPParameterSpec(
              "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
      cipher.init(Cipher.ENCRYPT_MODE, rsaKey, oaepParams);
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      System.out.printf("Ciphertext: %s%n", ciphertext);
      return ciphertext;
    }
  }

  public static void main(String[] args) {
    //    some example usages here
    String projectId = "keepid-282419";
    String location = "global";
    String keyRingId = "testJavaKeyRing";
    String cryptoKeyIdSymmetric = "testJavaKeyIdSymmetric";
    String cryptoKeyIdAsymmetric = "testJavaKeyIdAsymmetric";
    String cryptoKeyIdSigning = "testJavaKeySigning";
    String testKeyVersion = "1";
    String plaintext = "some secret message here";
    try {
      quickstart(projectId, location);
      //      createKeyRing(projectId, location, keyRingId);
      //      createKeySymmetricEncryptDecrypt(projectId, location, keyRingId,
      // cryptoKeyIdSymmetric);
      //      createKeyAsymmetricDecrypt(projectId, location, keyRingId, cryptoKeyIdAsymmetric);
      //      createKeyAsymmetricSign(projectId, location, keyRingId, cryptoKeyIdSigning);
      //      enableKeyVersion(projectId, location, keyRingId, cryptoKeyIdSymmetric,
      // testKeyVersion);
      //      enableKeyVersion(projectId, location, keyRingId, cryptoKeyIdAsymmetric,
      // testKeyVersion);

      //      destroyKeyVersion(projectId, location, keyRingId, cryptoKeyIdSymmetric,
      // testKeyVersion);
      //      restoreKeyVersion(projectId, location, keyRingId, cryptoKeyIdSymmetric,
      // testKeyVersion);
      //      disableKeyVersion(projectId, location, keyRingId, cryptoKeyIdSymmetric,
      // testKeyVersion);
      //      enableKeyVersion(projectId, location, keyRingId, cryptoKeyIdAsymmetric,
      // testKeyVersion);
      //      getPublicKey(projectId, location, keyRingId, cryptoKeyIdAsymmetric, testKeyVersion);
      //      byte[] cipher =
      //          encryptSymmetric(projectId, location, keyRingId, cryptoKeyIdSymmetric, plaintext);
      //      byte[] plaintextReturn =
      //          decryptSymmetric(projectId, location, keyRingId, cryptoKeyIdSymmetric, cipher);
      //      System.out.println(new String(plaintextReturn));
      //
      //      byte[] cipher2 =
      //          encryptAsymmetric(
      //              projectId, location, keyRingId, cryptoKeyIdAsymmetric, testKeyVersion,
      // plaintext);
      //      byte[] plaintextReturn2 =
      //          decryptAsymmetric(
      //              projectId, location, keyRingId, cryptoKeyIdAsymmetric, testKeyVersion,
      // cipher2);
      //      System.out.println(new String(plaintextReturn2));
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}
