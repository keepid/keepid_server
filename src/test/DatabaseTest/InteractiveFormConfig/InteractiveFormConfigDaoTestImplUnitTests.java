package DatabaseTest.InteractiveFormConfig;

import static org.junit.Assert.*;

import Config.DeploymentLevel;
import Database.InteractiveFormConfig.InteractiveFormConfigDao;
import Database.InteractiveFormConfig.InteractiveFormConfigDaoFactory;
import Form.InteractiveFormConfig;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InteractiveFormConfigDaoTestImplUnitTests {
  private InteractiveFormConfigDao dao;

  @Before
  public void setup() {
    dao = InteractiveFormConfigDaoFactory.create(DeploymentLevel.IN_MEMORY);
  }

  @After
  public void teardown() {
    dao.clear();
  }

  @Test
  public void saveAndGetById() {
    ObjectId fileId = new ObjectId();
    InteractiveFormConfig config =
        new InteractiveFormConfig(fileId, "{\"type\":\"object\"}", "{\"type\":\"Categorization\"}");
    dao.save(config);

    Optional<InteractiveFormConfig> result = dao.get(config.getId());
    assertTrue(result.isPresent());
    assertEquals(fileId, result.get().getFileId());
    assertEquals("{\"type\":\"object\"}", result.get().getJsonSchema());
    assertEquals("{\"type\":\"Categorization\"}", result.get().getUiSchema());
  }

  @Test
  public void getByFileId() {
    ObjectId fileId = new ObjectId();
    InteractiveFormConfig config =
        new InteractiveFormConfig(fileId, "{}", "{}");
    dao.save(config);

    Optional<InteractiveFormConfig> result = dao.getByFileId(fileId);
    assertTrue(result.isPresent());
    assertEquals(config.getId(), result.get().getId());
  }

  @Test
  public void getByFileIdNotFound() {
    Optional<InteractiveFormConfig> result = dao.getByFileId(new ObjectId());
    assertFalse(result.isPresent());
  }

  @Test
  public void upsertByFileIdCreatesNew() {
    ObjectId fileId = new ObjectId();
    InteractiveFormConfig config =
        new InteractiveFormConfig(fileId, "{\"v\":1}", "{\"v\":1}");
    dao.upsertByFileId(config);

    assertEquals(1, dao.size());
    Optional<InteractiveFormConfig> result = dao.getByFileId(fileId);
    assertTrue(result.isPresent());
    assertEquals("{\"v\":1}", result.get().getJsonSchema());
  }

  @Test
  public void upsertByFileIdUpdatesExisting() {
    ObjectId fileId = new ObjectId();
    InteractiveFormConfig config =
        new InteractiveFormConfig(fileId, "{\"v\":1}", "{\"v\":1}");
    dao.save(config);

    InteractiveFormConfig updated =
        new InteractiveFormConfig(fileId, "{\"v\":2}", "{\"v\":2}");
    dao.upsertByFileId(updated);

    assertEquals(1, dao.size());
    Optional<InteractiveFormConfig> result = dao.getByFileId(fileId);
    assertTrue(result.isPresent());
    assertEquals("{\"v\":2}", result.get().getJsonSchema());
    assertEquals("{\"v\":2}", result.get().getUiSchema());
  }

  @Test
  public void deleteByFileId() {
    ObjectId fileId = new ObjectId();
    InteractiveFormConfig config =
        new InteractiveFormConfig(fileId, "{}", "{}");
    dao.save(config);
    assertEquals(1, dao.size());

    dao.deleteByFileId(fileId);
    assertEquals(0, dao.size());
    assertFalse(dao.getByFileId(fileId).isPresent());
  }

  @Test
  public void deleteByFileIdNoOp() {
    dao.deleteByFileId(new ObjectId());
    assertEquals(0, dao.size());
  }

  @Test
  public void getAll() {
    dao.save(new InteractiveFormConfig(new ObjectId(), "{}", "{}"));
    dao.save(new InteractiveFormConfig(new ObjectId(), "{}", "{}"));
    assertEquals(2, dao.getAll().size());
  }

  @Test
  public void clear() {
    dao.save(new InteractiveFormConfig(new ObjectId(), "{}", "{}"));
    dao.save(new InteractiveFormConfig(new ObjectId(), "{}", "{}"));
    dao.clear();
    assertEquals(0, dao.size());
  }
}
