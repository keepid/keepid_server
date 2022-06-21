package UserTest;

import Config.DeploymentLevel;
import Config.Message;
import Database.User.UserDao;
import Database.User.UserDaoTestImpl;
import TestUtils.EntityFactory;
import User.Services.DocumentType;
import User.Services.SetUserDefaultIdService;
import User.User;
import User.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class SetDefaultIdServiceUnitTest {

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
        SetUserDefaultIdService setUserDefaultIdService = new SetUserDefaultIdService(userDao, user.getUsername(), documentType, "123456789");

        Message response = setUserDefaultIdService.executeAndGetResponse();
        String retrievedId = setUserDefaultIdService.getDocumentTypeId(documentType);
        User userRetrieved = setUserDefaultIdService.getUser();

        assertEquals(UserMessage.SUCCESS, response);
        assertEquals( "123456789", retrievedId);
        assertEquals(user, userRetrieved);
    }

    @Test
    public void set_ssn_retrieve_bc_fail_test() {
        UserDao userDao = new UserDaoTestImpl(DeploymentLevel.IN_MEMORY);

        User user = EntityFactory.createUser()
                .withFirstName("Jason")
                .withLastName("Zhang")
                .withUsername("jzhang0107")
                .buildAndPersist(userDao);

        DocumentType setDocumentType = DocumentType.SOCIAL_SECURITY_CARD;
        DocumentType retrieveDocumentType = DocumentType.BIRTH_CERTIFICATE;
        SetUserDefaultIdService setUserDefaultIdService = new SetUserDefaultIdService(userDao, user.getUsername(), setDocumentType, "12345");

        Message response = setUserDefaultIdService.executeAndGetResponse();
        String retrievedId = setUserDefaultIdService.getDocumentTypeId(retrieveDocumentType);
        User userRetrieved = setUserDefaultIdService.getUser();

        assertEquals(UserMessage.SUCCESS, response);
        assertEquals( null, retrievedId);
        assertEquals(user, userRetrieved);
    }

    @Test
    public void set_all_fields_successful_test() {
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
        SetUserDefaultIdService setUserDefaultIdService_ssc = new SetUserDefaultIdService(userDao, user.getUsername(), ssc_document, "12345");
        SetUserDefaultIdService setUserDefaultIdService_dl = new SetUserDefaultIdService(userDao, user.getUsername(), dl_document, "67890");
        SetUserDefaultIdService setUserDefaultIdService_bc = new SetUserDefaultIdService(userDao, user.getUsername(), bc_document, "09876");
        SetUserDefaultIdService setUserDefaultIdService_vc = new SetUserDefaultIdService(userDao, user.getUsername(), vc_document, "54321");

        Message ssc_response = setUserDefaultIdService_ssc.executeAndGetResponse();
        Message dl_response = setUserDefaultIdService_dl.executeAndGetResponse();
        Message bc_response = setUserDefaultIdService_bc.executeAndGetResponse();
        Message vc_response = setUserDefaultIdService_vc.executeAndGetResponse();

        String retrieved_ssc_id = setUserDefaultIdService_ssc.getDocumentTypeId(ssc_document);
        String retrieved_dl_id = setUserDefaultIdService_ssc.getDocumentTypeId(dl_document);
        String retrieved_bc_id = setUserDefaultIdService_ssc.getDocumentTypeId(bc_document);
        String retrieved_vc_id = setUserDefaultIdService_ssc.getDocumentTypeId(vc_document);
        User userRetrieved = setUserDefaultIdService_ssc.getUser();

        assertEquals(UserMessage.SUCCESS, ssc_response);
        assertEquals("12345", retrieved_ssc_id);
        assertEquals("67890", retrieved_dl_id);
        assertEquals("09876", retrieved_bc_id);
        assertEquals("54321", retrieved_vc_id);
        assertEquals(user, userRetrieved);
    }
}
