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
    public final String IP_INFO_TOKEN = System.getenv("IPINFO_TOKEN");
    private final String googleClientId = System.getenv("GOOGLE_CLIENT_ID");
    private final String googleClientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
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

    // Google profile info extracted from ID token (available when USER_NOT_FOUND)
    private String googleEmail;
    private String googleFirstName;
    private String googleLastName;

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
        if (googleClientId == null || googleClientSecret == null) {
            log.error("Google OAuth environment variables are not configured. " +
                "Please set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
            return GoogleLoginResponseMessage.INTERNAL_ERROR;
        }
        if (IP_INFO_TOKEN == null) {
            log.warn("IPINFO_TOKEN environment variable is not set. IP geolocation will be unavailable.");
        }
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
            if (!verifyIdToken(idToken)) {
                log.error("ID Token verification failed");
                return GoogleLoginResponseMessage.AUTH_FAILURE;
            }
            log.info("Attempting to find Keep.id account associated with ID Token");
            // Extract Google profile info from token (for sign-up pre-fill if user not found)
            GoogleIdToken.Payload payload = idToken.getPayload();
            googleEmail = payload.getEmail();
            googleFirstName = (String) payload.get("given_name");
            googleLastName = (String) payload.get("family_name");

            Optional<User> userOptional = convertJwtTokenToUser(idToken);
            if (userOptional.isEmpty()) {
                log.info("No Keep.id account associated with ID Token for email: {}", googleEmail);
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

        // log.info("Sending POST request to {} with body: {}", tokenExchangeUrl,
        //     bodyParams);
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

        if (!responseJSON.has("id_token")) {
            // Log Google's error response to help diagnose token exchange failures
            String error = responseJSON.optString("error", "unknown");
            String errorDescription = responseJSON.optString("error_description", "no description");
            log.error("Google token exchange failed. HTTP status: {}, error: '{}', description: '{}'. " +
                "Verify that GOOGLE_CLIENT_SECRET is correct and that the redirect URI '{}' " +
                "is registered in the Google Cloud Console.",
                response.statusCode(), error, errorDescription, redirectUri);
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

    private boolean verifyIdToken(GoogleIdToken idToken) {
        GoogleIdToken.Payload payload = idToken.getPayload();
        
        // Verify audience (client_id)
        if (!googleClientId.equals(payload.getAudience())) {
            log.error("Invalid audience in ID token");
            return false;
        }
        
        // Verify issuer
        if (!"https://accounts.google.com".equals(payload.getIssuer()) && 
            !"accounts.google.com".equals(payload.getIssuer())) {
            log.error("Invalid issuer in ID token");
            return false;
        }
        
        // Verify expiration
        if (payload.getExpirationTimeSeconds() * 1000 < System.currentTimeMillis()) {
            log.error("ID token has expired");
            return false;
        }
        
        return true;
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

    /** Google profile getters (available after executeAndGetResponse, even when USER_NOT_FOUND) */
    public String getGoogleEmail() {
        return googleEmail;
    }

    public String getGoogleFirstName() {
        return googleFirstName;
    }

    public String getGoogleLastName() {
        return googleLastName;
    }
}