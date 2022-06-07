package FormTest;

import Form.Form;
import TestUtils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.bson.types.ObjectId;
import org.json.JSONObject;

import static org.assertj.core.api.Assertions.assertThat;

public class FormControllerIntegrationTestHelperMethods {

  public static String username = "adminBSM";

  public static void uploadForm(Form form) {
    JSONObject exampleFormJSON = form.toJSON();
    JSONObject body = new JSONObject();
    body.put("targetUser", username);
    body.put("form", exampleFormJSON);

    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/upload-form").body(body.toString()).asString();
    System.out.println(uploadResponse.getBody());
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  public static void updateForm(Form form) {
    JSONObject exampleFormJSON = form.toJSON();
    JSONObject body = new JSONObject();
    body.put("targetUser", username);
    body.put("form", exampleFormJSON);

    HttpResponse<String> uploadResponse =
        Unirest.post(TestUtils.getServerUrl() + "/update-form").body(body.toString()).asString();
    System.out.println(uploadResponse.getBody());
    JSONObject uploadResponseJSON = TestUtils.responseStringToJSON(uploadResponse.getBody());
    assertThat(uploadResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  public static void delete(ObjectId id) {
    JSONObject body = new JSONObject();
    body.put("fileId", id);
    body.put("targetUser", username);
    body.put("isTemplate", "false");
    HttpResponse<String> deleteResponse =
        Unirest.post(TestUtils.getServerUrl() + "/delete-form").body(body.toString()).asString();
    System.out.println(deleteResponse.getBody());
    JSONObject deleteResponseJSON = TestUtils.responseStringToJSON(deleteResponse.getBody());
    assertThat(deleteResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  public static void getForm(ObjectId id) {
    JSONObject body = new JSONObject();
    body.put("fileId", id);
    body.put("targetUser", username);
    body.put("isTemplate", "false");
    HttpResponse<String> getResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-form").body(body.toString()).asString();
    System.out.println(getResponse.getBody());
    JSONObject deleteResponseJSON = TestUtils.responseStringToJSON(getResponse.getBody());
    assertThat(deleteResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }

  public static void getAllForms() {
    JSONObject body = new JSONObject();
    body.put("targetUser", username);
    body.put("isTemplate", "false");
    HttpResponse<String> getResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-form").body(body.toString()).asString();
    System.out.println(getResponse.getBody());
    JSONObject deleteResponseJSON = TestUtils.responseStringToJSON(getResponse.getBody());
    assertThat(deleteResponseJSON.getString("status")).isEqualTo("SUCCESS");
  }
}
