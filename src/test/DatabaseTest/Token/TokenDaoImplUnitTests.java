package DatabaseTest.Token;

import Config.MongoTestConfig;
import Database.Token.TokenDao;
import Database.Token.TokenDaoImpl;
import Security.Tokens;
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

public class TokenDaoImplUnitTests {

  private MongoTestConfig mongoTestConfig;
  private MongoDatabase mongoDatabase;
  private MongoCollection<Tokens> mongoCollection;
  private TokenDao tokenDao;

  @Before
  public void initialize() {
    mongoTestConfig = mock(MongoTestConfig.class);
    mongoDatabase = mock(MongoDatabase.class);
    mongoCollection = mock(MongoCollection.class);
    when(mongoTestConfig.getDatabase()).thenReturn(mongoDatabase);
    when(mongoDatabase.getCollection("tokens", Tokens.class)).thenReturn(mongoCollection);
    tokenDao = new TokenDaoImpl(mongoTestConfig);
  }

  @Test
  public void save() {
    String testUsername = "user1";
    Tokens tokens = EntityFactory.createTokens().withUsername(testUsername).build();
    tokenDao.save(tokens);
    verify(mongoCollection).insertOne(tokens);
  }

  @Test
  public void get() {
    FindIterable iterable = mock(FindIterable.class);
    String testUsername = "user1";
    Tokens tokens =
        EntityFactory.createTokens().withUsername(testUsername).buildAndPersist(tokenDao);
    when(mongoCollection.find(eq("username", testUsername))).thenReturn(iterable);
    when(iterable.first()).thenReturn(tokens);
    tokenDao.get(testUsername);
    verify(mongoCollection).find(eq("username", testUsername));
  }

  @Test
  public void deleteByUsername() {
    String testUsername = "user1";
    EntityFactory.createTokens().withUsername(testUsername).buildAndPersist(tokenDao);
    tokenDao.delete(testUsername);
    verify(mongoCollection).deleteOne(eq("username", testUsername));
  }

  @Test
  public void size() {
    when(mongoCollection.countDocuments()).thenReturn(3L);
    int result = tokenDao.size();
    assert result == 3;
  }

  @Test
  public void clear() {
    tokenDao.clear();
    verify(mongoCollection).drop();
  }

  @Test
  public void getAll() {
    String testUsername = "user1";
    List<Tokens> tokensList =
        Collections.singletonList(
            EntityFactory.createTokens().withUsername(testUsername).buildAndPersist(tokenDao));
    FindIterable iterable = mock(FindIterable.class);
    when(mongoCollection.find()).thenReturn(iterable);
    when(iterable.into(new ArrayList())).thenReturn(tokensList);
    tokenDao.getAll();
  }
}
