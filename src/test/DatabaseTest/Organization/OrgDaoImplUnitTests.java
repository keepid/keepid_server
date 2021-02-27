package DatabaseTest.Organization;

import Config.MongoTestConfig;
import Database.Organization.OrgDao;
import Database.Organization.OrgDaoImpl;
import Organization.Organization;
import TestUtils.EntityFactory;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static org.mockito.Mockito.*;

public class OrgDaoImplUnitTests {

  private MongoTestConfig mongoTestConfig;
  private MongoDatabase mongoDatabase;
  private MongoCollection<Organization> mongoCollection;
  private OrgDao orgDao;

  @Before
  public void initialize() {
    mongoTestConfig = mock(MongoTestConfig.class);
    mongoDatabase = mock(MongoDatabase.class);
    mongoCollection = mock(MongoCollection.class);
    when(mongoTestConfig.getDatabase()).thenReturn(mongoDatabase);
    when(mongoDatabase.getCollection("organization", Organization.class))
        .thenReturn(mongoCollection);
    orgDao = new OrgDaoImpl(mongoTestConfig);
  }

  @Test
  public void save() {
    String testOrgName = "org1";
    Organization organization = EntityFactory.createOrganization().withOrgName(testOrgName).build();
    orgDao.save(organization);
    verify(mongoCollection).insertOne(organization);
  }

  @Test
  public void get() {
    FindIterable iterable = mock(FindIterable.class);
    String testOrgName = "org1";
    Organization organization =
        EntityFactory.createOrganization().withOrgName(testOrgName).buildAndPersist(orgDao);
    when(mongoCollection.find(eq("orgName", testOrgName))).thenReturn(iterable);
    when(iterable.first()).thenReturn(organization);
    orgDao.get(testOrgName);
    verify(mongoCollection).find(eq("orgName", testOrgName));
  }

  @Test
  public void deleteByUsername() {
    String testOrgName = "org1";
    EntityFactory.createOrganization().withOrgName(testOrgName).buildAndPersist(orgDao);
    orgDao.delete(testOrgName);
    verify(mongoCollection).deleteOne(eq("orgName", testOrgName));
  }

  @Test
  public void size() {
    when(mongoCollection.countDocuments()).thenReturn(3L);
    int result = orgDao.size();
    assert result == 3;
  }

  @Test
  public void clear() {
    orgDao.clear();
    verify(mongoCollection).drop();
  }

  @Test
  public void getAll() {
    String testOrgName = "org1";
    List<Organization> orgList =
        Collections.singletonList(
            EntityFactory.createOrganization().withOrgName(testOrgName).buildAndPersist(orgDao));
    FindIterable iterable = mock(FindIterable.class);
    when(mongoCollection.find()).thenReturn(iterable);
    when(iterable.into(new ArrayList())).thenReturn(orgList);
    orgDao.getAll();
  }
}
