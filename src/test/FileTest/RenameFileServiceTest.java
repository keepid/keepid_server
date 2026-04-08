package FileTest;

import static org.junit.Assert.*;

import Config.DeploymentLevel;
import Config.Message;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import File.File;
import File.FileMessage;
import File.FileType;
import File.Services.RenameFileService;
import TestUtils.EntityFactory;
import User.UserType;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RenameFileServiceTest {
  private FileDao fileDao;

  @Before
  public void setUp() {
    fileDao = FileDaoFactory.create(DeploymentLevel.IN_MEMORY);
  }

  @After
  public void tearDown() {
    fileDao.clear();
  }

  private RenameFileService createService(
      String fileId, String newFilename, String orgName, UserType userType) {
    return new RenameFileService(
        fileDao, fileId, newFilename, orgName, userType, java.util.Optional.empty());
  }

  @Test
  public void workerCanRenameFileInSameOrg() {
    File file =
        EntityFactory.createFile()
            .withFilename("old-name.pdf")
            .withFileType(FileType.IDENTIFICATION_PDF)
            .withOrganizationName("TestOrg")
            .withUsername("client1")
            .buildAndPersist(fileDao);

    Message result =
        createService(file.getId().toString(), "new-name.pdf", "TestOrg", UserType.Worker)
            .executeAndGetResponse();

    assertEquals(FileMessage.SUCCESS.getErrorName(), result.getErrorName());
    File updated = fileDao.get(file.getId()).orElse(null);
    assertNotNull(updated);
    assertEquals("new-name.pdf", updated.getFilename());
  }

  @Test
  public void adminCanRenameFileInSameOrg() {
    File file =
        EntityFactory.createFile()
            .withFilename("old-name.pdf")
            .withFileType(FileType.APPLICATION_PDF)
            .withOrganizationName("TestOrg")
            .withUsername("client1")
            .buildAndPersist(fileDao);

    Message result =
        createService(file.getId().toString(), "renamed.pdf", "TestOrg", UserType.Admin)
            .executeAndGetResponse();

    assertEquals(FileMessage.SUCCESS.getErrorName(), result.getErrorName());
    assertEquals("renamed.pdf", fileDao.get(file.getId()).get().getFilename());
  }

  @Test
  public void directorCanRenameFileInSameOrg() {
    File file =
        EntityFactory.createFile()
            .withFilename("old-name.pdf")
            .withOrganizationName("TestOrg")
            .buildAndPersist(fileDao);

    Message result =
        createService(file.getId().toString(), "renamed.pdf", "TestOrg", UserType.Director)
            .executeAndGetResponse();

    assertEquals(FileMessage.SUCCESS.getErrorName(), result.getErrorName());
    assertEquals("renamed.pdf", fileDao.get(file.getId()).get().getFilename());
  }

  @Test
  public void clientCannotRenameFile() {
    File file =
        EntityFactory.createFile()
            .withFilename("old-name.pdf")
            .withOrganizationName("TestOrg")
            .withUsername("client1")
            .buildAndPersist(fileDao);

    Message result =
        createService(file.getId().toString(), "new-name.pdf", "TestOrg", UserType.Client)
            .executeAndGetResponse();

    assertEquals(FileMessage.INSUFFICIENT_PRIVILEGE.getErrorName(), result.getErrorName());
    assertEquals("old-name.pdf", fileDao.get(file.getId()).get().getFilename());
  }

  @Test
  public void cannotRenameCrossOrg() {
    File file =
        EntityFactory.createFile()
            .withFilename("old-name.pdf")
            .withOrganizationName("OrgA")
            .buildAndPersist(fileDao);

    Message result =
        createService(file.getId().toString(), "new-name.pdf", "OrgB", UserType.Worker)
            .executeAndGetResponse();

    assertEquals(FileMessage.INSUFFICIENT_PRIVILEGE.getErrorName(), result.getErrorName());
    assertEquals("old-name.pdf", fileDao.get(file.getId()).get().getFilename());
  }

  @Test
  public void renameNonexistentFileReturnsNoSuchFile() {
    ObjectId fakeId = new ObjectId();
    Message result =
        createService(fakeId.toString(), "new-name.pdf", "TestOrg", UserType.Worker)
            .executeAndGetResponse();

    assertEquals(FileMessage.NO_SUCH_FILE.getErrorName(), result.getErrorName());
  }

  @Test
  public void renameWithInvalidObjectIdReturnsInvalidParam() {
    Message result =
        createService("not-an-object-id", "new-name.pdf", "TestOrg", UserType.Worker)
            .executeAndGetResponse();

    assertEquals(FileMessage.INVALID_PARAMETER.getErrorName(), result.getErrorName());
  }

  @Test
  public void renameWithEmptyFilenameReturnsInvalidParam() {
    File file =
        EntityFactory.createFile()
            .withFilename("old-name.pdf")
            .withOrganizationName("TestOrg")
            .buildAndPersist(fileDao);

    Message result =
        createService(file.getId().toString(), "  ", "TestOrg", UserType.Worker)
            .executeAndGetResponse();

    assertEquals(FileMessage.INVALID_PARAMETER.getErrorName(), result.getErrorName());
    assertEquals("old-name.pdf", fileDao.get(file.getId()).get().getFilename());
  }

  @Test
  public void renameWithNullFilenameReturnsInvalidParam() {
    File file =
        EntityFactory.createFile()
            .withFilename("old-name.pdf")
            .withOrganizationName("TestOrg")
            .buildAndPersist(fileDao);

    Message result =
        createService(file.getId().toString(), null, "TestOrg", UserType.Worker)
            .executeAndGetResponse();

    assertEquals(FileMessage.INVALID_PARAMETER.getErrorName(), result.getErrorName());
  }

  @Test
  public void renameTrimsWhitespace() {
    File file =
        EntityFactory.createFile()
            .withFilename("old-name.pdf")
            .withOrganizationName("TestOrg")
            .buildAndPersist(fileDao);

    Message result =
        createService(
                file.getId().toString(), "  trimmed-name.pdf  ", "TestOrg", UserType.Worker)
            .executeAndGetResponse();

    assertEquals(FileMessage.SUCCESS.getErrorName(), result.getErrorName());
    assertEquals("trimmed-name.pdf", fileDao.get(file.getId()).get().getFilename());
  }
}
