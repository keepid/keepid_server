package FormTest;

import Form.Form;
import Form.Form.Metadata;
import Form.Form.Question;
import Form.Form.Section;
import Form.FormType;
import TestUtils.TestUtils;
import org.bson.types.ObjectId;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.LinkedList;
import java.util.Optional;
import java.util.TreeSet;

import static FormTest.FormControllerIntegrationTestHelperMethods.*;

public class FormControllerIntegrationTests {
  public static String username = "adminBSM";
  public static String title = "testTitle";
  public static String description = "testDescription";
  public static Form form =
      new Form(
          username,
          new ObjectId(),
          Optional.empty(),
          new Date(),
          Optional.empty(),
          FormType.FORM,
          false,
          new Metadata(
              title,
              description,
              "PA",
              "Philadelphia",
              new TreeSet<ObjectId>(),
              new Date(),
              new LinkedList<String>(),
              0),
          new Section(title, description, new LinkedList<Section>(), new LinkedList<Question>()),
          new ObjectId(),
          "");

  @BeforeClass
  public static void setUp() {
    TestUtils.startServer();
    TestUtils.setUpTestDB();
  }

  @AfterClass
  public static void tearDown() {
    TestUtils.tearDownTestDB();
  }

  @Test
  public void uploadFormTest() {
    TestUtils.login(username, username);
    uploadForm(form);
    TestUtils.logout();
  }

  @Test
  public void updateFormTest() {
    TestUtils.login(username, username);
    uploadForm(form);
    form.setUploadedAt(new Date());
    updateForm(form);
    TestUtils.logout();
  }

  @Test
  public void deleteFormTest() {
    TestUtils.login(username, username);
    uploadForm(form);
    delete(form.getId());
    TestUtils.logout();
  }

  @Test
  public void getFormTest() {
    TestUtils.login(username, username);
    uploadForm(form);
    getForm(form.getId());
    TestUtils.logout();
  }
}
