package DatabaseTest.File;

import Config.DeploymentLevel;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import File.File;
import File.FileType;

import java.util.Optional;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileDaoImplUnitTests {
  public FileDao fileDao;

  @Before
  public void initialize() {
    TestUtils.startServer();
    this.fileDao = FileDaoFactory.create(DeploymentLevel.TEST);
  }

  @After
  public void reset() {
    fileDao.clear();
  }

  @Test
  public void save() {
    String testUsername = "username1";
    File file = EntityFactory.createFile().withUsername(testUsername).build();
    fileDao.save(file);
    assertTrue(fileDao.get(file.getId()).isPresent());
    File fileFromDao = fileDao.get(file.getId()).get();

    assertEquals(1, fileDao.getAll(testUsername).size());
    assertEquals(testUsername, fileFromDao.getUsername());
    assertEquals("testOrganizationName", fileFromDao.getOrganizationName());
    assertEquals("testContentType", fileFromDao.getContentType());
    assertEquals(file.isAnnotated(), fileFromDao.isAnnotated());
    assertEquals(file.getUploadedAt().toString(), fileFromDao.getUploadedAt().toString());
    assertEquals(file.getFileId(), fileFromDao.getFileId());
    assertEquals(file.getFilename(), fileFromDao.getFilename());
    assertEquals(file.getFileType(), fileFromDao.getFileType());
  }

  @Test
  public void get() {
    String testUsername = "username1";
    File file = EntityFactory.createFile().withUsername(testUsername).buildAndPersist(fileDao);
    assertTrue(fileDao.getAll(testUsername).size() == 1);
    assertEquals(fileDao.getAll(testUsername).get(0), file);
  }

  @Test
  public void deleteById() {
    String testUsername = "username1";
    EntityFactory.createFile().withUsername(testUsername).buildAndPersist(fileDao);
    assertTrue(fileDao.getAll(testUsername).size() == 1);
    ObjectId id = fileDao.getAll(testUsername).get(0).getId();
    fileDao.delete(id);
    assertTrue(fileDao.getAll(testUsername).size() == 0);
  }

  @Test
  public void clear() {
    EntityFactory.createFile().withUsername("username1").buildAndPersist(fileDao);
    EntityFactory.createFile().withUsername("username2").buildAndPersist(fileDao);
    EntityFactory.createFile().withUsername("username3").buildAndPersist(fileDao);
    assertTrue(fileDao.size() > 0);
    fileDao.clear();
    assertEquals(0, fileDao.size());
  }

  @Test
  public void getAll() {
    fileDao.clear();
    EntityFactory.createFile().withUsername("username1").buildAndPersist(fileDao);
    EntityFactory.createFile().withUsername("username2").buildAndPersist(fileDao);
    EntityFactory.createFile().withUsername("username3").buildAndPersist(fileDao);
    assertEquals(3, fileDao.getAll().size());
  }

  @Test
  public void getPfp() {
    String user = "username1";
    FileType type = FileType.PROFILE_PICTURE;
    EntityFactory.createFile()
            .withUsername(user)
            .withFileType(type)
            .withFilename("7/20testpfp")
            .buildAndPersist(fileDao);

    Optional<File> retrievedfile = fileDao.get(user, type);
    assertTrue(retrievedfile.isPresent());
    assertEquals(FileType.PROFILE_PICTURE, retrievedfile.get().getFileType());
  }

// Requires a file to already be inserted to test_db with id
//  @Test
//  public void getPFPUsingID() {
//    String id = "62d83d68ccf42f70b087fdf3";
//    Optional<File> retrievedfile = fileDao.get(new ObjectId(id));
//    assertTrue(retrievedfile.isPresent());
//  }

// Used for development when fileDao.clear() is commented out
//  @Test
//  public void printAllFile() {
//    List<File> collection = fileDao.getAll();
//    for (File file : collection) {
//      System.out.println("name: " + file.getFilename());
//      assertEquals(FileType.PROFILE_PICTURE, file.getFileType());
//    }
//  }
}