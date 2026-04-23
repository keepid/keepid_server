package MailTest;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import Mail.Services.SubmitToLobMailService;
import org.junit.Test;

/**
 * Verifies the page-limit gate that protects us from Lob's hard API limits (letters 1–60 pages
 * single-sided; checks 1–6 pages). The logic is deliberately exposed as a small static method on
 * {@link SubmitToLobMailService} so we can exercise the full threshold matrix without constructing
 * a real Tink-backed EncryptionController.
 */
public class SubmitToLobMailServicePageLimitUnitTests {

  @Test
  public void letter_withinLimits_noThrow() {
    SubmitToLobMailService.assertWithinPageLimit(1, /*isCheck=*/ false);
    SubmitToLobMailService.assertWithinPageLimit(30, false);
    SubmitToLobMailService.assertWithinPageLimit(
        SubmitToLobMailService.LOB_LETTER_MAX_PAGES, false);
  }

  @Test
  public void check_withinLimits_noThrow() {
    SubmitToLobMailService.assertWithinPageLimit(1, /*isCheck=*/ true);
    SubmitToLobMailService.assertWithinPageLimit(
        SubmitToLobMailService.LOB_CHECK_MAX_PAGES, true);
  }

  @Test
  public void letter_exceedsLimit_throwsWithProductName() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () ->
                SubmitToLobMailService.assertWithinPageLimit(
                    SubmitToLobMailService.LOB_LETTER_MAX_PAGES + 1, /*isCheck=*/ false));
    String msg = ex.getMessage();
    assertTrue("Error should mention 'letter': " + msg, msg.toLowerCase().contains("letter"));
    assertTrue("Error should mention the page count: " + msg, msg.contains("61"));
  }

  @Test
  public void check_exceedsLimit_throwsWithProductName() {
    // A 10-page packet on a check product is the canonical "letter-sized packet, check product"
    // mistake that used to silently fail at Lob with an opaque 422.
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> SubmitToLobMailService.assertWithinPageLimit(10, /*isCheck=*/ true));
    String msg = ex.getMessage();
    assertTrue("Error should mention 'check': " + msg, msg.toLowerCase().contains("check"));
    assertTrue("Error should mention the page count: " + msg, msg.contains("10"));
  }

  @Test
  public void zeroPages_throwsEvenForLetter() {
    // Defensive: render failure that produced a 0-page output should not silently reach Lob.
    assertThrows(
        IllegalStateException.class,
        () -> SubmitToLobMailService.assertWithinPageLimit(0, /*isCheck=*/ false));
  }
}
