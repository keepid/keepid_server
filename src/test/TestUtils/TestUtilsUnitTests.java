package TestUtils;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static PDFTest.PDFTestUtils.resourcesFolderPath;
import static TestUtils.TestUtils.assertPDFEquals;

public class TestUtilsUnitTests {

  @Test(expected = AssertionError.class)
  public void testPDFNotEquals() throws IOException {
    File inputFile = new File(resourcesFolderPath + File.separator + "Application_for_a_Birth_Certificate.pdf");
    InputStream inputFileStream = FileUtils.openInputStream(inputFile);
    File expectedOutputFile = new File(resourcesFolderPath + File.separator + "testpdf.pdf");
    InputStream expectedOutputFileStream = FileUtils.openInputStream(expectedOutputFile);

    assertPDFEquals(expectedOutputFileStream, inputFileStream);
  }

  //  @Test
  //  public void testEncryptionSetup() {
  //    TestUtils.startServer();
  //    TestUtils.setUpTestDB();
  //    GoogleCredentials.generateCredentials();
  //    try {
  //      Aead aead = TestUtils.getAead();
  //      String original = "hello world";
  //      byte[] ciphertext = aead.encrypt(original.getBytes(), "".getBytes());
  //      byte[] decrypted = aead.decrypt(ciphertext, "".getBytes());
  //      assertEquals(original, new String(decrypted));
  //    } catch (GeneralSecurityException | IOException e) {
  //      e.printStackTrace();
  //      assert false;
  //    }
  //    GoogleCredentials.deleteCredentials();
  //    TestUtils.tearDownTestDB();
  //  }
}
