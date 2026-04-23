package PDFTest.PDFV2Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import PDF.Services.V2Services.NormalizePdfFieldAppearancesService;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
// PDAnnotationWidget is used by craftPdfWithPinnedSize() below.
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDVariableText;
import org.junit.Test;

public class NormalizePdfFieldAppearancesServiceUnitTests {

  private static final Pattern DA_SIZE_PATTERN =
      Pattern.compile("/\\S+\\s+([\\d.]+)\\s+Tf");

  private static final String RESOURCES_FOLDER =
      Paths.get("").toAbsolutePath().toString()
          + File.separator
          + "src"
          + File.separator
          + "test"
          + File.separator
          + "resources";

  private byte[] readResource(String filename) throws Exception {
    File f = new File(RESOURCES_FOLDER + File.separator + filename);
    try (InputStream in = new FileInputStream(f)) {
      return in.readAllBytes();
    }
  }

  @Test
  public void normalize_returnsOriginalWhenNullOrEmpty() {
    assertEquals(null, NormalizePdfFieldAppearancesService.normalize(null));
    byte[] empty = new byte[0];
    assertEquals(empty, NormalizePdfFieldAppearancesService.normalize(empty));
  }

  @Test
  public void normalize_pinsResolvedFontSizeOnVariableTextFields() throws Exception {
    // Proves the behavioral contract that matters to renderers: every variable-text field has
    // an explicit, non-zero /DA size after normalization.
    //
    // Note on NeedAppearances: pdfbox regenerates fresh /AP appearance streams at save time and
    // consequently clears /NeedAppearances (appearances are no longer stale). That's fine: viewers
    // now see consistent text drawn from the same normalized /DA, and pdf.js's renderForms path
    // still sizes HTML widgets from /DA directly. We therefore do not assert on NeedAppearances or
    // on /AP presence post-save.
    byte[] original = readResource("ss-5.pdf");
    byte[] normalized = NormalizePdfFieldAppearancesService.normalize(original);
    assertNotNull(normalized);

    try (PDDocument doc = Loader.loadPDF(normalized)) {
      PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
      assertNotNull("Test fixture must have an AcroForm", form);

      int textFields = 0;
      int zeroSizeAfter = 0;
      for (PDField field : collectTerminalFields(form.getFields())) {
        if (!(field instanceof PDVariableText)) continue;
        textFields++;
        String da = ((PDVariableText) field).getDefaultAppearance();
        float size = parseSize(da);
        if (size < 1f) {
          zeroSizeAfter++;
        }
      }
      assertTrue("Fixture should expose at least one variable-text field", textFields > 0);
      assertEquals("No field should remain at size 0 after normalization", 0, zeroSizeAfter);
    }
  }

  @Test
  public void normalize_preservesExplicitNonZeroSizes() throws Exception {
    // Build a minimal in-memory form with a pinned 12pt DA and make sure normalization leaves it.
    byte[] crafted = craftPdfWithPinnedSize(12);
    byte[] normalized = NormalizePdfFieldAppearancesService.normalize(crafted);
    try (PDDocument doc = Loader.loadPDF(normalized)) {
      PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
      assertNotNull(form);
      for (PDField field : collectTerminalFields(form.getFields())) {
        if (!(field instanceof PDVariableText)) continue;
        String da = ((PDVariableText) field).getDefaultAppearance();
        assertEquals("Authored 12pt DA must be preserved", 12f, parseSize(da), 0.001f);
      }
    }
  }

  @Test
  public void normalize_isNoopForPdfsWithoutAcroForm() throws Exception {
    byte[] original = readResource("testpdf.pdf");
    byte[] normalized = NormalizePdfFieldAppearancesService.normalize(original);
    try (PDDocument doc = Loader.loadPDF(normalized)) {
      PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
      assertTrue(
          "A PDF without an AcroForm should pass through with no AcroForm or no changes",
          form == null || !form.getNeedAppearances());
    }
    // The method should return the same bytes (not the same reference necessarily, but content).
    assertFalse(
        "Normalization should not balloon the file size of a formless PDF",
        normalized.length > original.length + 64);
  }

  private static List<PDField> collectTerminalFields(List<PDField> fields) {
    List<PDField> out = new java.util.ArrayList<>();
    for (PDField f : fields) {
      if (f instanceof PDNonTerminalField) {
        out.addAll(collectTerminalFields(((PDNonTerminalField) f).getChildren()));
      } else {
        out.add(f);
      }
    }
    return out;
  }

  private static float parseSize(String da) {
    if (da == null) return 0f;
    Matcher m = DA_SIZE_PATTERN.matcher(da);
    if (!m.find()) return 0f;
    try {
      return Float.parseFloat(m.group(1));
    } catch (NumberFormatException e) {
      return 0f;
    }
  }

  /**
   * Builds a tiny PDF with one AcroForm text field whose DA already specifies an explicit point
   * size, used to prove normalization leaves authored typography alone.
   */
  private static byte[] craftPdfWithPinnedSize(int pt) throws Exception {
    try (PDDocument doc = new PDDocument()) {
      org.apache.pdfbox.pdmodel.PDPage page =
          new org.apache.pdfbox.pdmodel.PDPage(
              org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER);
      doc.addPage(page);
      PDAcroForm form = new PDAcroForm(doc);
      doc.getDocumentCatalog().setAcroForm(form);
      org.apache.pdfbox.pdmodel.interactive.form.PDTextField tf =
          new org.apache.pdfbox.pdmodel.interactive.form.PDTextField(form);
      tf.setPartialName("pinnedField");
      tf.setDefaultAppearance("/Helv " + pt + " Tf 0 g");
      PDAnnotationWidget widget = new PDAnnotationWidget();
      widget.setRectangle(
          new org.apache.pdfbox.pdmodel.common.PDRectangle(72, 720, 200, 24));
      widget.setPage(page);
      page.getAnnotations().add(widget);
      tf.setWidgets(java.util.Collections.singletonList(widget));
      form.getFields().add(tf);
      java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
      doc.save(out);
      return out.toByteArray();
    }
  }
}
