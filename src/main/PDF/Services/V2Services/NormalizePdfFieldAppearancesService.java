package PDF.Services.V2Services;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDVariableText;

/**
 * One-shot normalization for uploaded AcroForm PDFs.
 *
 * <p>Rewrites the /DA (default appearance) on variable-text fields so every downstream renderer
 * (pdf.js in the client preview, pdfbox at fill time, pdf-lib at flatten time, and the browser's
 * native PDF viewer on print) sizes field text consistently. Explicit non-zero /DA sizes are
 * preserved; only ambiguous cases (size 0 / auto-fit, missing, or unparseable) are overwritten.
 *
 * <p>Also strips stale widget /AP appearance streams and flips /NeedAppearances so viewers
 * regenerate from the normalized /DA.
 *
 * <p>Constants intentionally mirror {@link FillPDFServiceV2} so template DA matches what the fill
 * pipeline later computes per field.
 */
public final class NormalizePdfFieldAppearancesService {

  private static final int DEFAULT_FONT_SIZE = 14;
  private static final int MIN_FONT_SIZE = 8;
  private static final float FIELD_HEIGHT_FONT_RATIO = 0.58f;

  private static final Pattern DA_SIZE_PATTERN =
      Pattern.compile("/\\S+\\s+([\\d.]+)\\s+Tf");

  private NormalizePdfFieldAppearancesService() {}

  /**
   * Returns normalized bytes, or the original bytes unchanged if the input has no AcroForm or
   * normalization fails for any reason. Best-effort: callers can use this as a pre-store pass
   * without fear of breaking uploads.
   */
  public static byte[] normalize(byte[] pdfBytes) {
    if (pdfBytes == null || pdfBytes.length == 0) {
      return pdfBytes;
    }
    try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
      PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
      if (acroForm == null) {
        return pdfBytes;
      }
      // Match the rest of the pipeline (FillPDFServiceV2, ParsePDFFieldsService): uploaded PDFs
      // may carry an encryption dict that would otherwise block save().
      doc.setAllSecurityToBeRemoved(true);
      boolean changed = normalizeFields(acroForm.getFields());
      if (!changed) {
        return pdfBytes;
      }
      acroForm.setNeedAppearances(true);
      // Defensive: setNeedAppearances already writes /NeedAppearances, but write it once more via
      // the COS dict so it survives even if an upstream pdfbox cleanup pass rewrites the object.
      acroForm.getCOSObject().setBoolean(COSName.NEED_APPEARANCES, true);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      doc.save(out);
      return out.toByteArray();
    } catch (Exception e) {
      return pdfBytes;
    }
  }

  private static boolean normalizeFields(List<PDField> fields) {
    boolean changed = false;
    for (PDField field : fields) {
      if (field instanceof PDNonTerminalField) {
        changed |= normalizeFields(((PDNonTerminalField) field).getChildren());
        continue;
      }
      if (!(field instanceof PDVariableText)) {
        continue;
      }
      PDVariableText variable = (PDVariableText) field;
      if (normalizeVariableTextField(variable)) {
        changed = true;
      }
    }
    return changed;
  }

  private static boolean normalizeVariableTextField(PDVariableText field) {
    String existingDa = field.getDefaultAppearance();
    float explicitSize = extractExplicitSize(existingDa);
    // Preserve authored typography: if the DA already has an explicit, non-zero size, trust it.
    if (explicitSize >= 1f) {
      return stripWidgetAppearances(field);
    }
    int resolvedPt = resolveFontSizeForSmallestWidget(field);
    field.setDefaultAppearance("/Helv " + resolvedPt + " Tf 0 g");
    stripWidgetAppearances(field);
    return true;
  }

  /** Returns the point size parsed from the DA's first `Tf` operator, or 0 if absent/invalid. */
  private static float extractExplicitSize(String da) {
    if (da == null || da.isEmpty()) {
      return 0f;
    }
    Matcher m = DA_SIZE_PATTERN.matcher(da);
    if (!m.find()) {
      return 0f;
    }
    try {
      return Float.parseFloat(m.group(1));
    } catch (NumberFormatException e) {
      return 0f;
    }
  }

  private static int resolveFontSizeForSmallestWidget(PDVariableText field) {
    int best = DEFAULT_FONT_SIZE;
    try {
      List<PDAnnotationWidget> widgets = field.getWidgets();
      if (widgets == null || widgets.isEmpty()) {
        return DEFAULT_FONT_SIZE;
      }
      for (PDAnnotationWidget widget : widgets) {
        PDRectangle rect = widget.getRectangle();
        if (rect == null) {
          continue;
        }
        float h = Math.abs(rect.getHeight());
        if (h <= 0f) {
          continue;
        }
        int cap = (int) Math.floor(h * FIELD_HEIGHT_FONT_RATIO);
        int pt = Math.max(MIN_FONT_SIZE, Math.min(DEFAULT_FONT_SIZE, cap));
        if (pt < best) {
          best = pt;
        }
      }
    } catch (Exception ignored) {
      return DEFAULT_FONT_SIZE;
    }
    return best;
  }

  /**
   * Removes existing appearance streams so viewers regenerate from the (freshly normalized) /DA.
   * Keeping stale /AP around causes legacy renderers to draw cached text at the old size.
   */
  private static boolean stripWidgetAppearances(PDVariableText field) {
    boolean changed = false;
    try {
      List<PDAnnotationWidget> widgets = field.getWidgets();
      if (widgets == null) {
        return false;
      }
      for (PDAnnotationWidget widget : widgets) {
        COSDictionary dict = widget.getCOSObject();
        if (dict.containsKey(COSName.AP)) {
          dict.removeItem(COSName.AP);
          changed = true;
        }
      }
    } catch (Exception ignored) {
      // Defensive: a malformed widget should not fail the whole pass.
    }
    return changed;
  }
}
