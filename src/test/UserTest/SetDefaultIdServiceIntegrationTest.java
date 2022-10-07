package UserTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoFactory;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import User.User;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.*;

import static org.assertj.core.api.Assertions.assertThat;

public class SetDefaultIdServiceIntegrationTest {
    UserDao userDao;
    JSONObject loginBody = new JSONObject();

    @Before
    public void configureDatabase() {
        TestUtils.startServer();
        // NEVER USE DEPLOYMENT LEVEL HIGHER THAN TEST
        userDao = UserDaoFactory.create(DeploymentLevel.TEST);

        // Initializing login body here since we need to authenticate the user in every endpoint we are testing
        loginBody.put("username", "johnsmith");
        loginBody.put("password", "johnsmith");
    }

    @After
    public void reset() {
        userDao.clear();
    }

    @Test
    public void setSocialSecurityCardSuccess() {
        User user = EntityFactory.createUser()
                .withFirstName("john")
                .withLastName("smith")
                .withUsername("johnsmith")
                .withPasswordToHash("johnsmith")
                .buildAndPersist(userDao);

        String documentType = "social-security";
        String documentId = "12345";

        Unirest.post(TestUtils.getServerUrl() + "/login").body(loginBody.toString()).asString();

        JSONObject body = new JSONObject();
        body.put("documentType", documentType);
        body.put("id", documentId);

        HttpResponse<String> actualResponse =
                Unirest.post(TestUtils.getServerUrl() + "/set-default-id")
                        .header("Accept", "*/*")
                        .header("Content-Type", "text/plain")
                        .body(body.toString())
                        .asString();

        JSONObject actualResponseJSON = TestUtils.responseStringToJSON(actualResponse.getBody().toString());

        assert (actualResponseJSON.has("status"));
        assertThat(actualResponseJSON.getString("status")).isEqualTo("SUCCESS");

        assert (actualResponseJSON.has("fileId"));
        assertThat(actualResponseJSON.getString("fileId")).isEqualTo(documentId);
    }

    @Test
    public void setVaccineCardSuccess() {
        User user = EntityFactory.createUser()
                .withFirstName("john")
                .withLastName("smith")
                .withUsername("johnsmith")
                .withPasswordToHash("johnsmith")
                .buildAndPersist(userDao);

        String documentType = "vaccine-card";
        String documentId = "23456";

        Unirest.post(TestUtils.getServerUrl() + "/login").body(loginBody.toString()).asString();

        JSONObject body = new JSONObject();
        body.put("documentType", documentType);
        body.put("id", documentId);

        HttpResponse<String> actualResponse =
                Unirest.post(TestUtils.getServerUrl() + "/set-default-id")
                        .header("Accept", "*/*")
                        .header("Content-Type", "text/plain")
                        .body(body.toString())
                        .asString();

        JSONObject actualResponseJSON = TestUtils.responseStringToJSON(actualResponse.getBody().toString());

        assert (actualResponseJSON.has("status"));
        assertThat(actualResponseJSON.getString("status")).isEqualTo("SUCCESS");

        assert (actualResponseJSON.has("fileId"));
        assertThat(actualResponseJSON.getString("fileId")).isEqualTo(documentId);
    }

    @Test
    public void setDriversLicenseSuccess() {
        User user = EntityFactory.createUser()
                .withFirstName("john")
                .withLastName("smith")
                .withUsername("johnsmith")
                .withPasswordToHash("johnsmith")
                .buildAndPersist(userDao);

        String documentType = "drivers-license";
        String documentId = "34567";

        Unirest.post(TestUtils.getServerUrl() + "/login").body(loginBody.toString()).asString();

        JSONObject body = new JSONObject();
        body.put("documentType", documentType);
        body.put("id", documentId);

        HttpResponse<String> actualResponse =
                Unirest.post(TestUtils.getServerUrl() + "/set-default-id")
                        .header("Accept", "*/*")
                        .header("Content-Type", "text/plain")
                        .body(body.toString())
                        .asString();

        JSONObject actualResponseJSON = TestUtils.responseStringToJSON(actualResponse.getBody().toString());

        assert (actualResponseJSON.has("status"));
        assertThat(actualResponseJSON.getString("status")).isEqualTo("SUCCESS");

        assert (actualResponseJSON.has("fileId"));
        assertThat(actualResponseJSON.getString("fileId")).isEqualTo(documentId);
    }

    @Test
    public void setBirthCertificateSuccess() {
        User user = EntityFactory.createUser()
                .withFirstName("john")
                .withLastName("smith")
                .withUsername("johnsmith")
                .withPasswordToHash("johnsmith")
                .buildAndPersist(userDao);

        String documentType = "birth-certificate";
        String documentId = "45678";

        Unirest.post(TestUtils.getServerUrl() + "/login").body(loginBody.toString()).asString();

        JSONObject body = new JSONObject();
        body.put("documentType", documentType);
        body.put("id", documentId);

        HttpResponse<String> actualResponse =
                Unirest.post(TestUtils.getServerUrl() + "/set-default-id")
                        .header("Accept", "*/*")
                        .header("Content-Type", "text/plain")
                        .body(body.toString())
                        .asString();

        JSONObject actualResponseJSON = TestUtils.responseStringToJSON(actualResponse.getBody().toString());

        assert (actualResponseJSON.has("status"));
        assertThat(actualResponseJSON.getString("status")).isEqualTo("SUCCESS");

        assert (actualResponseJSON.has("fileId"));
        assertThat(actualResponseJSON.getString("fileId")).isEqualTo(documentId);
    }
}
