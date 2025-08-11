package User.Services;

import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.User.UserDao;
import Security.URIUtil;
import User.User;
import User.GoogleLoginResponseMessage;
import User.UserType;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
public class ProcessGoogleLoginResponseService implements Service {
    public final String IP_INFO_TOKEN = Objects.requireNonNull(System.getenv("IPINFO_TOKEN"));
    private final String googleClientId = Objects.requireNonNull(
        System.getenv("GOOGLE_CLIENT_ID"));
    private final String googleClientSecret = Objects.requireNonNull(
        System.getenv("GOOGLE_CLIENT_SECRET"));
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

    public ProcessGoogleLoginResponseService(
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
    public Message executeAndGetResponse() throws Exception {
        if (origin == null || !URIUtil.isValidOriginURI(origin)) {
            log.error("Invalid Origin URI provided.");
            origin = "https://keep.id";
            return GoogleLoginResponseMessage.AUTH_FAILURE;
        }
        if (redirectUri == null || !URIUtil.isValidRedirectURI(redirectUri)) {
            log.error("Invalid Redirect URI provided.");
            return GoogleLoginResponseMessage.AUTH_FAILURE;
        }
        if (csrfToken == null || !csrfToken.equals(storedCsrfToken)) {
            log.error("Invalid CSRF Token provided.");
            return GoogleLoginResponseMessage.AUTH_FAILURE;
        }
        try {
            log.info("Attempting to exchange authorization code {} for ID Token.", authCode);
            GoogleIdToken idToken = exchangeAuthCodeForIDToken();
            if (idToken == null) {
                log.error("Unable to exchange Authorization code for ID Token");
                return GoogleLoginResponseMessage.AUTH_FAILURE;
            }
            log.info("Attempting to find Keep.id account associated with ID Token");
            Optional<User> userOptional = convertJwtTokenToUser(idToken);
            if (userOptional.isEmpty()) {
                log.info("No Keep.id account associated with ID Token");
                return GoogleLoginResponseMessage.USER_NOT_FOUND;
            }
            user = userOptional.get();
            LoginService.recordActivityLogin(user, activityDao); // record login activity
            LoginService.recordToLoginHistory(user, ip, userAgent, IP_INFO_TOKEN, userDao); // get ip location
            log.info("Login Successful!");
            return GoogleLoginResponseMessage.AUTH_SUCCESS;
        } catch (InterruptedException e) {
            log.error("verifier threw GeneralSecurityException");
        } catch (IOException e) {
            log.error("verifier threw IOException");
        }
        return GoogleLoginResponseMessage.INTERNAL_ERROR;
    }

    private GoogleIdToken exchangeAuthCodeForIDToken() throws InterruptedException, IOException {
        String tokenExchangeUrl = "https://oauth2.googleapis.com/token";
        JSONObject bodyParams = new JSONObject();
        bodyParams.put("client_id", googleClientId);
        bodyParams.put("client_secret", googleClientSecret);
        bodyParams.put("code", authCode);
        bodyParams.put("code_verifier", codeVerifier);
        bodyParams.put("grant_type", "authorization_code");
        bodyParams.put("redirect_uri", redirectUri);

        log.info("Sending POST request to {} with body: {}", tokenExchangeUrl,
            bodyParams);
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenExchangeUrl))
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bodyParams.toString()))
            .build();
        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

        JSONObject responseJSON = new JSONObject(response.body());
        log.info("Received response from {} with body: {}", tokenExchangeUrl, responseJSON);

        if (!responseJSON.has("id_token")) {
            log.error("No ID token found in response, returning null");
            return null;
        }
        return GoogleIdToken.parse(new GsonFactory(),
            (String) responseJSON.get("id_token"));
    }


    private Optional<User> convertJwtTokenToUser(GoogleIdToken idToken) {
        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();

        log.info("Querying user database for email: {}", email);
        return userDao.getByEmail(email);
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

    public String getFirstName() {
        Objects.requireNonNull(user);
        return user.getFirstName();
    }

    public String getLastName() {
        Objects.requireNonNull(user);
        return user.getLastName();
    }

    public String getFullName() {
        Objects.requireNonNull(user);
        return user.getFirstName() + " " + user.getLastName();
    }

    public boolean isTwoFactorOn() {
        Objects.requireNonNull(user);
        return user.getTwoFactorOn();
    }
}