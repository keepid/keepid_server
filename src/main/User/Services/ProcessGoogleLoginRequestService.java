package User.Services;

import Config.Message;
import Config.Service;
import Security.CSRFUtil;
import Security.PKCEUtil;
import Security.URIUtil;
import User.GoogleLoginRequestMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class ProcessGoogleLoginRequestService implements Service {
  private String codeVerifier;
  private String codeChallenge;
  private String csrfToken;
  private final String redirectURI;
  private final String originURI;

  public ProcessGoogleLoginRequestService(String redirectURI, String originURI) {
    this.redirectURI = redirectURI;
    this.originURI = originURI;
  }

  public Message executeAndGetResponse() {
    if (redirectURI == null || !URIUtil.isValidRedirectURI(redirectURI)) {
      log.warn("Invalid Redirect URI provided: {}", redirectURI);
      return GoogleLoginRequestMessage.INVALID_REDIRECT_URI;
    }
    if (originURI == null || !URIUtil.isValidOriginURI(originURI)) {
      log.warn("Invalid Origin URI provided: {}", originURI);
      return GoogleLoginRequestMessage.INVALID_ORIGIN_URI;
    }
    try {
      csrfToken = CSRFUtil.generateCSRFToken();
      log.debug("Generated CSRF Token: {}", csrfToken);

      codeVerifier = PKCEUtil.generateCodeVerifier();
      log.debug("Generated Code Verifier: {}", codeVerifier);

      codeChallenge = PKCEUtil.generateCodeChallange(codeVerifier);
      log.debug("Generated Code Challenge: {}", codeChallenge);
      return GoogleLoginRequestMessage.REQUEST_SUCCESS;
    } catch (Exception e) {
      log.error("Error processing Google Login Request: {}", e.getMessage());
    }
    return GoogleLoginRequestMessage.INTERNAL_ERROR;
  }
}
