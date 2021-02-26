package Issue;

import Config.Message;
import Database.Report.ReportDao;
import com.google.inject.Inject;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.Objects;
import java.util.Optional;

@Slf4j
public class IssueController {

  private final SubmitIssueService submitIssueService;
  private final ReportDao reportDao;

  @Inject
  public IssueController(SubmitIssueService submitIssueService, ReportDao reportDao) {
    this.submitIssueService = submitIssueService;
    this.reportDao = reportDao;
  }

  public static final String issueReportActualURL =
      Objects.requireNonNull(System.getenv("ISSUE_REPORT_ACTUALURL"));
  public static final String issueReportTestURL =
      Objects.requireNonNull(System.getenv("ISSUE_REPORT_TESTURL"));

  public void submitIssue(Context ctx) {
    log.info("Starting submitIssue");
    log.info("Trying to get fields from form");
    JSONObject req = new JSONObject(ctx.body());
    String title = req.getString("title");
    String description = req.getString("description");
    String email = req.getString("email");
    Message response = submitIssueService.executeAndGetResponse(title, description, email);
    log.info(response.toString() + response.getErrorDescription());
    JSONObject res = response.toJSON();
    res.put("issueTitle", title);
    res.put("issueDescription", description);
    res.put("issueEmail", email);
    ctx.result(res.toString());
  }

  // Exclusive for testing purposes.
  public void findIssue(Context ctx) {
    log.info(
        "Starting findIssue (This is never used in a real program. Good for testing purpose.)");
    log.info("Get title from the form");
    JSONObject req = new JSONObject(ctx.body());
    JSONObject res = new JSONObject();
    String title = req.getString("issueTitle");
    if (null == title || "".equals(title)) {
      log.error("The query has no title");
      res.put("issueTitle", "null");
      res.put("issueDescription", "null");
      res.put("issueEmail", "null");
      ctx.result(res.toString());
    } else {
      res.put("issueTitle", title);
      log.info("Trying to find target report from the database.");
      Optional<IssueReport> optionalIssueReport = reportDao.get(title);
      if (optionalIssueReport.isEmpty()) {
        log.info("Target report is not found");
        res.put("issueDescription", "null");
        res.put("issueEmail", "null");
        ctx.result(res.toString());
      } else {
        IssueReport issueReport = optionalIssueReport.get();
        res.put("issueDescription", issueReport.getIssueDescription());
        res.put("issueEmail", issueReport.getIssueEmail());
        ctx.result(res.toString());
        log.info("Finished with findIssue");
      }
    }
  }
}
