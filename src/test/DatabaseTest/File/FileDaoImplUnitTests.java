package DatabaseTest.File;

import Config.DeploymentLevel;
import Database.File.FileDao;
import Database.File.FileDaoFactory;
import File.File;
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
}
