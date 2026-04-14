package SecurityTest;

import Security.URIUtil;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class URIUtilTest {

  @Test
  public void microsoftRedirectUriAcceptsKnownHosts() {
    assertTrue(URIUtil.isValidMicrosoftRedirectURI("http://localhost:7001/microsoftLoginResponse"));
    assertTrue(URIUtil.isValidMicrosoftRedirectURI("https://staged.keep.id/microsoftLoginResponse"));
    assertTrue(URIUtil.isValidMicrosoftRedirectURI("https://server.keep.id/microsoftLoginResponse"));
  }

  @Test
  public void microsoftRedirectUriRejectsUnknownHostOrPath() {
    assertFalse(URIUtil.isValidMicrosoftRedirectURI("https://keep.id/microsoftLoginResponse"));
    assertFalse(URIUtil.isValidMicrosoftRedirectURI("http://localhost:7001/googleLoginResponse"));
  }

  @Test
  public void originUriAcceptsLocalAndHostedFrontends() {
    assertTrue(URIUtil.isValidOriginURI("http://localhost:3000"));
    assertTrue(URIUtil.isValidOriginURI("http://localhost:5173"));
    assertTrue(URIUtil.isValidOriginURI("https://keep.id"));
    assertTrue(URIUtil.isValidOriginURI("https://staged.keep.id"));
  }
}
