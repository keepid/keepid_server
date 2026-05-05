package PhoneUploadTest;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import PhoneUpload.PhoneUploadTokenUtil;
import org.junit.Test;

public class PhoneUploadTokenUtilTest {
  @Test
  public void generateRawTokenProducesDistinctValues() {
    String tokenA = PhoneUploadTokenUtil.generateRawToken();
    String tokenB = PhoneUploadTokenUtil.generateRawToken();
    assertNotEquals(tokenA, tokenB);
    assertTrue(tokenA.length() > 20);
    assertTrue(tokenB.length() > 20);
  }

  @Test
  public void hashTokenIsStableForSameInput() {
    String token = "sample-token-value";
    String hashA = PhoneUploadTokenUtil.hashToken(token);
    String hashB = PhoneUploadTokenUtil.hashToken(token);
    assertTrue(hashA.equals(hashB));
    assertTrue(hashA.length() > 20);
  }
}
