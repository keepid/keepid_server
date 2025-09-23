package Form.Jobs;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Form.Form;
import com.mongodb.client.MongoClient;
import java.util.List;
import java.util.Objects;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetWeeklyApplicationsJob {
  public static final String weeklyReportTestURL =
      Objects.requireNonNull(System.getenv("WEEKLY_REPORT_TESTURL"));
  public static final String weeklyReportActualURL =
      Objects.requireNonNull(System.getenv("WEEKLY_REPORT_ACTUALURL"));

  public static void main(String[] args) {
    // Connect with the database first. Using PRODUCTION for now
    MongoConfig mongoConfig = new MongoConfig();
    MongoClient client = mongoConfig.getMongoClient();
    FormDao formDao = FormDaoFactory.create(DeploymentLevel.STAGING);
    List<Form> forms = formDao.getWeeklyApplications();
    generateWeeklyApplicationsSlackMessage(forms);
  }

  private static void generateWeeklyApplicationsSlackMessage(List<Form> forms) {
    JSONArray blocks = new JSONArray();
    JSONObject titleJson = new JSONObject();
    JSONObject titleText = new JSONObject();
    titleText.put("text", "*Weekly Applications Report* ");
    titleText.put("type", "mrkdwn");
    titleJson.put("type", "section");
    titleJson.put("text", titleText);
    blocks.put(titleJson);
    JSONObject desJson = new JSONObject();
    JSONObject desText = new JSONObject();
    String description = "The number of applications this week is " + forms.size();
    desText.put("text", description);
    desText.put("type", "mrkdwn");
    desJson.put("text", desText);
    desJson.put("type", "section");
    blocks.put(desJson);
    JSONObject input = new JSONObject();
    input.put("blocks", blocks);
    HttpResponse posted =
        Unirest.post(weeklyReportActualURL)
            .header("accept", "application/json")
            .body(input.toString())
            .asEmpty();
  }
}
