package Security;

public class URIUtil {
  // TODO: in prod and staging environments, should remove the localhost URI
  public static boolean isValidRedirectURI(String redirectUri) {
    return redirectUri.equals("http://localhost:7001/googleLoginResponse")
        || redirectUri.equals("https://staged.keep.id/googleLoginResponse")
        || redirectUri.equals("https://staging.keep.id/googleLoginResponse")
        || redirectUri.equals("https://server.keep.id/googleLoginResponse");
  }

  // TODO: in prod and staging environments, should remove the localhost URI
  public static boolean isValidOriginURI(String originUri) {
    return originUri.equals("http://localhost:3000")
        || originUri.equals("https://keep.id");
  }
}
