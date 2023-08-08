package PDFTest.PDFV2Test;

import java.io.File;
import java.nio.file.Paths;

public class PDFTestUtilsV2 {
  public static String resourcesFolderPath =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources";
}
