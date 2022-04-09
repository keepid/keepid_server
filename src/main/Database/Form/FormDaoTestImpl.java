package Database.Form;

import Config.DeploymentLevel;
import Form.Form;
import org.bson.types.ObjectId;

import java.util.*;

public class FormDaoTestImpl implements FormDao {
  Map<String, List<Form>> formMap;
  Map<ObjectId, Form> objectIdFormMap;
  List<Form> allForms;

  public FormDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in memory test database in production or staging");
    }
    formMap = new LinkedHashMap<>();
    objectIdFormMap = new LinkedHashMap<>();
    allForms = new ArrayList<>();
  }

  @Override
  public List<Form> get(String username) {
    return formMap.get(username);
  }

  @Override
  public void delete(ObjectId id) {
    Form form = objectIdFormMap.remove(id);
    formMap.remove(form.getUsername());
  }

  @Override
  public Optional<Form> get(ObjectId id) {
    return Optional.ofNullable(objectIdFormMap.get(id));
  }

  @Override
  public Optional<Form> getByFileId(ObjectId fileId) {
    return allForms.stream().filter(x -> x.getFileId() == fileId).findFirst();
  }

  @Override
  public List<Form> getAll() {
    return allForms;
  }

  @Override
  public int size() {
    return formMap.size();
  }

  @Override
  public void save(Form form) {
    allForms.add(form);
    formMap.put(form.getUsername(), formMap.getOrDefault(form.getUsername(), new ArrayList<>()));
    objectIdFormMap.put(form.getId(), form);
  }

  @Override
  public void delete(Form form) {
    formMap.remove(form.getUsername());
    objectIdFormMap.remove(form.getId());
  }

  @Override
  public void update(Form newForm) {
    List<Form> forms = formMap.get(newForm.getUsername());
    Form existingForm = null;
    for (Form form : forms) {
      if (form.getId().equals(newForm.getId())) {
        existingForm = form;
      }
    }
    if (existingForm == null) {
      return;
    }
    forms.remove(existingForm);
    forms.add(newForm);
    formMap.put(newForm.getUsername(), forms);
    objectIdFormMap.put(newForm.getId(), newForm);
  }

  @Override
  public void clear() {
    formMap.clear();
    objectIdFormMap.clear();
  }
}
