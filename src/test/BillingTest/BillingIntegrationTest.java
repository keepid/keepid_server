package BillingTest;

import TestUtils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.BeforeClass;
import org.junit.Test;

public class BillingIntegrationTest {
  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
  }

  @Test
  public void tryStart() {
    HttpResponse<String> findResponse =
        Unirest.post(TestUtils.getServerUrl() + "/create-checkout-session").asString();
    System.out.print(findResponse.getBody());
  }
}
