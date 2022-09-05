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

public class FormDaoImplUnitTests {
  public FormDao formDao;

  @Before
  public void initialize() {
    TestUtils.startServer();
    this.formDao = FormDaoFactory.create(DeploymentLevel.TEST);
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
    assertEquals(1, formDao.get(testUsername).size());
    Form readForm = formDao.get(testUsername).get(0);
    assertEquals(form.getId(), readForm.getId());
    assertEquals(form.getFileId(), readForm.getFileId());
    assertEquals(form.isTemplate(), readForm.isTemplate());
    assertEquals(form.getUsername(), readForm.getUsername());
    assertEquals(form.getFormType(), readForm.getFormType());
    assertEquals(form.getMetadata(), readForm.getMetadata());
    assertEquals(form.getCondition(), readForm.getCondition());
    assertEquals(form.getConditionalFieldId(), readForm.getConditionalFieldId());
    assertEquals(form.getBody(), readForm.getBody());
    assertEquals(0, formDao.get(testUsername).get(0).compareTo(form));
  }

  @Test
  public void get() {
    String testUsername = "username1";
    Form form = EntityFactory.createForm().withUsername(testUsername).buildAndPersist(formDao);
    assertEquals(1, formDao.get(testUsername).size());
    assertEquals(0, formDao.get(testUsername).get(0).compareTo(form));
  }

  @Test
  public void deleteById() {
    String testUsername = "username1";
    EntityFactory.createForm().withUsername(testUsername).buildAndPersist(formDao);
    assertEquals(1, formDao.get(testUsername).size());
    ObjectId id = formDao.get(testUsername).get(0).getId();
    formDao.delete(id);
    assertEquals(0, formDao.get(testUsername).size());
  }

  @Test
  public void clear() {
    EntityFactory.createForm().withUsername("username1").buildAndPersist(formDao);
    EntityFactory.createForm().withUsername("username2").buildAndPersist(formDao);
    EntityFactory.createForm().withUsername("username3").buildAndPersist(formDao);
    assertEquals(3, formDao.size());
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
