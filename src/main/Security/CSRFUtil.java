package Security;
import java.security.SecureRandom;
import java.util.Base64;

public class CSRFUtil {
  public static String generateCSRFToken() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] tokenBytes = new byte[32];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }
}
