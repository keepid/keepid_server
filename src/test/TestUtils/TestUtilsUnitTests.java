package TestUtils;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Organization.Organization;
import PDF.Services.CrudServices.ImageToPDFService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static PDFTest.PDFTestUtils.resourcesFolderPath;
import static TestUtils.TestUtils.assertPDFEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtilsUnitTests {

  @Test
  public void setUpAndTeardownTest() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
    MongoDatabase testDB = MongoConfig.getDatabase(DeploymentLevel.TEST);
    MongoCollection<Organization> orgCollection =
        testDB.getCollection("organization", Organization.class);
    assertEquals(
        "311 Broad Street",
        Objects.requireNonNull(
                orgCollection.find(Filters.eq("orgName", "Broad Street Ministry")).first())
            .getOrgStreetAddress());
    TestUtils.tearDownTestDB();
  }

  @Test
  public void testPDFEquals() throws IOException {
    File inputFile = new File(resourcesFolderPath + File.separator + "Application_for_a_Birth_Certificate.pdf");
    InputStream inputFileStream = FileUtils.openInputStream(inputFile);
    File expectedOutputFile = new File(resourcesFolderPath + File.separator + "Application_for_a_Birth_Certificate_copy.pdf");
    InputStream expectedOutputFileStream = FileUtils.openInputStream(expectedOutputFile);

    assertPDFEquals(expectedOutputFileStream, inputFileStream);
  }

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
