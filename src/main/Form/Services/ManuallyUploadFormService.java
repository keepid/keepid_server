package Form.Services;

import Config.Message;
import Config.Service;
import Database.Form.FormDao;
import Form.Form;
import Form.FormMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

public class ManuallyUploadFormService implements Service {

  FormDao formDao;
  JSONObject formJson;
  Form form;
  private static String exampleFormJsonPath =
      Paths.get("").toAbsolutePath()
          + File.separator
          + "src"
          + File.separator
          + "main"
          + File.separator
          + "Form"
          + File.separator
          + "Services"
          + File.separator
          + "exampleForm.json";

  private static String metadataPath =
      Paths.get("").toAbsolutePath()
          + File.separator
          + "src"
          + File.separator
          + "main"
          + File.separator
          + "Form"
          + File.separator
          + "Services"
          + File.separator
          + "Resources"
          + File.separator
          + "metadata.csv";
  private static String questionsPath =
      Paths.get("").toAbsolutePath()
          + File.separator
          + "src"
          + File.separator
          + "main"
          + File.separator
          + "Form"
          + File.separator
          + "Services"
          + File.separator
          + "Resources"
          + File.separator
          + "questions.csv";

  public ManuallyUploadFormService(FormDao formDao) {
    this.formDao = formDao;
    try {
      this.formJson =
          new JSONObject(new String(Files.readAllBytes(Paths.get(exampleFormJsonPath))));
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  @Override
  public Message executeAndGetResponse() {
    try {
      this.form = Form.fromJson(formJson);
    } catch (Exception e) {
      return FormMessage.INVALID_FORM;
    }
    if (this.form == null) {
      return FormMessage.INVALID_FORM;
    }
    formDao.save(form);
    return FormMessage.SUCCESS;
  }

  public static String convertCsvToJson(
      String metadataCsvPath, String questionsCsvPath, String username, String fileId) {
    Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    // Parse Metadata CSV
    Map<String, String> metadata = new HashMap<>();
    try (CSVReader reader = new CSVReader(new FileReader(metadataCsvPath))) {
      String[] headers = reader.readNext();
      String[] values = reader.readNext();
      for (int i = 0; i < headers.length; i++) {
        metadata.put(headers[i], values[i]);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Parse Questions CSV
    List<Map<String, Object>> questions = new ArrayList<>();
    try (CSVReader reader = new CSVReader(new FileReader(questionsCsvPath))) {
      String[] headers = reader.readNext();
      String[] line;
      while ((line = reader.readNext()) != null) {
        Map<String, Object> question = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
          if ("options".equals(headers[i]) && !line[i].isEmpty()) {
            question.put(headers[i], gson.fromJson(line[i], List.class));
          } else {
            question.put(headers[i], line[i]);
          }
        }
        questions.add(question);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    Map<String, Object> jsonMap = new HashMap<>();
    Map<String, String> metadataMap = new HashMap<>();
    metadataMap.put("county", metadata.getOrDefault("County", ""));
    metadataMap.put("description", metadata.getOrDefault("Description", ""));
    metadataMap.put("state", metadata.getOrDefault("State", ""));
    metadataMap.put("title", metadata.getOrDefault("Title", ""));

    jsonMap.put("metadata", metadataMap);
    jsonMap.put("uploaderUsername", username);
    jsonMap.put("formType", "FORM");
    jsonMap.put("condition", "");

    Map<String, Object> body = new HashMap<>();
    body.put("questions", questions);
    body.put("title", metadata.getOrDefault("Title", ""));
    body.put("description", metadata.getOrDefault("Description", ""));
    body.put("revision", metadata.getOrDefault("Revision", ""));
    jsonMap.put("body", body);

    jsonMap.put("fileId", fileId);
    jsonMap.put("username", username);

    // Convert to JSON String
    return gson.toJson(jsonMap);
  }

  public static String createJSON() {
    String username = "test1234";
    String fileId = "671f055645d8b1652ecb14b4";
    String json = convertCsvToJson(metadataPath, questionsPath, username, fileId);
    System.out.println(json);
    return json;
  }

  public static void writeJSON(String json) {
    try (FileWriter fileWriter = new FileWriter(exampleFormJsonPath)) {
      fileWriter.write(json);
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("JSON written to: " + exampleFormJsonPath);
  }
}
