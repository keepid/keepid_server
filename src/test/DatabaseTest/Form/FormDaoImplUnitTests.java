package DatabaseTest.Form;

import Config.DeploymentLevel;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Form.Form;
import TestUtils.EntityFactory;
import TestUtils.TestUtils;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormDaoImplUnitTests {
  public FormDao formDao;

  @Before
  public void initialize() {
    TestUtils.startServer();
    this.formDao = FormDaoFactory.create(DeploymentLevel.TEST);
    formDao.clear();
  }

  @After
  public void reset() {
    formDao.clear();
  }

  @Test
  public void save() {
    String testUsername = "username1";
    Form form = EntityFactory.createForm().withUsername(testUsername).build();
    formDao.save(form);
    assertTrue(formDao.get(testUsername).size() == 1);
    assertEquals(formDao.get(testUsername).get(0), form);
  }

  @Test
  public void get() {
    String testUsername = "username1";
    Form form = EntityFactory.createForm().withUsername(testUsername).buildAndPersist(formDao);
    assertTrue(formDao.get(testUsername).size() == 1);
    assertEquals(formDao.get(testUsername).get(0), form);
  }

  @Test
  public void deleteById() {
    String testUsername = "username1";
    EntityFactory.createForm().withUsername(testUsername).buildAndPersist(formDao);
    assertTrue(formDao.get(testUsername).size() == 1);
    ObjectId id = formDao.get(testUsername).get(0).getId();
    formDao.delete(id);
    assertTrue(formDao.get(testUsername).size() == 0);
  }

  @Test
  public void clear() {
    EntityFactory.createForm().withUsername("username1").buildAndPersist(formDao);
    EntityFactory.createForm().withUsername("username2").buildAndPersist(formDao);
    EntityFactory.createForm().withUsername("username3").buildAndPersist(formDao);
    assertTrue(formDao.size() > 0);
    formDao.clear();
    assertEquals(0, formDao.size());
  }

  @Test
  public void getAll() {
    formDao.clear();
    EntityFactory.createForm().withUsername("username1").buildAndPersist(formDao);
    EntityFactory.createForm().withUsername("username2").buildAndPersist(formDao);
    EntityFactory.createForm().withUsername("username3").buildAndPersist(formDao);
    assertEquals(3, formDao.getAll().size());
  }
}
