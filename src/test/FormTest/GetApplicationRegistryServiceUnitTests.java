package FormTest;

import static org.junit.Assert.assertEquals;

import Config.DeploymentLevel;
import Database.ApplicationRegistry.ApplicationRegistryDao;
import Database.ApplicationRegistry.ApplicationRegistryDaoTestImpl;
import Form.ApplicationRegistryEntry;
import Form.FormMessage;
import Form.Services.GetApplicationRegistryService;
import java.math.BigDecimal;
import java.util.List;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetApplicationRegistryServiceUnitTests {
  private ApplicationRegistryDao registryDao;

  @Before
  public void setup() {
    registryDao = new ApplicationRegistryDaoTestImpl(DeploymentLevel.IN_MEMORY);
    registryDao.clear();
  }

  @Test
  public void returnsRegistryForMatchingLookupKeyAndOrgMapping() {
    ObjectId fileId = new ObjectId();
    ApplicationRegistryEntry entry =
        new ApplicationRegistryEntry(
            "SS$FED$INITIAL",
            "Social Security Initial",
            "Social Security Card",
            "FED",
            "INITIAL",
            null,
            new BigDecimal("0"),
            1,
            List.of(new ApplicationRegistryEntry.OrgMapping("TSA C.A.T.S Program", fileId)));
    registryDao.save(entry);

    GetApplicationRegistryService service =
        new GetApplicationRegistryService(
            registryDao, "SS", "FED", "INITIAL", "MYSELF", "TSA C.A.T.S Program");

    assertEquals(FormMessage.SUCCESS, service.executeAndGetResponse());
    JSONObject res = new JSONObject(service.getJsonInformation());
    assertEquals(fileId.toHexString(), res.getString("blankFormId"));
  }

  @Test
  public void returnsInvalidParameterWhenLookupKeyDoesNotExist() {
    GetApplicationRegistryService service =
        new GetApplicationRegistryService(
            registryDao, "SS", "FED", "REPLACEMENT", "MYSELF", "TSA C.A.T.S Program");

    assertEquals(FormMessage.INVALID_PARAMETER, service.executeAndGetResponse());
  }

  @Test
  public void returnsInvalidParameterWhenOrgMappingMissingForExistingLookupKey() {
    ApplicationRegistryEntry entry =
        new ApplicationRegistryEntry(
            "SS$FED$INITIAL",
            "Social Security Initial",
            "Social Security Card",
            "FED",
            "INITIAL",
            null,
            new BigDecimal("0"),
            1,
            List.of(
                new ApplicationRegistryEntry.OrgMapping("Some Other Org", new ObjectId())));
    registryDao.save(entry);

    GetApplicationRegistryService service =
        new GetApplicationRegistryService(
            registryDao, "SS", "FED", "INITIAL", "MYSELF", "TSA C.A.T.S Program");

    assertEquals(FormMessage.SUCCESS, service.executeAndGetResponse());
  }

  @Test
  public void supportsLegacyHashDelimitedLookupKeys() {
    ObjectId fileId = new ObjectId();
    ApplicationRegistryEntry entry =
        new ApplicationRegistryEntry(
            "TEST#NA#NA",
            "Test App",
            "NA",
            "NA",
            "NA",
            null,
            new BigDecimal("0"),
            1,
            List.of(new ApplicationRegistryEntry.OrgMapping("Daniel's org", fileId)));
    registryDao.save(entry);

    GetApplicationRegistryService service =
        new GetApplicationRegistryService(
            registryDao, "TEST", "NA", "NA", "MYSELF", "Daniel's org");

    assertEquals(FormMessage.SUCCESS, service.executeAndGetResponse());
    JSONObject res = new JSONObject(service.getJsonInformation());
    assertEquals(fileId.toHexString(), res.getString("blankFormId"));
  }
}
