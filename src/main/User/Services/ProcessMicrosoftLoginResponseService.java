package User.Services;

import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.User.UserDao;
import Security.URIUtil;
import User.MicrosoftLoginResponseMessage;
import User.User;
import User.UserType;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Slf4j
public class ProcessMicrosoftLoginResponseService implements Service {
  private static final String MICROSOFT_CONSUMER_TENANT_ID = "9188040d-6c67-4c5b-b112-36a304b66dad";

  private final String ipInfoToken = System.getenv("IPINFO_TOKEN");
  private final String microsoftClientId = System.getenv("MICROSOFT_CLIENT_ID");
  private final String microsoftClientSecret = System.getenv("MICROSOFT_CLIENT_SECRET");
  private final String microsoftTenantId = System.getenv("MICROSOFT_TENANT_ID");

  private final UserDao userDao;
  private final ActivityDao activityDao;
  private String origin;
  private final String csrfToken;
  private final String storedCsrfToken;
  private final String authCode;
  private final String codeVerifier;
  private final String redirectUri;
  private final String ip;
  private final String userAgent;
  private User user;

  private String microsoftEmail;
  private String microsoftFirstName;
  private String microsoftLastName;

  public ProcessMicrosoftLoginResponseService(
      UserDao userDao,
      ActivityDao activityDao,
      String state,
      String storedCsrfToken,
      String authCode,
      String codeVerifier,
      String originUri,
      String redirectUri,
      String ip,
      String userAgent) {
    this.userDao = userDao;
    this.activityDao = activityDao;
    this.csrfToken = state;
    this.origin = originUri;
    this.storedCsrfToken = storedCsrfToken;
    this.authCode = authCode;
    this.codeVerifier = codeVerifier;
    this.redirectUri = redirectUri;
    this.ip = ip;
    this.userAgent = userAgent;
  }

  @Override
  public Message executeAndGetResponse() {
    if (microsoftClientId == null || microsoftClientSecret == null || microsoftTenantId == null) {
      log.error(
          "Microsoft OAuth environment variables are not configured. "
              + "Please set MICROSOFT_CLIENT_ID, MICROSOFT_CLIENT_SECRET, and MICROSOFT_TENANT_ID.");
      return MicrosoftLoginResponseMessage.INTERNAL_ERROR;
    }
    if (ipInfoToken == null) {
      log.warn("IPINFO_TOKEN environment variable is not set. IP geolocation will be unavailable.");
    }
    if (origin == null || !URIUtil.isValidOriginURI(origin)) {
      log.error("Invalid Origin URI provided.");
      origin = "https://keep.id";
      return MicrosoftLoginResponseMessage.AUTH_FAILURE;
    }
    if (redirectUri == null || !URIUtil.isValidMicrosoftRedirectURI(redirectUri)) {
      log.error("Invalid Redirect URI provided.");
      return MicrosoftLoginResponseMessage.AUTH_FAILURE;
    }
    if (csrfToken == null || !csrfToken.equals(storedCsrfToken)) {
      log.error("Invalid CSRF Token provided.");
      return MicrosoftLoginResponseMessage.AUTH_FAILURE;
    }
    try {
      String rawIdToken = exchangeAuthCodeForIDToken();
      if (rawIdToken == null) {
        log.error("No Microsoft ID token returned by token exchange");
        return MicrosoftLoginResponseMessage.AUTH_FAILURE;
      }
      DecodedJWT idToken = JWT.decode(rawIdToken);
      if (!verifyIdToken(rawIdToken, idToken)) {
        log.error("Microsoft ID token verification failed");
        return MicrosoftLoginResponseMessage.AUTH_FAILURE;
      }

      microsoftEmail = extractEmail(idToken);
      microsoftFirstName = claimAsString(idToken.getClaim("given_name"));
      microsoftLastName = claimAsString(idToken.getClaim("family_name"));

      if (microsoftEmail == null || microsoftEmail.isBlank()) {
        log.error("Microsoft ID token did not contain a usable email claim");
        return MicrosoftLoginResponseMessage.AUTH_FAILURE;
      }

      Optional<User> userOptional = userDao.getByEmail(microsoftEmail.toLowerCase(Locale.ROOT));
      if (userOptional.isEmpty()) {
        log.info("No Keep.id account associated with Microsoft email: {}", microsoftEmail);
        return MicrosoftLoginResponseMessage.USER_NOT_FOUND;
      }
      user = userOptional.get();
      LoginService.recordActivityLogin(user, activityDao);
      LoginService.recordToLoginHistory(user, ip, userAgent, ipInfoToken, userDao);
      return MicrosoftLoginResponseMessage.AUTH_SUCCESS;
    } catch (Exception e) {
      log.error("Error processing Microsoft login response", e);
      return MicrosoftLoginResponseMessage.INTERNAL_ERROR;
    }
  }

  private String exchangeAuthCodeForIDToken() throws Exception {
    String tokenExchangeUrl =
        "https://login.microsoftonline.com/" + microsoftTenantId + "/oauth2/v2.0/token";

    String body =
        "client_id="
            + urlEncode(microsoftClientId)
            + "&client_secret="
            + urlEncode(microsoftClientSecret)
            + "&code="
            + urlEncode(authCode)
            + "&code_verifier="
            + urlEncode(codeVerifier)
            + "&grant_type=authorization_code"
            + "&redirect_uri="
            + urlEncode(redirectUri)
            + "&scope="
            + urlEncode("openid profile email");

    HttpClient client =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(tokenExchangeUrl))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    JSONObject responseJSON = new JSONObject(response.body());

    if (!responseJSON.has("id_token")) {
      String error = responseJSON.optString("error", "unknown");
      String errorDescription = responseJSON.optString("error_description", "no description");
      log.error(
          "Microsoft token exchange failed. HTTP status: {}, error: '{}', description: '{}'.",
          response.statusCode(),
          error,
          errorDescription);
      return null;
    }

    return responseJSON.getString("id_token");
  }

  private boolean verifyIdToken(String rawIdToken, DecodedJWT idToken) {
    if (!verifyIdTokenSignature(rawIdToken, idToken)) {
      log.error("Invalid Microsoft ID token signature");
      return false;
    }
    if (!hasValidAudience(idToken)) {
      log.error("Invalid audience in Microsoft ID token");
      return false;
    }
    if (!hasValidTenantAndIssuer(idToken)) {
      log.error("Invalid tenant or issuer in Microsoft ID token");
      return false;
    }

    var expiresAt = idToken.getExpiresAt();
    if (expiresAt == null || expiresAt.getTime() < System.currentTimeMillis()) {
      log.error("Microsoft ID token has expired");
      return false;
    }

    return true;
  }

  private boolean verifyIdTokenSignature(String rawIdToken, DecodedJWT idToken) {
    try {
      String keyId = idToken.getKeyId();
      if (keyId == null || keyId.isBlank()) {
        log.error("Microsoft ID token missing kid header");
        return false;
      }
      RSAPublicKey publicKey = loadSigningKeyByKid(keyId);
      if (publicKey == null) {
        log.error("No Microsoft signing key found for kid {}", keyId);
        return false;
      }
      Algorithm algorithm = Algorithm.RSA256(publicKey, null);
      JWTVerifier verifier = JWT.require(algorithm).build();
      verifier.verify(rawIdToken);
      return true;
    } catch (Exception e) {
      log.error("Microsoft ID token signature verification failed", e);
      return false;
    }
  }

  private RSAPublicKey loadSigningKeyByKid(String keyId) throws Exception {
    String keysUrl =
        "https://login.microsoftonline.com/" + microsoftTenantId + "/discovery/v2.0/keys";
    HttpClient client =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(keysUrl))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    JSONObject jwks = new JSONObject(response.body());
    var keys = jwks.optJSONArray("keys");
    if (keys == null) {
      return null;
    }
    for (int i = 0; i < keys.length(); i++) {
      JSONObject key = keys.getJSONObject(i);
      if (!keyId.equals(key.optString("kid"))) {
        continue;
      }
      String modulus = key.optString("n");
      String exponent = key.optString("e");
      if (modulus == null || modulus.isBlank() || exponent == null || exponent.isBlank()) {
        return null;
      }
      byte[] modulusBytes = Base64.getUrlDecoder().decode(modulus);
      byte[] exponentBytes = Base64.getUrlDecoder().decode(exponent);
      BigInteger modulusBigInt = new BigInteger(1, modulusBytes);
      BigInteger exponentBigInt = new BigInteger(1, exponentBytes);
      RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulusBigInt, exponentBigInt);
      return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }
    return null;
  }

  private boolean hasValidTenantAndIssuer(DecodedJWT idToken) {
    String tokenTenantId = claimAsString(idToken.getClaim("tid"));
    if (tokenTenantId == null || tokenTenantId.isBlank()) {
      log.error("Microsoft ID token missing tid claim");
      return false;
    }

    String configuredTenant = microsoftTenantId.toLowerCase(Locale.ROOT);
    if (isSpecialTenantAlias(configuredTenant)) {
      if ("organizations".equals(configuredTenant)
          && MICROSOFT_CONSUMER_TENANT_ID.equalsIgnoreCase(tokenTenantId)) {
        log.error("Consumer tenant token is not allowed for organizations-only login");
        return false;
      }
    } else if (!microsoftTenantId.equalsIgnoreCase(tokenTenantId)) {
      log.error(
          "Tenant mismatch in Microsoft ID token. expected={}, actual={}",
          microsoftTenantId,
          tokenTenantId);
      return false;
    }

    String expectedTenantForIssuer =
        isSpecialTenantAlias(configuredTenant) ? tokenTenantId : microsoftTenantId;
    String expectedIssuer =
        "https://login.microsoftonline.com/" + expectedTenantForIssuer + "/v2.0";
    String actualIssuer = idToken.getIssuer();
    if (actualIssuer == null || !expectedIssuer.equalsIgnoreCase(actualIssuer)) {
      log.error(
          "Issuer mismatch in Microsoft ID token. expected={}, actual={}",
          expectedIssuer,
          actualIssuer);
      return false;
    }
    return true;
  }

  private boolean isSpecialTenantAlias(String tenantId) {
    return "common".equals(tenantId)
        || "organizations".equals(tenantId)
        || "consumers".equals(tenantId);
  }

  private boolean hasValidAudience(DecodedJWT idToken) {
    List<String> audience = idToken.getAudience();
    return audience != null && audience.contains(microsoftClientId);
  }

  private String extractEmail(DecodedJWT idToken) {
    String email = claimAsString(idToken.getClaim("email"));
    if (email != null && !email.isBlank()) {
      return email;
    }
    String preferredUsername = claimAsString(idToken.getClaim("preferred_username"));
    if (preferredUsername != null && preferredUsername.contains("@")) {
      return preferredUsername;
    }
    return null;
  }

  private String claimAsString(Claim claim) {
    if (claim == null || claim.isNull()) {
      return null;
    }
    return claim.asString();
  }

  private String urlEncode(String value) {
    return URLEncoder.encode(Objects.toString(value, ""), StandardCharsets.UTF_8);
  }

  public String getOrigin() {
    Objects.requireNonNull(origin);
    return origin;
  }

  public UserType getUserRole() {
    Objects.requireNonNull(user);
    return user.getUserType();
  }

  public String getOrganization() {
    Objects.requireNonNull(user);
    return user.getOrganization();
  }

  public String getUsername() {
    Objects.requireNonNull(user);
    return user.getUsername();
  }

  public String getFullName() {
    Objects.requireNonNull(user);
    return user.getCurrentName() != null ? user.getCurrentName().getFullName() : "";
  }

  public String getMicrosoftEmail() {
    return microsoftEmail;
  }

  public String getMicrosoftFirstName() {
    return microsoftFirstName;
  }

  public String getMicrosoftLastName() {
    return microsoftLastName;
  }
}
