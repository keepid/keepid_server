package SecurityTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import File.FileType;
import Security.FileStorageCryptoPolicy;
import org.junit.Test;

public class FileStorageCryptoPolicyUnitTests {

  @Test
  public void formTemplatesDoNotRequireEncryptionAtRest() {
    assertFalse(FileStorageCryptoPolicy.requiresEncryptionAtRest(FileType.FORM));
  }

  @Test
  public void sensitiveFileTypesRequireEncryptionAtRest() {
    assertTrue(FileStorageCryptoPolicy.requiresEncryptionAtRest(FileType.APPLICATION_PDF));
    assertTrue(FileStorageCryptoPolicy.requiresEncryptionAtRest(FileType.IDENTIFICATION_PDF));
    assertTrue(FileStorageCryptoPolicy.requiresEncryptionAtRest(FileType.PROFILE_PICTURE));
    assertTrue(FileStorageCryptoPolicy.requiresEncryptionAtRest(FileType.MISC));
  }

  @Test
  public void detectsPdfMagicHeader() {
    byte[] pdfHeader = new byte[] {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
    byte[] nonPdf = new byte[] {'{', '"', 's', 't', 'a', 't', 'u', 's', '"'};
    assertTrue(FileStorageCryptoPolicy.looksLikePdf(pdfHeader));
    assertFalse(FileStorageCryptoPolicy.looksLikePdf(nonPdf));
  }
}
