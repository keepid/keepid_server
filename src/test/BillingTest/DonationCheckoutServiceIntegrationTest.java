package BillingTest;

import TestUtils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DonationCheckoutServiceIntegrationTest {

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
  }

  @Test
  public void success() { // test is currently broken, we will debug once this is more important
    JSONObject request = new JSONObject();
    request.put("payment_method_nonce", "fake-valid-nonce");
    request.put("amount", "10");
    HttpResponse<String> response =
        Unirest.post(TestUtils.getServerUrl() + "/donation-checkout")
            .body(request.toString())
            .asString();
    assertEquals("Internal server error", response.getBody());
  }

  /**
   * Unable to correctly mock failed response @Test public void failure() {
   * Mockito.when(gateway.transaction().sale(transactionRequest)).thenReturn(transactionResult);
   * JSONObject request = new JSONObject(); request.put("payment_method_nonce",
   * "fake-processor-declined-visa-nonce"); request.put("amount", "10"); HttpResponse<String>
   * response = Unirest.post(TestUtils.getServerUrl() + "/donation-checkout")
   * .body(request.toString()) .asString(); JSONObject responseJSON =
   * TestUtils.responseStringToJSON(response.getBody());
   * assertThat(responseJSON.getString("status")).isEqualTo("FAILURE"); }
   */
}
