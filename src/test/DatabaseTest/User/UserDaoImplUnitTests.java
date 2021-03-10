package DatabaseTest.User;

import Config.MongoStagingConfig;
import Database.User.UserDao;
import Database.User.UserDaoImpl;
import TestUtils.EntityFactory;
import User.User;
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

public class UserDaoImplUnitTests {

  private MongoStagingConfig mongoStagingConfig;
  private MongoDatabase mongoDatabase;
  private MongoCollection<User> mongoCollection;
  private UserDao userDao;

  @Before
  public void initialize() {
    mongoStagingConfig = mock(MongoStagingConfig.class);
    mongoDatabase = mock(MongoDatabase.class);
    mongoCollection = mock(MongoCollection.class);
    when(mongoStagingConfig.getDatabase()).thenReturn(mongoDatabase);
    when(mongoDatabase.getCollection("user", User.class)).thenReturn(mongoCollection);
    userDao = new UserDaoImpl(mongoStagingConfig);
  }

  @Test
  public void save() {
    String testUsername = "user1";
    User user = EntityFactory.createUser().withUsername(testUsername).build();
    userDao.save(user);
    verify(mongoCollection).insertOne(user);
  }

  @Test
  public void get() {
    FindIterable iterable = mock(FindIterable.class);
    String testUsername = "user1";
    User user = EntityFactory.createUser().withUsername(testUsername).buildAndPersist(userDao);
    when(mongoCollection.find(eq("username", testUsername))).thenReturn(iterable);
    when(iterable.first()).thenReturn(user);
    userDao.get(testUsername);
    verify(mongoCollection).find(eq("username", testUsername));
  }

  @Test
  public void deleteByUsername() {
    String testUsername = "user1";
    EntityFactory.createUser().withUsername(testUsername).buildAndPersist(userDao);
    userDao.delete(testUsername);
    verify(mongoCollection).deleteOne(eq("username", testUsername));
  }

  @Test
  public void size() {
    when(mongoCollection.countDocuments()).thenReturn(3L);
    int result = userDao.size();
    assert result == 3;
  }

  @Test
  public void clear() {
    userDao.clear();
    verify(mongoCollection).drop();
  }

  @Test
  public void getAll() {
    String testUsername = "user1";
    List<User> userList =
        Collections.singletonList(
            EntityFactory.createUser().withUsername(testUsername).buildAndPersist(userDao));
    FindIterable iterable = mock(FindIterable.class);
    when(mongoCollection.find()).thenReturn(iterable);
    when(iterable.into(new ArrayList())).thenReturn(userList);
    userDao.getAll();
  }
}
