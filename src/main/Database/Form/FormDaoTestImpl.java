package Database.Form;

import static Form.FormType.APPLICATION;

import Config.DeploymentLevel;
import Form.Form;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;

public class FormDaoTestImpl implements FormDao {
  Map<String, List<Form>> formMap;
  Map<ObjectId, Form> objectIdFormMap;

  public FormDaoTestImpl(DeploymentLevel deploymentLevel) {
    if (deploymentLevel != DeploymentLevel.IN_MEMORY) {
      throw new IllegalStateException(
          "Should not run in memory test database in production or staging");
    }
    formMap = new LinkedHashMap<>();
    objectIdFormMap = new LinkedHashMap<>();
  }

  @Override
  public List<Form> get(String username) {
    return formMap.get(username);
  }

  @Override
  public void delete(ObjectId id) {
    Form form = objectIdFormMap.get(id);
    objectIdFormMap.remove(id);
    String username = form.getUsername();

    List<Form> userForms = formMap.get(username);
    Form existingForm = null;
    for (Form f : userForms) {
      if (f.getId().equals(id)) {
        existingForm = form;
      }
    }
    if (existingForm == null) {
      return;
    }
    userForms.remove(existingForm);
    formMap.put(form.getUsername(), userForms);
  }

  @Override
  public Optional<Form> get(ObjectId id) {
    return Optional.ofNullable(objectIdFormMap.get(id));
  }

  @Override
  public List<Form> getWeeklyApplications() {
    return objectIdFormMap.values().stream()
        .filter(
            form ->
                form.getFormType().equals(APPLICATION)
                    && form.getUploadedAt().isAfter(LocalDateTime.now().minusDays(7)))
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Form> getByFileId(ObjectId fileId) {
    return objectIdFormMap.values().stream().filter(x -> x.getFileId() == fileId).findFirst();
  }

  @Override
  public List<Form> getAll() {
    return new ArrayList<Form>(objectIdFormMap.values());
  }

  @Override
  public int size() {
    return formMap.size();
  }

  @Override
  public void save(Form form) {
    // System.out.println(formMap.get(form.getUsername()).size());
    List<Form> userForms = formMap.getOrDefault(form.getUsername(), new ArrayList<>());
    userForms.add(form);
    formMap.put(form.getUsername(), userForms);
    objectIdFormMap.put(form.getId(), form);
  }

  @Override
  public void delete(Form form) {
    List<Form> forms = formMap.get(form.getUsername());
    Form existingForm = null;
    for (Form f : forms) {
      if (form.getId().equals(form.getId())) {
        existingForm = form;
      }
    }
    if (existingForm == null) {
      return;
    }
    List<Form> userForms = formMap.getOrDefault(form.getUsername(), new ArrayList<>());
    userForms.remove(existingForm);
    formMap.put(form.getUsername(), userForms);
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
