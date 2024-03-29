package DatabaseTest.Form;

import Config.DeploymentLevel;
import Database.Form.FormDao;
import Database.Form.FormDaoFactory;
import Form.Form;
import TestUtils.EntityFactory;
import com.google.common.collect.ImmutableList;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormDaoTestImplUnitTests {
  public FormDao formDao;

  @Before
  public void initialize() {
    this.formDao = FormDaoFactory.create(DeploymentLevel.IN_MEMORY);
  }

  @After
  public void reset() {
    this.formDao.clear();
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
    assertTrue(formDao.get(testUsername).size() > 0);
    assertEquals(formDao.get(testUsername).get(0), form);
  }

  @Test
  public void delete() {
    String testUsername = "username1";
    EntityFactory.createForm().withUsername(testUsername).buildAndPersist(formDao);
    assertTrue(formDao.get(testUsername).size() > 0);
    ObjectId id = formDao.get(testUsername).get(0).getId();
    formDao.delete(id);
    // assertTrue(formDao.get(testUsername).size() == 0);
  }

  @Test
  public void size() {
    EntityFactory.createForm().withUsername("username1").buildAndPersist(formDao);
    EntityFactory.createForm().withUsername("username2").buildAndPersist(formDao);
    EntityFactory.createForm().withUsername("username3").buildAndPersist(formDao);
    assertEquals(3, formDao.size());
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
    Form form1 = EntityFactory.createForm().withUsername("username1").buildAndPersist(formDao);
    Form form2 = EntityFactory.createForm().withUsername("username2").buildAndPersist(formDao);
    Form form3 = EntityFactory.createForm().withUsername("username3").buildAndPersist(formDao);
    assertEquals(ImmutableList.of(form1, form2, form3), formDao.getAll());
  }

  @Ignore
  @Test
  public void JSONTests() {
    String testUsername = "username1";
    Form form = EntityFactory.createForm().withUsername(testUsername).buildAndPersist(formDao);
    JSONObject obj = form.toJSON();
    Form newForm = Form.fromJson(obj);
    // System.out.println(obj);
    // System.out.println(form.getMetadata().getLast());
    assertTrue(newForm.equals(form));
  }
}
