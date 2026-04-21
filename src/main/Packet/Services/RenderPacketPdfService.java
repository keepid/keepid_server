package Packet.Services;

import Database.File.FileDao;
import File.File;
import File.FileType;
import PDF.Services.V2Services.NormalizePdfFieldAppearancesService;
import Packet.Packet;
import Packet.PacketPart;
import Security.EncryptionController;
import Security.OrganizationCryptoAad;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

/**
 * Functional interface for decrypting a single part's encrypted byte stream. Split out so tests
 * can pass a passthrough implementation without having to construct a real
 * {@link EncryptionController} (which requires Tink + GCP KMS).
 */
@FunctionalInterface
interface PartDecryptor {
  InputStream decrypt(InputStream encryptedStream, String aad) throws Exception;
}

/**
 * Renders an application packet (base application PDF + enabled attachments) into a single
 * finalized PDF suitable for mailing, printing, or downloading.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Load each enabled {@link PacketPart} in order, decrypting with the correct AAD per-part
 *       (application PDFs are keyed by the target username; {@code ORG_DOCUMENT} attachments are
 *       keyed by the organization id).
 *   <li>Merge all parts into a single {@link PDDocument} via {@link PDFMergerUtility}.
 *   <li>Flatten any remaining AcroForm so downstream viewers (Lob's native renderer, browser
 *       print) have no live fields left to re-auto-size.
 *   <li>Run one final {@link NormalizePdfFieldAppearancesService#normalize} pass on the merged
 *       bytes as a belt-and-suspenders fix for fields that refused to flatten.
 * </ul>
 *
 * <p>When {@code packet} is {@code null} (legacy applications with no packet record yet), only
 * the base application PDF is rendered — behavior identical to the pre-packet flow.
 */
@Slf4j
public final class RenderPacketPdfService {

  private RenderPacketPdfService() {}

  /**
   * Renders the packet to flat PDF bytes.
   *
   * @param applicationFile base application PDF ({@code FileType.APPLICATION_PDF}); used as the
   *     sole part when {@code packet} is null and as the username AAD source for decrypt.
   * @param packet packet record, or {@code null} to treat as a single-part packet containing only
   *     the application PDF.
   * @param fileDao used to resolve each part's metadata and encrypted byte stream.
   * @param encryptionController used to decrypt each part's stream.
   * @param targetUsername AAD used to decrypt non-org-scoped parts (the application PDF itself
   *     and any user-owned parts). Typically {@code mail.getTargetUsername()}.
   * @return merged + flattened + normalized PDF bytes.
   * @throws IOException if any part cannot be loaded or decrypted.
   */
  public static byte[] render(
      File applicationFile,
      Packet packet,
      FileDao fileDao,
      EncryptionController encryptionController,
      String targetUsername)
      throws IOException {
    return render(applicationFile, packet, fileDao, encryptionController::decryptFile, targetUsername);
  }

  /**
   * Test seam: same as {@link #render(File, Packet, FileDao, EncryptionController, String)} but
   * accepts a lambda for decryption so unit tests can pass passthrough/plaintext fakes.
   */
  static byte[] render(
      File applicationFile,
      Packet packet,
      FileDao fileDao,
      PartDecryptor decryptor,
      String targetUsername)
      throws IOException {
    if (applicationFile == null) {
      throw new IllegalArgumentException("applicationFile must not be null");
    }

    List<PacketPart> orderedParts = resolveOrderedParts(applicationFile, packet);
    List<PDDocument> openDocs = new ArrayList<>(orderedParts.size());
    try (PDDocument merged = new PDDocument()) {
      PDFMergerUtility merger = new PDFMergerUtility();

      for (PacketPart part : orderedParts) {
        File partFile = fileDao.get(part.getFileId()).orElse(null);
        if (partFile == null) {
          log.warn(
              "Skipping missing packet part fileId={} for application {}",
              part.getFileId(),
              applicationFile.getId());
          continue;
        }
        byte[] partBytes = decryptPartBytes(partFile, fileDao, decryptor, targetUsername);
        PDDocument partDoc = Loader.loadPDF(partBytes);
        partDoc.setAllSecurityToBeRemoved(true);
        openDocs.add(partDoc);
        merger.appendDocument(merged, partDoc);
      }

      flattenAcroFormQuietly(merged);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      merged.save(out);
      byte[] mergedBytes = out.toByteArray();
      return NormalizePdfFieldAppearancesService.normalize(mergedBytes);
    } finally {
      for (PDDocument doc : openDocs) {
        try {
          doc.close();
        } catch (IOException ignored) {
          // Best-effort: the merged copy already holds everything we need.
        }
      }
    }
  }

  /** Quick page-count helper that does not pay the cost of a second merge pass. */
  public static int countPages(byte[] pdfBytes) throws IOException {
    try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
      return doc.getNumberOfPages();
    }
  }

  private static List<PacketPart> resolveOrderedParts(File applicationFile, Packet packet) {
    if (packet == null || packet.getParts() == null || packet.getParts().isEmpty()) {
      List<PacketPart> implicit = new ArrayList<>(1);
      implicit.add(new PacketPart(applicationFile.getId(), "APPLICATION_BASE", 0, true));
      return implicit;
    }
    List<PacketPart> enabled = new ArrayList<>();
    for (PacketPart part : packet.getParts()) {
      if (part != null && part.isEnabled() && part.getFileId() != null) {
        enabled.add(part);
      }
    }
    enabled.sort(Comparator.comparingInt(PacketPart::getOrder));
    if (enabled.isEmpty()) {
      // Defensive: a packet record with every part disabled still needs to send _something_.
      // Fall back to the base application so we never mail an empty envelope.
      enabled.add(new PacketPart(applicationFile.getId(), "APPLICATION_BASE", 0, true));
    }
    return enabled;
  }

  private static byte[] decryptPartBytes(
      File partFile, FileDao fileDao, PartDecryptor decryptor, String targetUsername)
      throws IOException {
    String aad = aadForPart(partFile, targetUsername);
    InputStream encryptedStream =
        fileDao
            .getStream(partFile.getId())
            .orElseThrow(() -> new IOException("Missing stream for file " + partFile.getId()));
    try {
      InputStream decrypted = decryptor.decrypt(encryptedStream, aad);
      return IOUtils.toByteArray(decrypted);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Failed to decrypt packet part " + partFile.getId(), e);
    }
  }

  /**
   * Matches the AAD chosen by {@code UploadFileService.uploadFile}: org-scoped file types
   * ({@code ORG_DOCUMENT}, {@code FORM}) use the organization id; everything else (application
   * PDFs, identification) uses the owning username.
   */
  private static String aadForPart(File partFile, String fallbackUsername) {
    FileType type = partFile.getFileType();
    if ((type == FileType.ORG_DOCUMENT || type == FileType.FORM)
        && partFile.getOrganizationId() != null) {
      return OrganizationCryptoAad.fromOrganizationId(partFile.getOrganizationId());
    }
    // Fall back to the file's own username when present (e.g. application PDF owned by the
    // target client) to keep behavior correct even if the caller passed a different username.
    if (partFile.getUsername() != null && !partFile.getUsername().isEmpty()) {
      return partFile.getUsername();
    }
    return fallbackUsername;
  }

  /**
   * Flattens the merged document's AcroForm when one exists. Best-effort: pdfbox flatten can
   * throw on malformed widgets and we'd rather mail a non-flattened (but still valid) PDF than
   * abort the whole send.
   */
  private static void flattenAcroFormQuietly(PDDocument merged) {
    try {
      PDAcroForm form = merged.getDocumentCatalog().getAcroForm();
      if (form == null) {
        return;
      }
      // Pdfbox's flatten() regenerates appearance streams from each field's /DA; refreshing first
      // ensures the regenerated pixels match the normalized sizes we set at upload time.
      form.refreshAppearances();
      form.flatten();
    } catch (Exception e) {
      log.warn("AcroForm flatten failed during packet render; mailing unflattened: {}", e.getMessage());
    }
  }
}
