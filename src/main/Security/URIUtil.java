package Security;

public class URIUtil {
  // TODO: in prod and staging environments, should remove the localhost URI
  public static boolean isValidRedirectURI(String redirectUri) {
    return isValidGoogleRedirectURI(redirectUri);
  }

  public static boolean isValidGoogleRedirectURI(String redirectUri) {
    return redirectUri.equals("http://localhost:7001/googleLoginResponse")
        || redirectUri.equals("https://staged.keep.id/googleLoginResponse")
        || redirectUri.equals("https://staging.keep.id/googleLoginResponse")
        || redirectUri.equals("https://server.keep.id/googleLoginResponse");
  }

  public static boolean isValidMicrosoftRedirectURI(String redirectUri) {
    return redirectUri.equals("http://localhost:7001/microsoftLoginResponse")
        || redirectUri.equals("https://staged.keep.id/microsoftLoginResponse")
        || redirectUri.equals("https://staging.keep.id/microsoftLoginResponse")
        || redirectUri.equals("https://server.keep.id/microsoftLoginResponse");
  }

  // TODO: in prod and staging environments, should remove the localhost URI
  public static boolean isValidOriginURI(String originUri) {
    return originUri.equals("http://localhost:3000")
        || originUri.equals("http://localhost:3001")
        || originUri.equals("http://localhost:5173")
        || originUri.equals("https://keep.id")
        || originUri.equals("https://staged.keep.id")
        || originUri.equals("https://staging.keep.id");
  }
}
