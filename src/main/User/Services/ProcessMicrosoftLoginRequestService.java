package User.Services;

import Config.Message;
import Config.Service;
import Security.CSRFUtil;
import Security.PKCEUtil;
import Security.URIUtil;
import User.MicrosoftLoginRequestMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class ProcessMicrosoftLoginRequestService implements Service {
  private String codeVerifier;
  private String codeChallenge;
  private String csrfToken;
  private final String redirectURI;
  private final String originURI;

  public ProcessMicrosoftLoginRequestService(String redirectURI, String originURI) {
    this.redirectURI = redirectURI;
    this.originURI = originURI;
  }

  public Message executeAndGetResponse() {
    if (redirectURI == null || !URIUtil.isValidMicrosoftRedirectURI(redirectURI)) {
      log.warn("Invalid Redirect URI provided: {}", redirectURI);
      return MicrosoftLoginRequestMessage.INVALID_REDIRECT_URI;
    }
    if (originURI == null || !URIUtil.isValidOriginURI(originURI)) {
      log.warn("Invalid Origin URI provided: {}", originURI);
      return MicrosoftLoginRequestMessage.INVALID_ORIGIN_URI;
    }
    try {
      csrfToken = CSRFUtil.generateCSRFToken();
      codeVerifier = PKCEUtil.generateCodeVerifier();
      codeChallenge = PKCEUtil.generateCodeChallange(codeVerifier);
      return MicrosoftLoginRequestMessage.REQUEST_SUCCESS;
    } catch (Exception e) {
      log.error("Error processing Microsoft Login Request: {}", e.getMessage());
    }
    return MicrosoftLoginRequestMessage.INTERNAL_ERROR;
  }
}
