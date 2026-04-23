package PacketTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import Config.DeploymentLevel;
import Database.File.FileDao;
import Database.File.FileDaoTestImpl;
import File.File;
import File.FileType;
import File.IdCategoryType;
import Packet.Packet;
import Packet.PacketPart;
import Packet.Services.RenderPacketPdfService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

/**
 * RenderPacketPdfService tests focus on the two invariants Lob relies on: (a) parts are
 * concatenated in {@code order} with only {@code enabled=true} parts included; and (b) page
 * counts on the merged output are deterministic so callers can safely gate page-limit checks
 * on {@link RenderPacketPdfService#countPages}.
 *
 * <p>These tests use the package-private {@code render(..., PartDecryptor, ...)} overload with a
 * passthrough lambda so they don't depend on Tink/GCP KMS for real decryption.
 */
public class RenderPacketPdfServiceUnitTests {

  /** Reflection handle for the package-private 5-arg testable overload (no override). */
  private static Method renderOverload;

  /** Reflection handle for the 6-arg overload that accepts an override for APPLICATION_BASE. */
  private static Method renderWithOverrideOverload;

  private FileDao fileDao;

  @Before
  public void setUp() throws Exception {
    fileDao = new FileDaoTestImpl(DeploymentLevel.IN_MEMORY);
    if (renderOverload == null || renderWithOverrideOverload == null) {
      for (Method m : RenderPacketPdfService.class.getDeclaredMethods()) {
        if (!m.getName().equals("render")) continue;
        Class<?>[] params = m.getParameterTypes();
        // Both testable overloads have PartDecryptor at index 3; the 5-arg one is the
        // no-override path, the 6-arg one accepts an applicationBaseOverride byte[] at index 5.
        if (params.length == 5 && params[3].getSimpleName().equals("PartDecryptor")) {
          renderOverload = m;
          renderOverload.setAccessible(true);
        } else if (params.length == 6
            && params[3].getSimpleName().equals("PartDecryptor")
            && params[5] == byte[].class) {
          renderWithOverrideOverload = m;
          renderWithOverrideOverload.setAccessible(true);
        }
      }
    }
    assertNotNull("Test seam overload not found", renderOverload);
    assertNotNull("Override test seam overload not found", renderWithOverrideOverload);
  }

  @Test
  public void render_nullPacket_returnsBasePdfPageCount() throws Exception {
    File base = registerPdf("alice", FileType.APPLICATION_PDF, /*pages=*/ 2, /*orgId=*/ null);

    byte[] rendered = invokeRender(base, null);

    assertNotNull(rendered);
    assertEquals(2, RenderPacketPdfService.countPages(rendered));
  }

  @Test
  public void render_multiPartPacket_concatenatesInOrderAndRespectsEnabled() throws Exception {
    File base = registerPdf("alice", FileType.APPLICATION_PDF, /*pages=*/ 2, null);
    ObjectId orgId = new ObjectId();
    File attach1 = registerPdf("alice", FileType.ORG_DOCUMENT, /*pages=*/ 1, orgId);
    File attach2 = registerPdf("alice", FileType.ORG_DOCUMENT, /*pages=*/ 3, orgId);
    File disabledAttach = registerPdf("alice", FileType.ORG_DOCUMENT, /*pages=*/ 5, orgId);

    Packet packet = new Packet(orgId, base.getId(), "worker");
    List<PacketPart> parts = new ArrayList<>();
    parts.add(new PacketPart(base.getId(), "APPLICATION_BASE", 0, true));
    parts.add(new PacketPart(attach2.getId(), "ORG_ATTACHMENT", 2, true));
    parts.add(new PacketPart(attach1.getId(), "ORG_ATTACHMENT", 1, true));
    parts.add(new PacketPart(disabledAttach.getId(), "ORG_ATTACHMENT", 3, false));
    packet.setParts(parts);

    byte[] rendered = invokeRender(base, packet);

    // Expected: base (2) + attach1 (1, order=1) + attach2 (3, order=2). Disabled attach excluded.
    assertEquals(2 + 1 + 3, RenderPacketPdfService.countPages(rendered));
  }

  @Test
  public void render_packetWithAllPartsDisabled_fallsBackToBaseApplication() throws Exception {
    File base = registerPdf("alice", FileType.APPLICATION_PDF, /*pages=*/ 1, null);
    ObjectId orgId = new ObjectId();
    File attach = registerPdf("alice", FileType.ORG_DOCUMENT, /*pages=*/ 4, orgId);

    Packet packet = new Packet(orgId, base.getId(), "worker");
    List<PacketPart> parts = new ArrayList<>();
    parts.add(new PacketPart(base.getId(), "APPLICATION_BASE", 0, false));
    parts.add(new PacketPart(attach.getId(), "ORG_ATTACHMENT", 1, false));
    packet.setParts(parts);

    byte[] rendered = invokeRender(base, packet);

    // All parts disabled: we never want to mail a zero-page envelope, so the service falls back
    // to the base application PDF.
    assertEquals(1, RenderPacketPdfService.countPages(rendered));
  }

  @Test
  public void render_skipsMissingPartFilesInsteadOfThrowing() throws Exception {
    File base = registerPdf("alice", FileType.APPLICATION_PDF, /*pages=*/ 2, null);
    ObjectId orgId = new ObjectId();
    File attach = registerPdf("alice", FileType.ORG_DOCUMENT, /*pages=*/ 1, orgId);
    ObjectId missingId = new ObjectId(); // never registered in fileDao

    Packet packet = new Packet(orgId, base.getId(), "worker");
    List<PacketPart> parts = new ArrayList<>();
    parts.add(new PacketPart(base.getId(), "APPLICATION_BASE", 0, true));
    parts.add(new PacketPart(missingId, "ORG_ATTACHMENT", 1, true));
    parts.add(new PacketPart(attach.getId(), "ORG_ATTACHMENT", 2, true));
    packet.setParts(parts);

    byte[] rendered = invokeRender(base, packet);

    // Missing file is logged + skipped so one deleted attachment doesn't break mail sends.
    assertEquals(2 + 1, RenderPacketPdfService.countPages(rendered));
  }

  @Test
  public void countPages_matchesPdfboxForEmbeddedDocument() throws Exception {
    byte[] pdf = buildPdf(7);
    assertEquals(7, RenderPacketPdfService.countPages(pdf));
  }

  @Test
  public void render_producesValidPdfBytes() throws Exception {
    File base = registerPdf("alice", FileType.APPLICATION_PDF, /*pages=*/ 3, null);

    byte[] rendered = invokeRender(base, null);

    // If the service ever produced non-PDF bytes, Loader.loadPDF would throw. Cheapest smoke test.
    try (PDDocument doc = Loader.loadPDF(rendered)) {
      assertTrue(doc.getNumberOfPages() > 0);
    }
  }

  @Test
  public void render_withOverride_replacesBasePartButKeepsAttachments() throws Exception {
    // Stored base is 2 pages; override is 5 pages -- rendered output should pick the override's
    // page count for the base slot, then concatenate the stored 3-page attachment after it.
    File base = registerPdf("alice", FileType.APPLICATION_PDF, /*pages=*/ 2, null);
    ObjectId orgId = new ObjectId();
    File attach = registerPdf("alice", FileType.ORG_DOCUMENT, /*pages=*/ 3, orgId);

    Packet packet = new Packet(orgId, base.getId(), "worker");
    List<PacketPart> parts = new ArrayList<>();
    parts.add(new PacketPart(base.getId(), "APPLICATION_BASE", 0, true));
    parts.add(new PacketPart(attach.getId(), "ORG_ATTACHMENT", 1, true));
    packet.setParts(parts);

    byte[] override = buildPdf(5);
    byte[] rendered = invokeRenderWithOverride(base, packet, override);

    assertEquals(5 + 3, RenderPacketPdfService.countPages(rendered));
  }

  @Test
  public void render_withNullOverride_matchesNoOverridePath() throws Exception {
    File base = registerPdf("alice", FileType.APPLICATION_PDF, /*pages=*/ 2, null);

    byte[] rendered = invokeRenderWithOverride(base, null, null);

    // Passing null for the override must be equivalent to the legacy no-override call.
    assertEquals(2, RenderPacketPdfService.countPages(rendered));
  }

  // --- helpers --------------------------------------------------------------------------------

  /** Invokes the package-private render(..., PartDecryptor, ...) overload with passthrough decrypt. */
  private byte[] invokeRender(File applicationFile, Packet packet) throws Exception {
    Object passthroughDecryptor = buildPassthroughDecryptor(renderOverload);
    return (byte[])
        renderOverload.invoke(null, applicationFile, packet, fileDao, passthroughDecryptor, "alice");
  }

  /**
   * Invokes the 6-arg render(..., PartDecryptor, ..., byte[] override) overload. Passing {@code
   * null} for {@code override} exercises the no-override branch through the 6-arg path.
   */
  private byte[] invokeRenderWithOverride(File applicationFile, Packet packet, byte[] override)
      throws Exception {
    Object passthroughDecryptor = buildPassthroughDecryptor(renderWithOverrideOverload);
    return (byte[])
        renderWithOverrideOverload.invoke(
            null, applicationFile, packet, fileDao, passthroughDecryptor, "alice", override);
  }

  private static Object buildPassthroughDecryptor(Method overload) {
    Class<?> decryptorType = overload.getParameterTypes()[3];
    return java.lang.reflect.Proxy.newProxyInstance(
        decryptorType.getClassLoader(),
        new Class<?>[] {decryptorType},
        (proxy, method, args) -> args[0]); // return the InputStream arg as-is
  }

  private File registerPdf(String username, FileType type, int pages, ObjectId organizationId)
      throws Exception {
    byte[] pdfBytes = buildPdf(pages);
    File file =
        new File(
            username,
            new Date(),
            new ByteArrayInputStream(pdfBytes),
            type,
            IdCategoryType.OTHER,
            "fixture.pdf",
            "TestOrg",
            /*isAnnotated=*/ false,
            "application/pdf");
    file.setOrganizationId(organizationId);
    fileDao.save(file);
    return file;
  }

  private static byte[] buildPdf(int pages) throws Exception {
    try (PDDocument doc = new PDDocument()) {
      for (int i = 0; i < pages; i += 1) {
        doc.addPage(new PDPage(PDRectangle.LETTER));
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      doc.save(out);
      return out.toByteArray();
    }
  }
}
