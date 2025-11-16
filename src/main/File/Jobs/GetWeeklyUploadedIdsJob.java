package File.Jobs;

import Database.File.FileDao;
import File.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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

  private static JSONObject createSectionBlock(String text) {
    JSONObject block = new JSONObject();
    JSONObject textJson = new JSONObject();
    textJson.put("text", text);
    textJson.put("type", "mrkdwn");
    block.put("type", "section");
    block.put("text", textJson);
    return block;
  }

  private static void generateWeeklyUploadedIdsSlackMessage(List<File> files) {
    JSONArray blocks = new JSONArray();

    blocks.put(createSectionBlock("*Weekly Uploaded IDs Report*"));

    blocks.put(
        createSectionBlock("The number of uploaded IDs this week is *" + files.size() + "*."));

    // Group by organization -> uploader
    Map<String, Map<String, List<File>>> orgMap =
        files.stream()
            .collect(
                Collectors.groupingBy(
                    File::getOrganizationName,
                    LinkedHashMap::new,
                    Collectors.groupingBy(
                        File::getUsername, LinkedHashMap::new, Collectors.toList())));

    StringBuilder sb = new StringBuilder();

    for (var orgEntry : orgMap.entrySet()) {
      String orgName = orgEntry.getKey();
      sb.append("*Organization:* ").append(orgName).append("\n");

      Map<String, List<File>> userMap = orgEntry.getValue();
      for (var userEntry : userMap.entrySet()) {
        String username = userEntry.getKey();
        sb.append("    • *Uploader:* ").append(username).append("\n");

        List<File> userFiles = userEntry.getValue();
        for (File f : userFiles) {
          sb.append("        • [")
              .append(f.getFileType())
              .append("] ")
              .append(f.getFilename())
              .append("\n");
        }
      }
    }

    blocks.put(createSectionBlock(sb.toString()));

    JSONObject payload = new JSONObject();
    payload.put("blocks", blocks);

    try {
      Unirest.post(weeklyReportActualURL)
          .header("accept", "application/json")
          .body(payload.toString())
          .asEmpty();
    } catch (Exception e) {
      System.err.println("Failed to send Slack message: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
