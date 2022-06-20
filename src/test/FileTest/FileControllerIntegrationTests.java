package FileTest;

import Security.EncryptionUtils;
import TestUtils.TestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static FileTest.FileControllerIntegrationTestHelperMethods.uploadTestPDF;

public class FileControllerIntegrationTests {
  private static EncryptionUtils encryptionUtils;
  public static String username = "adminBSM";

  public static String currentPDFFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "PDFTest";

  public static String resourcesFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources";

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void uploadValidPDFTest() {
    TestUtils.login(username, username);
    uploadTestPDF();
    TestUtils.logout();
  }
}
