package UserTest;

import Config.DeploymentLevel;
import Database.User.UserDao;
import Database.User.UserDaoTestImpl;
import TestUtils.EntityFactory;
import User.Services.GetUserDefaultIdService;
import User.User;
import org.junit.Test;
import Config.Message;
import User.UserMessage;
import User.Services.DocumentType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetDefaultIdServiceUnitTest {
    private UserDao userDao;

    @Test
    public void social_security_card_successful_test() {
        UserDao userDao = new UserDaoTestImpl(DeploymentLevel.IN_MEMORY);

        User user = EntityFactory.createUser()
                .withFirstName("Jason")
                .withLastName("Zhang")
                .withUsername("jzhang0107")
                .buildAndPersist(userDao);

        DocumentType documentType = DocumentType.SOCIAL_SECURITY_CARD;
        user.setDefaultId(documentType, "123456789");

        GetUserDefaultIdService getUserDefaultIdService = new GetUserDefaultIdService(userDao, user.getUsername(), documentType);
        Message response = getUserDefaultIdService.executeAndGetResponse();
        String retrievedId = getUserDefaultIdService.getId();
        User retrievedUser = getUserDefaultIdService.getUser();

        assertEquals(UserMessage.SUCCESS, response);
        assertEquals("123456789", retrievedId);
        assertEquals(user, retrievedUser);
    }

    @Test
    public void retrieve_null_field_fail_test() {
        UserDao userDao = new UserDaoTestImpl(DeploymentLevel.IN_MEMORY);

        User user = EntityFactory.createUser()
                .withFirstName("Jason")
                .withLastName("Zhang")
                .withUsername("jzhang0107")
                .buildAndPersist(userDao);

        DocumentType setDocumentType = DocumentType.SOCIAL_SECURITY_CARD;
        DocumentType getDocumentType = DocumentType.BIRTH_CERTIFICATE;
        user.setDefaultId(setDocumentType, "123456789");

        GetUserDefaultIdService getUserDefaultIdService = new GetUserDefaultIdService(userDao, user.getUsername(), getDocumentType);
        Message response = getUserDefaultIdService.executeAndGetResponse();
        String retrievedId = getUserDefaultIdService.getId();
        User retrievedUser = getUserDefaultIdService.getUser();

        assertEquals(UserMessage.SUCCESS, response);
        assertEquals(null, retrievedId);
        assertEquals(user, retrievedUser);
    }

    @Test
    public void all_ids_successful_test() {
        UserDao userDao = new UserDaoTestImpl(DeploymentLevel.IN_MEMORY);

        User user = EntityFactory.createUser()
                .withFirstName("Jason")
                .withLastName("Zhang")
                .withUsername("jzhang0107")
                .buildAndPersist(userDao);

        DocumentType ssc_document = DocumentType.SOCIAL_SECURITY_CARD;
        DocumentType dl_document = DocumentType.DRIVER_LICENSE;
        DocumentType bc_document = DocumentType.BIRTH_CERTIFICATE;
        DocumentType vc_document = DocumentType.VACCINE_CARD;

        user.setDefaultId(ssc_document, "12345");
        user.setDefaultId(dl_document, "54321");
        user.setDefaultId(bc_document, "67890");
        user.setDefaultId(vc_document, "09876");

        GetUserDefaultIdService getUserDefaultIdService_ssc = new GetUserDefaultIdService(userDao, user.getUsername(), ssc_document);
        GetUserDefaultIdService getUserDefaultIdService_dl = new GetUserDefaultIdService(userDao, user.getUsername(), dl_document);
        GetUserDefaultIdService getUserDefaultIdService_bc = new GetUserDefaultIdService(userDao, user.getUsername(), bc_document);
        GetUserDefaultIdService getUserDefaultIdService_vc = new GetUserDefaultIdService(userDao, user.getUsername(), vc_document);

        Message ssc_response = getUserDefaultIdService_ssc.executeAndGetResponse();
        Message dl_response = getUserDefaultIdService_dl.executeAndGetResponse();
        Message bc_response = getUserDefaultIdService_bc.executeAndGetResponse();
        Message vc_response = getUserDefaultIdService_vc.executeAndGetResponse();

        String ssc_id = getUserDefaultIdService_ssc.getId();
        String dl_id = getUserDefaultIdService_dl.getId();
        String bc_id = getUserDefaultIdService_bc.getId();
        String vc_id = getUserDefaultIdService_vc.getId();
        User retrievedUser = getUserDefaultIdService_ssc.getUser();

        // message should still be success cause user is found
        assertEquals(UserMessage.SUCCESS, ssc_response);

        // all should pass
        assertEquals("12345", ssc_id);
        assertEquals("54321", dl_id);
        assertEquals("67890", bc_id);
        assertEquals("09876", vc_id);

        // retrieved user should be the same since there is only 1 user
        assertEquals(user, retrievedUser);
    }
}