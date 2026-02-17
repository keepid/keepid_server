package Config;

import Activity.ActivityController;
import Admin.AdminController;
import Billing.BillingController;
import Database.Activity.ActivityDao;
import Database.Activity.ActivityDaoFactory;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Database.Mail.MailDao;
import Database.Mail.MailDaoFactory;
import Database.Organization.OrgDao;
import Database.Organization.OrgDaoFactory;
import Database.Token.TokenDao;
import Database.Token.TokenDaoFactory;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import File.FileController;
import Form.FormController;
import Issue.IssueController;
import Mail.FileBackfillController;
import Mail.MailController;
import Organization.Organization;
import Organization.OrganizationController;
import PDF.PdfController;
import PDF.PdfControllerV2;
import Production.ProductionController;
import Security.AccountSecurityController;
import Security.EmailSender;
import Security.EmailSenderFactory;
import Security.EncryptionController;
import Security.EncryptionTools;
import Security.EncryptionUtils;
import User.User;
import User.UserController;
import User.UserType;
import com.mongodb.client.MongoDatabase;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import java.util.HashMap;
import java.util.Optional;
import lombok.SneakyThrows;
import org.bson.types.ObjectId;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppConfig {
  public static Long ASYNC_TIME_OUT = 10L;
  public static int SERVER_PORT = Integer.parseInt(System.getenv("PORT"));
  public static int SERVER_TEST_PORT = Integer.parseInt(System.getenv("TEST_PORT"));

  @SneakyThrows
  public static Javalin appFactory(DeploymentLevel deploymentLevel) {
    log.info("Deployment level set to: {}", deploymentLevel);
    System.setProperty("logback.configurationFile", "../Logger/Resources/logback.xml");
    Javalin app = AppConfig.createJavalinApp(deploymentLevel);
    MongoConfig.getMongoClient();
    UserDao userDao = UserDaoFactory.create(deploymentLevel);
    TokenDao tokenDao = TokenDaoFactory.create(deploymentLevel);
    OrgDao orgDao = OrgDaoFactory.create(deploymentLevel);
    FormDao formDao = FormDaoFactory.create(deploymentLevel);
    FileDao fileDao = FileDaoFactory.create(deploymentLevel);
    ActivityDao activityDao = ActivityDaoFactory.create(deploymentLevel);
    MailDao mailDao = MailDaoFactory.create(deploymentLevel);
    MongoDatabase db = MongoConfig.getDatabase(deploymentLevel);
    setApplicationHeaders(app);
    EncryptionTools tools = new EncryptionTools(db);
    tools.generateGoogleCredentials();
    EncryptionUtils.initialize();
    //    try {
    //      encryptionController = new EncryptionController(db);
    //    } catch (GeneralSecurityException | IOException e) {
    //      System.err.println(e.getStackTrace());
    //      System.exit(0);
    //      return null;
    //    }

    // We need to instantiate the controllers with the database.
    EncryptionController encryptionController = new EncryptionController(db);
    EmailSender emailSender = EmailSenderFactory.forDeploymentLevel(deploymentLevel);
    OrganizationController orgController = new OrganizationController(db, activityDao, emailSender);
    UserController userController =
        new UserController(userDao, tokenDao, fileDao, activityDao, formDao, orgDao, db, emailSender);
    AccountSecurityController accountSecurityController =
        new AccountSecurityController(userDao, tokenDao, activityDao, emailSender);
    PdfController pdfController = new PdfController(db, userDao, encryptionController);
    FormController formController = new FormController(formDao, userDao, encryptionController);
    FileController fileController = new FileController(db, userDao, fileDao, activityDao, formDao, encryptionController);
    IssueController issueController = new IssueController(db);
    ActivityController activityController = new ActivityController(activityDao);
    AdminController adminController = new AdminController(userDao, db);
    ProductionController productionController = new ProductionController(orgDao, userDao);
    BillingController billingController = new BillingController();
    MailController mailController =
        new MailController(mailDao, fileDao, encryptionController, deploymentLevel);
    FileBackfillController backfillController = new FileBackfillController(db, fileDao, userDao);
    PdfControllerV2 pdfControllerV2 =
        new PdfControllerV2(fileDao, formDao, activityDao, userDao, encryptionController);
    //    try { do not recommend this block of code, this will delete and regenerate our encryption
    // key
    //      System.out.println("generating keyset");
    //      tools.generateAndUploadKeySet();
    //      System.out.println("successfully generated keyset");
    //    } catch (Exception e) {
    //      System.out.println(e);
    //    }

    /* -------------- DUMMY PATHS ------------------------- */
    app.get("/", ctx -> ctx.result("Welcome to the Keep.id Server"));
    app.get("/custom-upload-form", formController.customFormGet);
    app.get("/ingest-from-csv", userController.ingestCsv);
    // These are all deprecated and should be deleted
    /* -------------- FILE MANAGEMENT --------------------- */
    //    app.post("/upload", pdfController.pdfUpload);
    //    app.post("/upload-annotated", pdfController.pdfUploadAnnotated);
    //    app.post("/upload-signed-pdf", pdfController.pdfSignedUpload);
    //    app.post("/download", pdfController.pdfDownload);
    //    app.post("/delete-document/", pdfController.pdfDelete);
    //    app.post("/get-documents", pdfController.pdfGetFilesInformation);
    //    app.post("/get-application-questions", pdfController.getApplicationQuestions);
    //    app.post("/fill-application", pdfController.fillPDFForm);

    /* -------------- FILE MANAGEMENT v2 --------------------- */
    app.post("/upload-file", fileController.fileUpload);
    app.post("/download-file", fileController.fileDownload);
    app.post("/delete-file/", fileController.fileDelete);
    app.post("/get-files", fileController.getFiles);
    /// app.post("/get-application-questions-v2", fileController.getApplicationQuestions);
    // app.post("/fill-form", fileController.fillPDFForm);

    app.post("/upload-form", formController.formUpload);
    app.post("/get-form", formController.formGet);
    app.post("/delete-form", formController.formDelete);

    /* -------------- PDF CONTROLLER v2 --------------------- */
    app.post("/delete-pdf-2", pdfControllerV2.deletePDF);
    app.post("/download-pdf-2", pdfControllerV2.downloadPDF);
    app.post("/filter-pdf-2", pdfControllerV2.filterPDF);
    app.post("/upload-pdf-2", pdfControllerV2.uploadPDF);
    app.post("/upload-annotated-pdf-2", pdfControllerV2.uploadAnnotatedPDF);
    app.post("/upload-signed-pdf-2", pdfControllerV2.uploadSignedPDF);
    app.post("/get-questions-2", pdfControllerV2.getQuestions);
    app.post("/fill-pdf-2", pdfControllerV2.fillPDF);

    app.post("/get-application-registry", formController.getAppRegistry);

    /* -------------- USER AUTHENTICATION/USER RELATED ROUTES-------------- */
    app.post("/login", userController.loginUser);
    app.post("/googleLoginRequest", userController.googleLoginRequestHandler);
    app.get("/googleLoginResponse", userController.googleLoginResponseHandler);
    app.get("/get-session-user", userController.getSessionUser);
    app.post("/authenticate", userController.authenticateUser);
    app.post("/create-user", userController.createNewUser);
    app.post("/create-invited-user", userController.createNewInvitedUser);
    app.get("/logout", userController.logout);
    app.post("/forgot-password", accountSecurityController.forgotPassword);
    app.post("/change-password", accountSecurityController.changePassword);
    app.post("/reset-password", accountSecurityController.resetPassword);
    app.post("/get-user-info", userController.getUserInfo);
    app.post("/get-organization-info", userController.getOrganizationInfo);
    // New unified profile endpoints
    app.post("/update-user-profile", userController.updateUserProfile);
    app.post("/send-email-login-instructions", userController.sendEmailLoginInstructions);
    app.post("/delete-profile-field", userController.deleteProfileField);
    app.post("/get-organization-members", userController.getMembers);
    app.post("/get-all-members-by-role", userController.getAllMembersByRole);
    app.post("/get-login-history", userController.getLogInHistory);
    app.post("/assign-worker-to-user", userController.assignWorkerToUser);
    app.get("/onboarding-checklist", userController.getOnboardingChecklist);
    app.post("/onboarding-checklist", userController.postOnboardingStatus);

    // TODO: no longer necessary with upload file route
    app.post("/upload-pfp", userController.uploadPfp);
    app.post("/load-pfp", userController.loadPfp);
    app.post("/username-exists", userController.usernameExists);
    app.post("/delete-user", userController.deleteUser);
    app.post("/remove-organization-member", userController.removeOrganizationMember);
    app.post("/set-default-id", userController.setDefaultIds);
    app.post("/get-default-id", userController.getDefaultIds);

    /* -------------- ORGANIZATION SIGN UP ------------------ */
    //    app.post("/organization-signup-validator", orgController.organizationSignupValidator);
    app.post("/organization-signup", orgController.enrollOrganization);

    app.post("/invite-user", orgController.inviteUsers);

    /* -------------- ACCOUNT SETTINGS ------------------ */
    app.post("/change-account-setting", accountSecurityController.changeAccountSetting);
    /* -------------- SUBMIT BUG------------------ */
    app.post("/submit-issue", issueController.submitIssue);

    /* -------------- ADMIN DASHBOARD ------------------ */
    app.post("/get-usertype-count", orgController.findMembersOfOrgs);
    app.post("/test-delete-org", adminController.testDeleteOrg);
    app.post("/delete-org", adminController.deleteOrg);

    /* --------------- SEARCH FUNCTIONALITY ------------- */
    app.post("/get-all-orgs", orgController.listOrgs);
    app.post("/get-all-activities", activityController.findMyActivities);
    app.post("/get-org-activities", activityController.findOrganizationActivities);

    /* --------------- FILE BACKFILL ROUTE ------------- */
    //    app.get("/backfill", backfillController.backfillSingleFile);

    /* --------------- PRODUCTION API --------------- */

    // TODO use Access manager for this instead https://javalin.io/documentation#access-manager
    app.before(
        "/organizations*",
        ctx -> {
          String sessionUsername = ctx.sessionAttribute("username");

          var sessionUser = userDao.get(sessionUsername);

          if (sessionUsername == null || sessionUsername.isEmpty() || sessionUser.isEmpty()) {
            throw new HttpResponseException(401, "Authentication failed", new HashMap<>());
          } else if (sessionUser.get().getUserType() != UserType.Developer) {
            throw new HttpResponseException(403, "Insufficient permissions", new HashMap<>());
          }
        });

    app.get("/organizations", productionController.readAllOrgs);
    app.post("/organizations", productionController.createOrg);

    app.before(
        "/organizations/:orgId",
        ctx -> {
          ObjectId objectId = new ObjectId(ctx.pathParam("orgId"));
          Optional<Organization> organizationOptional = orgDao.get(objectId);

          if (organizationOptional.isEmpty()) {
            throw new HttpResponseException(
                404,
                "Organization with id '" + objectId.toHexString() + "' not found",
                new HashMap<>());
          }
        });
    app.get("/organizations/:orgId", productionController.readOrg);
    app.patch("/organizations/:orgId", productionController.updateOrg);
    app.delete("/organizations/:orgId", productionController.deleteOrg);
    app.get("/organizations/:orgId/users", productionController.getUsersFromOrg);

    app.before(
        "/users*",
        ctx -> {
          if (ctx.method().equals("OPTIONS")) {
            return;
          }

          String sessionUsername = ctx.sessionAttribute("username");
          var sessionUser = userDao.get(sessionUsername);

          if (sessionUsername == null || sessionUsername.isEmpty() || sessionUser.isEmpty()) {
            throw new HttpResponseException(401, "Authentication failed", new HashMap<>());
          } else if (sessionUser.get().getUserType() != UserType.Developer) {
            throw new HttpResponseException(403, "Insufficient permissions", new HashMap<>());
          }
        });

    app.get("/users", productionController.readAllUsers);
    app.post("/users", productionController.createUser);

    app.before(
        "/users/:username",
        ctx -> {
          String username = ctx.pathParam("username");
          Optional<User> userOptional = userDao.get(username);

          if (userOptional.isEmpty()) {
            throw new HttpResponseException(
                404, "User with username '" + username + "' not found", new HashMap<>());
          }
        });

    app.get("/users/:username", productionController.readUser);
    app.patch("/users/:username", productionController.updateUser);
    app.delete("/users/:username", productionController.deleteUser);

    /* -------------- Billing ----------------- */
    app.get("/donation-generate-client-token", billingController.donationGenerateClientToken);
    app.post("/donation-checkout", billingController.donationCheckout);

    /* --------------- WEEKLY METRICS ------------- */
    app.get("/get-weekly-applications", formController.getWeeklyApplications);
    app.get("/get-weekly-uploaded-ids", fileController.getWeeklyUploadedIds);

    /* --------------- MAIL FORM FEATURES ------------- */
    app.get("/get-form-mail-addresses", mailController.getFormMailAddresses);
    app.post("/submit-mail", mailController.saveMail);
    return app;
  }

  public static void setApplicationHeaders(Javalin app) {
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

  public static Javalin createJavalinApp(DeploymentLevel deploymentLevel) {
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

              config.enableCorsForOrigin(
                  "https://keep.id",
                  "https://server.keep.id",
                  "http://localhost",
                  "http://localhost:3000",
                  "https://staging.keep.id",
                  "https://staged.keep.id",
                  "127.0.0.1:3000");

              config.enableDevLogging();
              config.enforceSsl = false;
              // log a warning if user doesn't start javalin instance (default is true)
              config.logIfServerNotStarted = true;
              config.showJavalinBanner = false;
              config.prefer405over404 =
                  false; // send a 405 if handlers exist for different verb on the same path
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
