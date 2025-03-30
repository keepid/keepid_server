package User.Services;

import Config.Message;
import Config.Service;
import Database.Activity.ActivityDao;
import Database.Token.TokenDao;
import Database.User.UserDao;
import User.User;
import User.UserMessage;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;


@Slf4j
public class LoginGoogleUserService implements Service {
    public final String IP_INFO_TOKEN = Objects.requireNonNull(System.getenv("IPINFO_TOKEN"));
    private UserDao userDao;
    private TokenDao tokenDao;
    private ActivityDao activityDao;
    private User user;
    private final String jwtToken;
    private final String ip;
    private final String userAgent;
    @Getter
    private LoginService loginService;

    public LoginGoogleUserService(
            UserDao userDao,
            TokenDao tokenDao,
            ActivityDao activityDao,
            String jwtToken,
            String ip,
            String userAgent) {
        this.userDao = userDao;
        this.tokenDao = tokenDao;
        this.activityDao = activityDao;
        this.jwtToken = jwtToken;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    private GoogleIdToken validateJwtToken() throws GeneralSecurityException, IOException {
        String googleClientId = System.getenv("REACT_APP_GOOGLE_CLIENT_ID");
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        GoogleIdToken idToken = verifier.verify(jwtToken);
        return idToken;
    }

    private Optional<User> convertJwtTokenToUser(GoogleIdToken idToken) {
        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        log.info(email);
        boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
        if (emailVerified) {
            log.info("Email Verified");
        } else {
            log.info("Email not verified");
        }
        return userDao.get(email);
    }

    @Override
    public Message executeAndGetResponse() throws Exception {
        try {
            GoogleIdToken idToken = validateJwtToken();
            if (idToken == null) {
                log.info("Invalid JWT Token");
                return UserMessage.AUTH_FAILURE;
            }
            Optional<User> userOptional = convertJwtTokenToUser(idToken);
            if (userOptional.isEmpty()) {
                log.info("user with email does not exist in database");
                return UserMessage.USER_NOT_FOUND;
            }
            user = userOptional.get();
            // just a wrapper of loginService
            this.loginService = new LoginService(userDao, tokenDao, activityDao,
                    user.getUsername(), user.getPassword(), ip, userAgent);
            return loginService.executeAndGetResponse();
        } catch (GeneralSecurityException e) {
            log.info("verifier threw GeneralSecurityException");
        } catch (IOException e) {
            log.info("verifier threw IOException");
        }
        return UserMessage.AUTH_FAILURE;
    }
}