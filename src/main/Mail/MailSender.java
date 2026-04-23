package Mail;

public interface MailSender {
  /**
   * Sends a mail piece (letter or check) to the transport provider.
   *
   * @param mail mail record (target, requester, addressing metadata). The {@code mail} object
   *     is the sole source of Lob-facing metadata; the actual PDF payload is supplied separately
   *     so that callers (service layer) can pre-render multi-part packets.
   * @param renderedPdfBytes fully merged, flattened PDF bytes to mail. Decryption, part merging,
   *     appearance normalization, and flattening are all done upstream (see
   *     {@code RenderPacketPdfService}).
   * @param returnAddress optional return address override; {@code null} falls back to the
   *     provider's default.
   */
  MailResult sendMail(Mail mail, byte[] renderedPdfBytes, ReturnAddress returnAddress)
      throws Exception;

  MailResult refreshStatus(String lobId, boolean isCheck) throws Exception;
}
