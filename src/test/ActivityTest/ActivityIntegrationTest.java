package ActivityTest;

import TestUtils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class ActivityIntegrationTest {

  @Before
  public void setup() {
    TestUtils.startServer();
  }

  @Test
  public void testAddActivity() {
    JSONObject body = new JSONObject();
    body.put("username", "createAdminOwner");

    HttpResponse<String> findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/get-all-activities")
            .body(body.toString())
            .asString();
    assert findResponse.isSuccess();
  }
}
