package File.Jobs;

import Database.File.FileDao;
import File.File;
import java.util.List;
import java.util.Objects;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetWeeklyUploadedIdsJob {
  public static final String weeklyReportTestURL =
      Objects.requireNonNull(System.getenv("WEEKLY_REPORT_TESTURL"));
  public static final String weeklyReportActualURL =
      Objects.requireNonNull(System.getenv("WEEKLY_REPORT_ACTUALURL"));

  public static void run(FileDao fileDao) {
    List<File> files = fileDao.getWeeklyUploadedIds();
    generateWeeklyUploadedIdsSlackMessage(files);
  }

  private static void generateWeeklyUploadedIdsSlackMessage(List<File> files) {
    JSONArray blocks = new JSONArray();
    JSONObject titleJson = new JSONObject();
    JSONObject titleText = new JSONObject();
    titleText.put("text", "*Weekly Uploaded IDs Report* ");
    titleText.put("type", "mrkdwn");
    titleJson.put("type", "section");
    titleJson.put("text", titleText);
    blocks.put(titleJson);
    JSONObject desJson = new JSONObject();
    JSONObject desText = new JSONObject();
    String description = "The number of uploaded IDs this week is " + files.size();
    desText.put("text", description);
    desText.put("type", "mrkdwn");
    desJson.put("text", desText);
    desJson.put("type", "section");
    blocks.put(desJson);
    JSONObject input = new JSONObject();
    input.put("blocks", blocks);
    Unirest.post(weeklyReportActualURL)
        .header("accept", "application/json")
        .body(input.toString())
        .asEmpty();
  }
}
