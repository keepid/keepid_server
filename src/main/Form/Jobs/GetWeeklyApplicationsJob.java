package Form.Jobs;

import static Form.FormType.APPLICATION;

import Database.Form.FormDao;
import Form.Form;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetWeeklyApplicationsJob {
  public static final String weeklyReportTestURL =
      Objects.requireNonNull(System.getenv("WEEKLY_REPORT_TESTURL"));
  public static final String weeklyReportActualURL =
      Objects.requireNonNull(System.getenv("WEEKLY_REPORT_ACTUALURL"));

  public static void run(FormDao formDao) {
    List<Form> forms = formDao.getWeeklyApplications();
    generateWeeklyApplicationsSlackMessage(forms);
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

  private static void generateWeeklyApplicationsSlackMessage(List<Form> forms) {
    JSONArray blocks = new JSONArray();

    List<Form> applications = forms.stream().filter(f -> f.getFormType() == APPLICATION).toList();

    blocks.put(createSectionBlock("*Weekly Applications Report*"));

    blocks.put(
        createSectionBlock(
            "The number of applications this week is *" + applications.size() + "*."));

    // Group by location (county, state)
    Map<String, Map<String, List<Form>>> locationMap =
        applications.stream()
            .collect(
                Collectors.groupingBy(
                    form -> form.getMetadata().getCounty() + ", " + form.getMetadata().getState(),
                    LinkedHashMap::new,
                    Collectors.groupingBy(
                        Form::getUploaderUsername, LinkedHashMap::new, Collectors.toList())));

    StringBuilder sb = new StringBuilder();

    for (var locationEntry : locationMap.entrySet()) {
      String location = locationEntry.getKey();
      sb.append("*Location:* ").append(location).append("\n");

      Map<String, List<Form>> userMap = locationEntry.getValue();
      for (var userEntry : userMap.entrySet()) {
        String uploader = userEntry.getKey();
        sb.append("    • *Uploader:* ").append(uploader).append("\n");

        List<Form> userForms = userEntry.getValue();
        for (Form f : userForms) {
          sb.append("        • ").append(f.getMetadata().getTitle());
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
