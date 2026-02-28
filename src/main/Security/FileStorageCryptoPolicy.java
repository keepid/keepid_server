package Security;

import File.FileType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

public final class FileStorageCryptoPolicy {
  private FileStorageCryptoPolicy() {}

  public static boolean requiresEncryptionAtRest(FileType fileType) {
    if (fileType == null) {
      return true;
    }
    // Templates are shared resources and should not be user-AAD bound.
    return fileType != FileType.FORM;
  }

  public static InputStream prepareForStorage(
      InputStream source,
      FileType fileType,
      String username,
      EncryptionController encryptionController)
      throws GeneralSecurityException, IOException {
    if (requiresEncryptionAtRest(fileType)) {
      return encryptionController.encryptFile(source, username);
    }
    return source;
  }

  public static InputStream openForRead(
      byte[] storedBytes,
      FileType fileType,
      String username,
      EncryptionController encryptionController)
      throws GeneralSecurityException, IOException {
    if (requiresEncryptionAtRest(fileType)) {
      return encryptionController.decryptFile(new ByteArrayInputStream(storedBytes), username);
    }

    // Preferred path for template resources: plain PDF bytes.
    if (looksLikePdf(storedBytes)) {
      return new ByteArrayInputStream(storedBytes);
    }

    // Temporary legacy compatibility for old template files that were encrypted.
    return encryptionController.decryptFile(new ByteArrayInputStream(storedBytes), username);
  }

  public static boolean looksLikePdf(byte[] bytes) {
    return bytes != null
        && bytes.length >= 4
        && bytes[0] == '%'
        && bytes[1] == 'P'
        && bytes[2] == 'D'
        && bytes[3] == 'F';
  }
}
