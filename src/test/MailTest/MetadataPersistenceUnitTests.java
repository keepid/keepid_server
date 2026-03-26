package MailTest;

import static org.junit.Assert.*;

import Form.Form;
import TestUtils.EntityFactory;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class MetadataPersistenceUnitTests {

  @Test
  public void newForm_hasEmptyApplicationMetadata() {
    Form form = EntityFactory.createForm().build();
    assertNotNull(form.getApplicationMetadata());
    assertTrue(form.getApplicationMetadata().isEmpty());
  }

  @Test
  public void setApplicationMetadata_persists() {
    Form form = EntityFactory.createForm().build();

    Map<String, String> metadata = new HashMap<>();
    metadata.put("mailKey", "PA_BIRTH_CERTIFICATE");
    metadata.put("mailAmount", "44.00");
    form.setApplicationMetadata(metadata);

    assertEquals(2, form.getApplicationMetadata().size());
    assertEquals("PA_BIRTH_CERTIFICATE", form.getApplicationMetadata().get("mailKey"));
    assertEquals("44.00", form.getApplicationMetadata().get("mailAmount"));
  }

  @Test
  public void setApplicationMetadata_canBeOverwritten() {
    Form form = EntityFactory.createForm().build();

    Map<String, String> meta1 = new HashMap<>();
    meta1.put("mailKey", "PA_BIRTH_CERTIFICATE");
    form.setApplicationMetadata(meta1);
    assertEquals("PA_BIRTH_CERTIFICATE", form.getApplicationMetadata().get("mailKey"));

    Map<String, String> meta2 = new HashMap<>();
    meta2.put("mailKey", "PA_SOCIAL_SECURITY");
    form.setApplicationMetadata(meta2);
    assertEquals("PA_SOCIAL_SECURITY", form.getApplicationMetadata().get("mailKey"));
  }

  @Test
  public void getApplicationMetadata_returnsEmptyMapWhenNull() {
    Form form = new Form();
    assertNotNull(form.getApplicationMetadata());
    assertTrue(form.getApplicationMetadata().isEmpty());
  }
}
