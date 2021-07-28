package BillingControllerTest;

import TestUtils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BillingControllerIntegrationTest {

    @BeforeClass
    public static void setUp() {
        TestUtils.startServer();
        TestUtils.setUpTestDB();
    }

    @AfterClass
    public static void tearDown() {
        TestUtils.tearDownTestDB();
    }

    @Test
    public void getOrgEmailTest() {
        TestUtils.login("adminBSM", "adminBSM");

        HttpResponse<String> actualResponse =
                Unirest.get(TestUtils.getServerUrl() + "/get-orgEmail").asString();
        JSONObject actualResponseJson = TestUtils.responseStringToJSON(actualResponse.getBody());
        assertThat(actualResponseJson.get("city").toString()).isEqualTo("Philadelphia");
        assertThat(actualResponseJson.get("firstName").toString()).isEqualTo("Mike");
        assertThat(actualResponseJson.get("lastName").toString()).isEqualTo("Dahl");
        assertThat(actualResponseJson.get("zipcode").toString()).isEqualTo("19104");
        assertThat(actualResponseJson.get("phone").toString()).isEqualTo("1234567890");
        assertThat(actualResponseJson.get("address").toString()).isEqualTo("311 Broad Street");
        assertThat(actualResponseJson.get("birthDate").toString()).isEqualTo("06-16-1960");
        assertThat(actualResponseJson.get("email").toString())
                .isEqualTo("mikedahl@broadstreetministry.org");
    }
}
