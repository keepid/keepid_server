package Issue;

import Config.Message;
import Database.Report.ReportDao;
import Validation.ValidationUtils;
import com.google.inject.Inject;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import static Issue.IssueController.issueReportActualURL;

@Slf4j
public class SubmitIssueService {

  ReportDao reportDao;

  @Inject
  public SubmitIssueService(ReportDao reportDao) {
    this.reportDao = reportDao;
  }

  public Message executeAndGetResponse(String title, String description, String email) {
    log.info("Checking for empty fields");
    if (title == null || title.isEmpty()) {
      log.error("Empty issue title");
      return IssueReportMessage.EMPTY_FIELD;
    }
    if (description == null || description.isEmpty()) {
      log.error("Empty issue description");
      return IssueReportMessage.EMPTY_FIELD;
    }
    log.info("Checking email validation");
    if (email == null || email.isEmpty()) {
      log.error("Empty email field");
      return IssueReportMessage.EMPTY_FIELD;
    }
    if (!ValidationUtils.isValidEmail(email)) {
      log.error("Invalid email");
      return IssueReportMessage.INVALID_EMAIL;
    }
    log.info("Trying to convert the report to a message on Slack");
    JSONArray blocks = new JSONArray();
    JSONObject titleJson = new JSONObject();
    JSONObject titleText = new JSONObject();
    titleText.put("text", "*Issue Title: * " + title);
    titleText.put("type", "mrkdwn");
    titleJson.put("type", "section");
    titleJson.put("text", titleText);
    blocks.put(titleJson);
    JSONObject desJson = new JSONObject();
    JSONObject desText = new JSONObject();
    desText.put("text", "*Issue Description: * " + description);
    desText.put("type", "mrkdwn");
    desJson.put("text", desText);
    desJson.put("type", "section");
    blocks.put(desJson);
    JSONObject input = new JSONObject();
    input.put("blocks", blocks);
    log.info("Trying to post the message on Slack");
    HttpResponse posted =
        Unirest.post(issueReportActualURL)
            .header("accept", "application/json")
            .body(input.toString())
            .asEmpty();
    log.info("Trying to add issueReport to database");
    IssueReport issueReport = new IssueReport(title, description, email);
    reportDao.save(issueReport);
    if (!posted.isSuccess()) {
      log.error("Posing on Slack failed");
      return IssueReportMessage.SLACK_FAILED;
    }
    log.info("Issue successfully submitted");
    return IssueReportMessage.SUCCESS;
  }
}
