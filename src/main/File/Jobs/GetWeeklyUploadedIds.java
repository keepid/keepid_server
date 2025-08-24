package File.Jobs;

import Config.DeploymentLevel;
import Config.MongoConfig;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import File.File;
import com.mongodb.client.MongoClient;
import java.util.List;
import java.util.Objects;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetWeeklyUploadedIds {
  public static final String weeklyReportTestURL =
      Objects.requireNonNull(System.getenv("WEEKLY_REPORT_TESTURL"));
  public static final String weeklyReportActualURL =
      Objects.requireNonNull(System.getenv("WEEKLY_REPORT_ACTUALURL"));

  public static void main(String[] args) {
    // Connect with the database first. Using PRODUCTION for now
    MongoClient client = MongoConfig.getMongoClient();
    FileDao fileDao = FileDaoFactory.create(DeploymentLevel.STAGING);
    List<File> files = fileDao.getWeeklyUploadedIds();
    generateWeeklyApplicationsSlackMessage(files);
  }

  private static void generateWeeklyApplicationsSlackMessage(List<File> files) {
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
    String description = "The number of uploaded ids this week is " + files.size();
    desText.put("text", description);
    desText.put("type", "mrkdwn");
    desJson.put("text", desText);
    desJson.put("type", "section");
    blocks.put(desJson);
    JSONObject input = new JSONObject();
    input.put("blocks", blocks);
    HttpResponse posted =
        Unirest.post(weeklyReportTestURL)
            .header("accept", "application/json")
            .body(input.toString())
            .asEmpty();
  }
}
