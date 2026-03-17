package OrgTests;

import Organization.Organization;
import User.Address;
import Validation.ValidationException;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrganizationControllerUnitTests {
  @Test
  public void checkCreationDateOrg() throws ValidationException {
    String orgName = "Example Org";
    String orgWebsite = "https://example.com";
    String orgEIN = "12-1234567";
    Address orgAddress = new Address("Example Address", "Chicago", "IL", "12345");
    String orgEmail = "email@email.com";
    String orgPhoneNumber = "1234567890";

    Organization org =
        new Organization(
            orgName,
            orgWebsite,
            orgEIN,
            orgAddress,
            orgEmail,
            orgPhoneNumber);

    Date currDate = new Date();
    Calendar cal = Calendar.getInstance();
    cal.setTime(currDate);

    cal.add(Calendar.SECOND, 1);
    Date upperBound = cal.getTime();

    Date creationDate = org.getCreationDate();

    assertTrue(creationDate.before(upperBound));
  }
}
