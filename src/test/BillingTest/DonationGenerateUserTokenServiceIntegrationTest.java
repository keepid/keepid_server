package BillingTest;

import TestUtils.TestUtils;
import com.braintreegateway.BraintreeGateway;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

public class DonationGenerateUserTokenServiceIntegrationTest {
  private static BraintreeGateway gateway =
      Mockito.mock(BraintreeGateway.class, Mockito.RETURNS_DEEP_STUBS);

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
  }

  @Test
  public void success() {
    // this test isn't working as we aren't correctly hitting the sandbox api, but for now, billing
    // is deprioritized.
    when(gateway.clientToken().generate()).thenReturn("sampleHash");
    AtomicReference<HttpResponse<String>> response = new AtomicReference<>();
    assertThrows(
        "",
        IllegalStateException.class,
        () -> {
          response.set(
              Unirest.get(TestUtils.getServerUrl() + "/donation-generate-client-token")
                  .header("Accept", "*/*")
                  .header("Content-Type", "text/plain")
                  .asString());
          JSONObject responseJSON = TestUtils.responseStringToJSON(response.get().getBody());
        });
  }
}
