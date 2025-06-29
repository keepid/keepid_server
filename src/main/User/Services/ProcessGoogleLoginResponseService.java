package User.Services;

import Activity.LoginActivity;
import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.User.UserDao;
import Issue.IssueController;
import Security.URIUtil;
import User.IpObject;
import User.User;
import User.GoogleLoginResponseMessage;
import User.UserType;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.json.gson.GsonFactory;
import io.ipinfo.api.IPInfo;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class ProcessGoogleLoginResponseService implements Service {
    public final String IP_INFO_TOKEN = Objects.requireNonNull(System.getenv("IPINFO_TOKEN"));
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
            recordActivityLogin(); // record login activity
            getLocationOfLogin(user, ip, userAgent); // get ip location
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
        String googleClientId = System.getenv("REACT_APP_GOOGLE_CLIENT_ID");
        String googleClientSecret = System.getenv("REACT_APP_GOOGLE_CLIENT_SECRET");
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

    public void recordActivityLogin() {
        LoginActivity log = new LoginActivity(user.getUsername(), user.getTwoFactorOn());
        activityDao.save(log);
    }

    public void getLocationOfLogin(User user, String ip, String userAgent) {
        List<IpObject> loginList = user.getLogInHistory();
        if (loginList == null) {
            loginList = new ArrayList<IpObject>(1000);
        }
        if (loginList.size() >= 1000) {
            loginList.remove(0);
        }
        log.info("Trying to add login to login history");

        IpObject thisLogin = new IpObject();
        ZonedDateTime currentTime = ZonedDateTime.now();
        String formattedDate =
                currentTime.format(DateTimeFormatter.ofPattern("MM/dd/YYYY, HH:mm")) +
                    " Local Time";
        boolean isMobile = userAgent.contains("Mobi");
        String device = isMobile ? "Mobile" : "Computer";

        thisLogin.setDate(formattedDate);
        thisLogin.setIp(ip);
        thisLogin.setDevice(device);

        IPInfo ipInfo = IPInfo.builder().setToken(IP_INFO_TOKEN).build();
        try {
            IPResponse response = ipInfo.lookupIP(ip);
            thisLogin.setLocation(
                    response.getPostal() + ", " + response.getCity() + "," + response.getRegion());
        } catch (RateLimitedException ex) {
            log.error("Failed to retrieve login location due to limited rates for IPInfo.com");
            thisLogin.setLocation("Unknown");
            JSONObject body = new JSONObject();
            body.put(
                    "text",
                    "You are receiving this because we have arrived at maximum amount of IP "
                            + "lookups we are allowed for our free plan.");
            Unirest.post(IssueController.issueReportActualURL).body(body.toString()).asEmpty();
        }
        loginList.add(thisLogin);
        addLoginHistoryToDB(loginList);
    }

    public void addLoginHistoryToDB(List<IpObject> loginList) {
        user.setLogInHistory(loginList);
        userDao.update(user);
        log.info("Added login to login history");
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