package Config;

import Activity.ActivityController;
import Admin.AdminController;
import Issue.IssueController;
import Organization.OrgControllerV2;
import Organization.OrganizationController;
import PDF.DocumentControllerV2;
import PDF.PdfController;
import Security.AccountSecurityController;
import Security.EncryptionUtils;
import User.UserController;
import User.UserControllerV2;
import com.google.inject.Inject;
import io.javalin.Javalin;

import static io.javalin.apibuilder.ApiBuilder.crud;

public class AppConfigV2 {
  public static Long ASYNC_TIME_OUT = 10L;
  public static int SERVER_PORT = Integer.parseInt(System.getenv("PORT"));
  public static int SERVER_TEST_PORT = Integer.parseInt(System.getenv("TEST_PORT"));

  private final UserControllerV2 userControllerV2;
  private final OrgControllerV2 orgControllerV2;
  private final DocumentControllerV2 documentControllerV2;
  private final PdfController pdfController;
  private final UserController userController;
  private final OrganizationController orgController;
  private final AccountSecurityController accountSecurityController;
  private final IssueController issueController;
  private final AdminController adminController;
  private final ActivityController activityController;

  @Inject
  public AppConfigV2(
      UserControllerV2 userControllerV2,
      OrgControllerV2 orgControllerV2,
      DocumentControllerV2 documentControllerV2,
      PdfController pdfController,
      UserController userController,
      OrganizationController orgController,
      AccountSecurityController accountSecurityController,
      IssueController issueController,
      AdminController adminController,
      ActivityController activityController) {
    this.userControllerV2 = userControllerV2;
    this.orgControllerV2 = orgControllerV2;
    this.documentControllerV2 = documentControllerV2;
    this.pdfController = pdfController;
    this.userController = userController;
    this.orgController = orgController;
    this.accountSecurityController = accountSecurityController;
    this.issueController = issueController;
    this.adminController = adminController;
    this.activityController = activityController;
  }

  public Javalin appFactory(DeploymentLevel deploymentLevel) {
    System.setProperty("logback.configurationFile", "../Logger/Resources/logback.xml");
    Javalin app = createJavalinApp(deploymentLevel);
    MongoConfig.getMongoClient();
    setApplicationHeaders(app);

    EncryptionUtils.initialize();
    /* -------------- DUMMY PATHS ------------------------- */
    app.get("/", ctx -> ctx.result("Welcome to the Keep.id Server"));

    /* -------------- FILE MANAGEMENT --------------------- */
    app.post("/upload", pdfController.pdfUpload);
    app.post("/upload-annotated", pdfController.pdfUploadAnnotated);
    app.post("/upload-signed-pdf", pdfController.pdfSignedUpload);
    app.post("/download", pdfController.pdfDownload);
    app.post("/delete-document/", pdfController.pdfDelete);
    app.post("/get-documents", pdfController.pdfGetDocuments);
    app.post("/get-application-questions", pdfController.getApplicationQuestions);
    app.post("/fill-application", pdfController.fillPDFForm);

    /* -------------- USER AUTHENTICATION/USER RELATED ROUTES-------------- */
    app.post("/login", userController.loginUser);
    app.post("/authenticate", userController.authenticateUser);
    app.post("/create-user", userController.createNewUser);
    app.post("/create-invited-user", userController.createNewInvitedUser);
    app.get("/logout", userController.logout);
    app.post("/forgot-password", accountSecurityController.forgotPassword);
    app.post("/change-password", accountSecurityController.changePassword);
    app.post("/reset-password", accountSecurityController.resetPassword);
    app.post("/get-user-info", userController.getUserInfo);
    app.post("/two-factor", accountSecurityController.twoFactorAuth);
    app.post("/get-organization-members", userController.getMembers);
    app.post("/get-login-history", userController.getLogInHistory);
    app.post("/upload-pfp", userController.uploadPfp);
    app.post("/load-pfp", userController.loadPfp);
    app.post("/username-exists", userController.usernameExists);

    /* -------------- ORGANIZATION SIGN UP ------------------ */
    //    app.post("/organization-signup-validator", orgController.organizationSignupValidator);
    app.post("/organization-signup", orgController.enrollOrganization);

    app.post("/invite-user", orgController.inviteUsers);

    /* -------------- ACCOUNT SETTINGS ------------------ */
    app.post("/change-account-setting", accountSecurityController.changeAccountSetting);
    app.post("/change-two-factor-setting", accountSecurityController.change2FASetting);

    /* -------------- SUBMIT BUG------------------ */
    app.post("/submit-issue", issueController::submitIssue);

    /* -------------- ADMIN DASHBOARD ------------------ */
    app.post("/delete-org", adminController::deleteOrg);

    /* --------------- SEARCH FUNCTIONALITY ------------- */
    app.post("/get-all-orgs", orgController.listOrgs);
    app.post("/get-all-activities", activityController::findMyActivities);

    /* --------------- PRODUCTION TOOLING ------------- */
    app.routes(
        () -> {
          crud("organizations/:organizationID", orgControllerV2);
        });

    app.routes(
        () -> {
          crud("users/:username", userControllerV2);
        });

    app.routes(
        () -> {
          crud("users/:username/documents/:documentId", documentControllerV2);
        });

    return app;
  }

  public void setApplicationHeaders(Javalin app) {
    app.before(
        ctx -> {
          ctx.header("Content-Security-Policy", "script-src 'self' 'unsafe-inline';");
          ctx.header("X-Frame-Options", "SAMEORIGIN");
          ctx.header("X-Xss-Protection", "1; mode=block");
          ctx.header("X-Content-Type-Options", "nosniff");
          ctx.header("Referrer-Policy", "no-referrer-when-downgrade");
          ctx.header("Access-Control-Allow-Credentials", "true");
        });
  }

  public Javalin createJavalinApp(DeploymentLevel deploymentLevel) {
    int port;
    switch (deploymentLevel) {
      case STAGING:
      case PRODUCTION:
        port = SERVER_PORT;
        break;
      case TEST:
        port = SERVER_TEST_PORT;
        break;
      default:
        throw new IllegalStateException(
            "Remember to config your port according to: " + deploymentLevel);
    }
    return Javalin.create(
            config -> {
              config.asyncRequestTimeout =
                  ASYNC_TIME_OUT; // timeout for async requests (default is 0, no timeout)
              config.autogenerateEtags = false; // auto generate etags (default is false)
              config.contextPath = "/"; // context path for the http servlet (default is "/")
              config.defaultContentType =
                  "text/plain"; // content type to use if no content type is set (default is
              // "text/plain")

              config.enableCorsForAllOrigins(); // enable cors for all origins

              config.enableDevLogging(); // enable extensive development logging for
              // http and
              // websocket
              config.enforceSsl = false;
              // log a warning if user doesn't start javalin instance (default is true)
              config.logIfServerNotStarted = true;
              config.showJavalinBanner = false;
              config.prefer405over404 =
                  false; // send a 405 if handlers exist for different verb on the same path
              // (default is false)
              config.sessionHandler(
                  () -> {
                    try {
                      return SessionConfig.getSessionHandlerInstance(deploymentLevel);
                    } catch (Exception e) {
                      System.err.println("Unable to instantiate session handler.");
                      e.printStackTrace();
                      System.exit(1);
                      return null;
                    }
                  });
            })
        .start(port);
  }
}
