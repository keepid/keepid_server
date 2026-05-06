package PhoneUpload;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class PhoneUploadTokenUtil {
  private static final int TOKEN_BYTES = 32;

  private PhoneUploadTokenUtil() {}

  public static String generateRawToken() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] tokenBytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }

  public static String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
